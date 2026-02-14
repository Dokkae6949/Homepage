use crate::models::OnlineUser;
use chrono::Utc;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::{broadcast, RwLock};

/// Application state shared across handlers
#[derive(Clone)]
pub struct AppState {
    pub pool: sqlx::PgPool,
    pub online_users: Arc<RwLock<HashMap<String, OnlineUser>>>,
    pub message_tx: broadcast::Sender<String>,
}

impl AppState {
    pub fn new(pool: sqlx::PgPool) -> Self {
        let (message_tx, _) = broadcast::channel(100);
        Self {
            pool,
            online_users: Arc::new(RwLock::new(HashMap::new())),
            message_tx,
        }
    }

    /// Mark user as online
    pub async fn user_online(&self, username: String, language: String) {
        let user = OnlineUser {
            username: username.clone(),
            language,
            last_seen: Utc::now(),
        };
        self.online_users.write().await.insert(username, user);
    }

    /// Mark user as offline
    pub async fn user_offline(&self, username: &str) {
        self.online_users.write().await.remove(username);
    }

    /// Get list of online users
    pub async fn get_online_users(&self) -> Vec<OnlineUser> {
        self.online_users.read().await.values().cloned().collect()
    }

    /// Update user's last seen timestamp
    pub async fn update_user_activity(&self, username: &str) {
        if let Some(user) = self.online_users.write().await.get_mut(username) {
            user.last_seen = Utc::now();
        }
    }
}
