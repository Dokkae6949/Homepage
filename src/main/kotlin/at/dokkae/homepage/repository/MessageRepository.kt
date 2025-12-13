package at.dokkae.homepage.repository

import at.dokkae.homepage.Message

interface MessageRepository {
    fun save(message: Message): Message
    fun findAll(): List<Message>
}