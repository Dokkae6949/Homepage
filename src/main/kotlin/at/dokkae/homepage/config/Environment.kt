package at.dokkae.homepage.config

import io.github.cdimascio.dotenv.Dotenv

data class Environment(
    val port: Int,
    val host: String,
    val dbUrl: String,
    val dbUsername: String,
    val dbPassword: String,
    val dbMigrate: Boolean,
) {
    companion object {
        /**
         * Returns a loaded Environment object instance.
         * @throws IllegalStateException if required environment variables were not found within the provided `dotenv` instance.
         */
        fun load(dotenv: Dotenv): Environment = Environment(
            port = requireEnv(dotenv, "PORT").toInt(),
            host = requireEnv(dotenv, "HOST"),
            dbUrl = requireEnv(dotenv, "DB_URL"),
            dbUsername = requireEnv(dotenv, "DB_USERNAME"),
            dbPassword = requireEnv(dotenv, "DB_PASSWORD"),
            dbMigrate = dotenv["DB_MIGRATE"]?.toBoolean() ?: false
        )

        private fun requireEnv(dotenv: Dotenv, key: String): String {
            return dotenv[key] ?: throw IllegalStateException(
                "Missing required environment variable: $key"
            )
        }
    }
}