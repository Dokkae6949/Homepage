package at.dokkae.homepage.repository.impls

import at.dokkae.homepage.Message
import at.dokkae.homepage.generated.jooq.tables.records.MessageRecord
import at.dokkae.homepage.generated.jooq.tables.references.MESSAGE
import at.dokkae.homepage.repository.MessageRepository
import org.jooq.DSLContext
import org.jooq.impl.DSL

class JooqMessageRepository(
    private val dslContext: DSLContext
) : MessageRepository {
    override fun save(message: Message): Message = dslContext.transactionResult { config ->
        val ctx = DSL.using(config)

        ctx.insertInto(MESSAGE)
            .set(MESSAGE.ID, message.id)
            .set(MESSAGE.AUTHOR, message.author)
            .set(MESSAGE.CONTENT, message.content)
            .onDuplicateKeyUpdate()
            .set(MESSAGE.AUTHOR, message.author)
            .set(MESSAGE.CONTENT, message.content)
            .returning()
            .fetchOne()!!
            .toMessage()
    }

    override fun findAll(): List<Message> = dslContext.selectFrom(MESSAGE)
        .orderBy(MESSAGE.CREATED_AT.desc())
        .fetch { it.toMessage() }


    private fun MessageRecord.toMessage(): Message = Message(
        id = this.id,
        author = this.author,
        content = this.content,
        createdAt = this.createdAt!!.toInstant(),
        updatedAt = this.updatedAt?.toInstant(),
    )
}