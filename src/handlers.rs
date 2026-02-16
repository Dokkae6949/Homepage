use crate::error::AppResult;
use crate::extractors::{AuthenticatedUser, Translator};
use crate::models::{Message, Session};
use crate::state::AppState;
use crate::templates::Templates;
use axum::{
    extract::State,
    response::{Html, IntoResponse, Redirect, Sse},
    Form,
};
use axum_extra::extract::{
    cookie::{Cookie as CookieType, SameSite},
    PrivateCookieJar,
};
use chrono::Utc;
use minijinja::Value;
use serde::Deserialize;
use std::convert::Infallible;
use std::sync::Arc;
use tokio_stream::{wrappers::BroadcastStream, StreamExt};
use uuid::Uuid;
use validator::Validate;

const SESSION_COOKIE_NAME: &str = "session";

/// Login form data
#[derive(Debug, Deserialize, Validate)]
pub struct LoginForm {
    #[validate(length(min = 3, max = 50))]
    username: String,
    language: String,
}

/// Message form data
#[derive(Debug, Deserialize, Validate)]
pub struct MessageForm {
    #[validate(length(min = 1, max = 500))]
    content: String,
}

/// Show login page
pub async fn show_login(translator: Translator) -> AppResult<Html<String>> {
    let templates = Templates::new(Arc::new(crate::i18n::Translator::new()));

    let context = minijinja::context! {
        language => translator.language(),
        languages => translator.supported_languages(),
        dir => "ltr",
    };

    let html = templates.render("login.html", Value::from_serialize(&context))?;
    Ok(Html(html))
}

/// Handle login
pub async fn login(
    State(state): State<AppState>,
    jar: PrivateCookieJar,
    Form(form): Form<LoginForm>,
) -> AppResult<impl IntoResponse> {
    // Validate form
    form.validate()
        .map_err(|e| crate::error::AppError::Validation(format!("{}", e)))?;

    // Create session
    let session = Session {
        id: Uuid::new_v4(),
        username: form.username.clone(),
        language: form.language.clone(),
    };

    // Save session to database
    sqlx::query(
        "INSERT INTO sessions (id, username, language) VALUES ($1, $2, $3)
         ON CONFLICT (id) DO UPDATE SET last_active_at = NOW()",
    )
    .bind(session.id)
    .bind(&session.username)
    .bind(&session.language)
    .execute(&state.pool)
    .await?;

    // Mark user as online
    state.user_online(form.username, form.language).await;

    // Broadcast user update
    let _ = state.message_tx.send("user-update".to_string());

    // Set session cookie
    let session_json = serde_json::to_string(&session).map_err(|e| {
        crate::error::AppError::Session(format!("Failed to serialize session: {}", e))
    })?;

    let cookie = CookieType::build((SESSION_COOKIE_NAME, session_json))
        .path("/")
        .http_only(true)
        .same_site(SameSite::Strict)
        .build();

    let jar = jar.add(cookie);

    Ok((jar, Redirect::to("/chat")))
}

/// Show chat page
pub async fn show_chat(user: AuthenticatedUser, translator: Translator) -> AppResult<Html<String>> {
    let templates = Templates::new(Arc::new(crate::i18n::Translator::new()));

    let context = minijinja::context! {
        username => user.username,
        language => translator.language(),
        languages => translator.supported_languages(),
        dir => "ltr",
    };

    let html = templates.render("chat.html", Value::from_serialize(&context))?;
    Ok(Html(html))
}

/// Get messages
pub async fn get_messages(
    State(state): State<AppState>,
    user: AuthenticatedUser,
) -> AppResult<Html<String>> {
    let messages = Message::get_recent(&state.pool, 50).await?;

    let templates = Templates::new(Arc::new(crate::i18n::Translator::new()));
    let mut html = String::new();

    for message in messages.iter().rev() {
        let context = minijinja::context! {
            username => message.username,
            content => message.content,
            time => format_time(&message.created_at),
            is_own => message.username == user.username,
        };

        html.push_str(&templates.render("message.html", Value::from_serialize(&context))?);
    }

    Ok(Html(html))
}

