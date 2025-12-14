package at.dokkae.homepage

import at.dokkae.homepage.config.Env
import at.dokkae.homepage.config.Environment
import at.dokkae.homepage.extensions.Precompiled
import at.dokkae.homepage.repository.MessageRepository
import at.dokkae.homepage.repository.impls.JooqMessageRepository
import at.dokkae.homepage.templates.IndexTemplate
import at.dokkae.homepage.templates.MessageTemplate
import io.github.cdimascio.dotenv.dotenv
import org.flywaydb.core.Flyway
import org.http4k.core.HttpHandler
import org.http4k.core.Method.*
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.getFirst
import org.http4k.core.toParametersMap
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bindHttp
import org.http4k.routing.bindSse
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.static
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.template.JTETemplates
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

fun migrateDatabase(env: Environment) {
    val flyway = Flyway.configure()
        .dataSource(env.dbUrl, env.dbUsername, env.dbPassword)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true) // optional: creates baseline if no schema history exists
        .load()

    val result = flyway.migrate()
    println("Migrated ${result.migrationsExecuted} migration${if (result.migrationsExecuted != 1) "s" else ""}")
}

data class Message(
    val author: String,
    val content: String,

    val id: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
) {
    init {
        require(author.length <= 31) { "Author must be 31 characters or less" }
        require(content.length <= 255) { "Content must be 255 characters or less" }
    }
}

fun main() {
    val env = Environment.load(dotenv {
        ignoreIfMissing = true
        ignoreIfMalformed = true
    })

    if (env.dbMigrate) {
        migrateDatabase(env)
    }

    val connection = DriverManager.getConnection(env.dbUrl, env.dbUsername, env.dbPassword)
    val dslContext = DSL.using(connection, SQLDialect.POSTGRES)
    val messageRepository: MessageRepository = JooqMessageRepository(dslContext)

    val subscribers = CopyOnWriteArrayList<Sse>()
    val renderer = when (env.appEnv) {
        Env.DEVELOPMENT -> {
            println("🔥 Hot-Reloading JTE templates")
            JTETemplates().HotReload("src/main/kte")
        }
        Env.PRODUCTION -> {
            println("📦 Loading pre-compiled JTE templates")
            JTETemplates().Precompiled("build/generated-resources/jte")
        }
    }

    val indexHandler: HttpHandler = {
        Response(Status.OK).body(renderer(IndexTemplate(messageRepository.findAll())))
    }

    val sse = sse(
        "/message-events" bindSse { req ->
            SseResponse { sse ->
                subscribers.add(sse)

                sse.onClose { subscribers.remove(sse) }
            }
        }
    )

    val http = routes(
        static(ResourceLoader.Classpath("static")),

        "/" bindHttp GET to indexHandler,

        "/messages" bindHttp POST to { req ->
            try {
                val params = req.form().toParametersMap()
                val author = params.getFirst("author").takeIf { !it.isNullOrBlank() } ?: "Anonymous"
                val message = params.getFirst("message")

                if (message == null) {
                    Response(Status.BAD_REQUEST)
                } else {
                    val msg = Message(author, message)
                    val sseMsg = SseMessage.Data(renderer(MessageTemplate(msg)))

                    messageRepository.save(msg)
                    subscribers.forEach {
                        thread { it.send(sseMsg) }
                    }

                    Response(Status.CREATED)
                }
            } catch (ex: Exception) {
                println("Failed to receive message: ${ex.toString()} ${ex.message}")

                Response(Status.INTERNAL_SERVER_ERROR)
            }
        }
    )

    poly(http, sse).asServer(Jetty(port = env.appPort)).start()

    println("Server started on http://${env.appDomain}:${env.appPort}")
}
