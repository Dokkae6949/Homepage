package at.dokkae.homepage

import at.dokkae.homepage.config.Environment
import at.dokkae.homepage.extensions.Precompiled
import at.dokkae.homepage.repository.MessageRepository
import at.dokkae.homepage.repository.impls.JooqMessageRepository
import at.dokkae.homepage.templates.Index
import io.github.cdimascio.dotenv.dotenv
import org.flywaydb.core.Flyway
import org.http4k.core.HttpHandler
import org.http4k.core.Method.*
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.getFirst
import org.http4k.core.toParametersMap
import org.http4k.routing.bindHttp
import org.http4k.routing.bindSse
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.template.JTETemplates
import org.http4k.template.ViewModel
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
) : ViewModel {
    init {
        require(author.length <= 31) { "Author must be 31 characters or less" }
        require(content.length <= 255) { "Content must be 255 characters or less" }
    }


    override fun template(): String = "partials/Message"
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
    val renderer = JTETemplates().Precompiled("build/generated-resources/jte")

    val indexHandler: HttpHandler = {
        Response(Status.OK).body(renderer(Index(messageRepository.findAll())))
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
        "/" bindHttp GET to indexHandler,

        "/messages" bindHttp POST to { req ->
            val params = req.form().toParametersMap()
            val author = params.getFirst("author").takeIf { !it.isNullOrBlank() } ?: "Anonymous"
            val message = params.getFirst("message")

            if (message == null) {
                Response(Status.BAD_REQUEST)
            } else {
                val msg = Message(author, message)
                val sseMsg = SseMessage.Data(renderer(msg))

                messageRepository.save(msg)
                subscribers.forEach {
                    thread { it.send(sseMsg) }
                }

                Response(Status.CREATED)
            }
        }
    )

    poly(http, sse).asServer(Jetty(port = env.port)).start()

    println("Server started on http://${env.host}:${env.port}")
}