/// Post a message
pub async fn post_message(
    State(state): State<AppState>,
    user: AuthenticatedUser,
    Form(form): Form<MessageForm>,
) -> AppResult<Html<String>> {
    // Validate form
    form.validate()
        .map_err(|e| crate::error::AppError::Validation(format!("{}", e)))?;

    // Create message
    let message = Message::create(&state.pool, &user.username, &form.content).await?;

    // Broadcast new message
    let _ = state.message_tx.send("new-message".to_string());

    // Render message HTML
    let templates = Templates::new(Arc::new(crate::i18n::Translator::new()));
    let context = minijinja::context! {
        username => message.username,
        content => message.content,
        time => format_time(&message.created_at),
        is_own => true,
    };

    let html = templates.render("message.html", Value::from_serialize(&context))?;
    Ok(Html(html))
}

/// Get online users
pub async fn get_online_users(
    State(state): State<AppState>,
    user: AuthenticatedUser,
    translator: Translator,
) -> AppResult<Html<String>> {
    let users = state.get_online_users().await;

    let templates = Templates::new(Arc::new(crate::i18n::Translator::new()));
    let mut html = String::new();

    for online_user in users {
        let context = minijinja::context! {
            username => online_user.username,
            language => translator.language(),
            is_current => online_user.username == user.username,
        };

        html.push_str(&templates.render("online_user.html", Value::from_serialize(&context))?);
    }

    if html.is_empty() {
        html = format!(
            r#"<div class="no-users">{}</div>"#,
            translator.translate("no-users-online")
        );
    }

    Ok(Html(html))
}

/// Server-Sent Events endpoint
pub async fn events(
    State(state): State<AppState>,
    _user: AuthenticatedUser,
) -> Sse<
    impl tokio_stream::Stream<Item = std::result::Result<axum::response::sse::Event, Infallible>>,
> {
    let rx = state.message_tx.subscribe();
    let stream = BroadcastStream::new(rx).map(move |msg| {
        let event_type = msg.unwrap_or_else(|_| "ping".to_string());
        Ok(axum::response::sse::Event::default()
            .event(&event_type)
            .data("update"))
    });

    Sse::new(stream).keep_alive(
        axum::response::sse::KeepAlive::new()
            .interval(std::time::Duration::from_secs(15))
            .text("keep-alive"),
    )
}

/// Change language
pub async fn change_language(
    State(state): State<AppState>,
    user: AuthenticatedUser,
    jar: PrivateCookieJar,
    Form(form): Form<LanguageForm>,
) -> AppResult<impl IntoResponse> {
    // Update session language
    let session = Session {
        id: user.session_id,
        username: user.username.clone(),
        language: form.language.clone(),
    };

    // Update in database
    sqlx::query("UPDATE sessions SET language = $1 WHERE id = $2")
        .bind(&form.language)
        .bind(session.id)
        .execute(&state.pool)
        .await?;

    // Update session cookie
    let session_json = serde_json::to_string(&session).map_err(|e| {
        crate::error::AppError::Session(format!("Failed to serialize session: {}", e))
    })?;

    let cookie = CookieType::build((SESSION_COOKIE_NAME, session_json))
        .path("/")
        .http_only(true)
        .same_site(SameSite::Strict)
        .build();

    let jar = jar.add(cookie);

    Ok((jar, Redirect::to("/chat")))
}

#[derive(Debug, Deserialize)]
pub struct LanguageForm {
    language: String,
}

/// Helper to format time
fn format_time(time: &chrono::DateTime<Utc>) -> String {
    let now = Utc::now();
    let diff = now.signed_duration_since(*time);

    if diff.num_seconds() < 60 {
        "just now".to_string()
    } else if diff.num_minutes() < 60 {
        format!("{} min ago", diff.num_minutes())
    } else if diff.num_hours() < 24 {
        format!("{} hr ago", diff.num_hours())
    } else {
        time.format("%Y-%m-%d %H:%M").to_string()
    }
}
