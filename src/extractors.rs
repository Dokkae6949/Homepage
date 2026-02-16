use crate::error::{AppError, AppResult};
use crate::i18n::Translator as I18nTranslator;
use crate::models::Session;
use crate::state::{AppState, AppStateWithKey};
use axum::{
    async_trait,
    extract::{FromRef, FromRequestParts},
    http::request::Parts,
};
use axum_extra::extract::{cookie::Key, PrivateCookieJar};

const SESSION_COOKIE_NAME: &str = "session";

/// Extractor for authenticated user
#[derive(Debug, Clone)]
pub struct AuthenticatedUser {
    pub username: String,
    pub language: String,
    pub session_id: uuid::Uuid,
}

#[async_trait]
impl<S> FromRequestParts<S> for AuthenticatedUser
where
    AppStateWithKey: FromRef<S>,
    Key: FromRef<S>,
    S: Send + Sync,
{
    type Rejection = AppError;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> AppResult<Self> {
        let jar = PrivateCookieJar::<Key>::from_request_parts(parts, state)
            .await
            .map_err(|e| AppError::Session(format!("Failed to extract cookies: {}", e)))?;

        let cookie = jar
            .get(SESSION_COOKIE_NAME)
            .ok_or_else(|| AppError::Session("No session cookie found".to_string()))?;

        let session: Session = serde_json::from_str(cookie.value())
            .map_err(|e| AppError::Session(format!("Invalid session data: {}", e)))?;

        Ok(AuthenticatedUser {
            username: session.username,
            language: session.language,
            session_id: session.id,
        })
    }
}

/// Extractor for translations
#[derive(Clone)]
pub struct Translator {
    language: String,
}

impl Translator {
    pub fn new(language: String) -> Self {
        Self { language }
    }

    pub fn translate(&self, key: &str) -> String {
        // For now, return simple translations
        // In production, you'd load from fluent files
        let translator = I18nTranslator::new();
        translator.translate(&self.language, key, None)
    }

    pub fn language(&self) -> &str {
        &self.language
    }

    pub fn supported_languages(&self) -> Vec<String> {
        vec!["en".to_string(), "de".to_string()]
    }
}

#[async_trait]
impl<S> FromRequestParts<S> for Translator
where
    AppStateWithKey: FromRef<S>,
    Key: FromRef<S>,
    S: Send + Sync,
{
    type Rejection = AppError;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> AppResult<Self> {
        // Try to get language from session, default to 'en'
        let language = if let Ok(user) = AuthenticatedUser::from_request_parts(parts, state).await {
            user.language
        } else {
            "en".to_string()
        };

        Ok(Translator::new(language))
    }
}
