use fluent::{FluentArgs, FluentBundle, FluentResource};
use std::collections::HashMap;
use unic_langid::LanguageIdentifier;

/// Translation manager for i18n
pub struct Translator {
    bundles: HashMap<String, FluentBundle<FluentResource>>,
    default_lang: String,
}

impl Translator {
    pub fn new() -> Self {
        let mut translator = Self {
            bundles: HashMap::new(),
            default_lang: "en".to_string(),
        };

        // Load translations for English and German only
        translator.load_language("en", include_str!("../locales/en.ftl"));
        translator.load_language("de", include_str!("../locales/de.ftl"));

        translator
    }

    fn load_language(&mut self, lang_code: &str, content: &str) {
        let lang_id: LanguageIdentifier = lang_code.parse().expect("Invalid language ID");
        let resource =
            FluentResource::try_new(content.to_string()).expect("Failed to parse FTL string");

        let mut bundle = FluentBundle::new(vec![lang_id]);
        bundle
            .add_resource(resource)
            .expect("Failed to add FTL resource");

        self.bundles.insert(lang_code.to_string(), bundle);
    }

    /// Translate a message key
    pub fn translate(&self, lang: &str, key: &str, args: Option<&FluentArgs>) -> String {
        let bundle = self
            .bundles
            .get(lang)
            .or_else(|| self.bundles.get(&self.default_lang))
            .expect("Default language bundle not found");

        let message = bundle.get_message(key).expect("Message not found");
        let pattern = message.value().expect("Message has no value");

        let mut errors = vec![];
        let value = bundle.format_pattern(pattern, args, &mut errors);

        if !errors.is_empty() {
            tracing::warn!("Translation errors for key '{}': {:?}", key, errors);
        }

        value.to_string()
    }

    /// Get list of supported languages
    pub fn supported_languages(&self) -> Vec<String> {
        self.bundles.keys().cloned().collect()
    }
}

impl Default for Translator {
    fn default() -> Self {
        Self::new()
    }
}
