use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;

/// User session data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Session {
    pub id: Uuid,
    pub username: String,
    pub language: String,
}

/// Database session record
#[derive(Debug, Clone, FromRow)]
pub struct DbSession {
    pub id: Uuid,
    pub username: String,
    pub language: String,
    pub created_at: DateTime<Utc>,
    pub last_active_at: DateTime<Utc>,
}

/// Chat message
#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct Message {
    pub id: Uuid,
    pub username: String,
    pub content: String,
    pub created_at: DateTime<Utc>,
}

impl Message {
    /// Create a new message and save to database
    pub async fn create(
        pool: &sqlx::PgPool,
        username: &str,
        content: &str,
    ) -> Result<Self, sqlx::Error> {
        let id = Uuid::new_v4();
        let message = sqlx::query_as::<_, Message>(
            "INSERT INTO messages (id, username, content) VALUES ($1, $2, $3) RETURNING *",
        )
        .bind(id)
        .bind(username)
        .bind(content)
        .fetch_one(pool)
        .await?;
        Ok(message)
    }

    /// Get recent messages (limit 50)
    pub async fn get_recent(pool: &sqlx::PgPool, limit: i64) -> Result<Vec<Self>, sqlx::Error> {
        sqlx::query_as::<_, Message>("SELECT * FROM messages ORDER BY created_at DESC LIMIT $1")
            .bind(limit)
            .fetch_all(pool)
            .await
    }
}

/// Online user tracking
#[derive(Debug, Clone, Serialize)]
pub struct OnlineUser {
    pub username: String,
    pub language: String,
    pub last_seen: DateTime<Utc>,
}
