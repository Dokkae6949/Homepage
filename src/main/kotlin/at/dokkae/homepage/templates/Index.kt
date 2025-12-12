package at.dokkae.homepage.templates

import at.dokkae.homepage.Message
import org.http4k.template.ViewModel

data class Index(val messages: List<Message> = listOf()) : ViewModel {
    override fun template(): String = "Index"
}