package at.dokkae.homepage.templates

import at.dokkae.homepage.Message
import org.http4k.template.ViewModel

data class MessageTemplate(val message: Message) : ViewModel {
    override fun template(): String = "partials/Message"
}