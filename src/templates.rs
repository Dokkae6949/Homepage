use crate::error::AppResult;
use crate::i18n::Translator as I18nTranslator;
use minijinja::{Environment, Value};
use std::sync::Arc;

/// Template renderer
pub struct Templates {
    env: Environment<'static>,
    _translator: Arc<I18nTranslator>,
}

impl Templates {
    pub fn new(translator: Arc<I18nTranslator>) -> Self {
        let mut env = Environment::new();

        // Load templates from embedded strings
        env.add_template("login.html", include_str!("../templates/login.html"))
            .expect("Failed to add login template");
        env.add_template("chat.html", include_str!("../templates/chat.html"))
            .expect("Failed to add chat template");
        env.add_template("message.html", include_str!("../templates/message.html"))
            .expect("Failed to add message template");
        env.add_template(
            "online_user.html",
            include_str!("../templates/online_user.html"),
        )
        .expect("Failed to add online_user template");
        env.add_template("error.html", include_str!("../templates/error.html"))
            .expect("Failed to add error template");

        // Note: The translation filter creates a new translator on each call
        // This is acceptable for simplicity, but in production you might want to
        // cache translators or pass translation data through the context
        env.add_filter("t", |key: String, lang: String| -> String {
            let translator = I18nTranslator::new();
            translator.translate(&lang, &key, None)
        });

        Self {
            env,
            _translator: translator,
        }
    }

    pub fn render(&self, template_name: &str, context: Value) -> AppResult<String> {
        let tmpl = self
            .env
            .get_template(template_name)
            .map_err(|e| crate::error::AppError::Template(format!("Template not found: {}", e)))?;

        tmpl.render(context)
            .map_err(|e| crate::error::AppError::Template(format!("Render error: {}", e)))
    }
}
