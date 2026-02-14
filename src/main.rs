mod config;
mod error;
mod extractors;
mod handlers;
mod i18n;
mod models;
mod state;
mod templates;

use axum::{
    extract::DefaultBodyLimit,
    routing::{get, post},
    Router,
};
use axum_extra::extract::cookie::Key;
use sqlx::postgres::PgPoolOptions;
use std::net::SocketAddr;
use tower_http::{
    compression::CompressionLayer,
    services::ServeDir,
    trace::{DefaultOnResponse, TraceLayer},
};
use tracing::Level;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

use crate::config::Config;
use crate::state::{AppState, AppStateWithKey};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize tracing
    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| {
                "chat_app=debug,tower_http=debug,axum::rejection=trace".into()
            }),
        )
        .with(tracing_subscriber::fmt::layer())
        .init();

    // Load configuration
    let config = Config::from_env()?;
    tracing::info!("Starting application with config: {:?}", config);

    // Setup database connection pool
    let pool = PgPoolOptions::new()
        .max_connections(10)
        .connect(&config.database_url)
        .await?;

    tracing::info!("Connected to database");

    // Run migrations
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await?;

    tracing::info!("Migrations completed");

    // Create application state
    let state = AppState::new(pool);

    // Generate a random key for cookie encryption
    // In production, this should be loaded from environment
    let key = Key::generate();

    let app_state = AppStateWithKey::new(state, key);

    // Build router
    let app = Router::new()
        .route("/", get(handlers::show_login))
        .route("/login", post(handlers::login))
        .route("/chat", get(handlers::show_chat))
        .route("/messages", get(handlers::get_messages))
        .route("/messages", post(handlers::post_message))
        .route("/online-users", get(handlers::get_online_users))
        .route("/events", get(handlers::events))
        .route("/change-language", post(handlers::change_language))
        .nest_service("/static", ServeDir::new("static"))
        .layer(DefaultBodyLimit::max(1024 * 1024)) // 1MB
        .layer(CompressionLayer::new())
        .layer(
            TraceLayer::new_for_http()
                .on_response(DefaultOnResponse::new().level(Level::INFO)),
        )
        .with_state(app_state);

    // Start server
    let addr = SocketAddr::from(([0, 0, 0, 0], config.port));
    tracing::info!("Listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}
