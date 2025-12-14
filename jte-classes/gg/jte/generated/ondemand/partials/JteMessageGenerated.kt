@file:Suppress("ktlint")
package gg.jte.generated.ondemand.partials
import at.dokkae.homepage.templates.MessageTemplate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
@Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
class JteMessageGenerated {
companion object {
	@JvmField val JTE_NAME = "partials/Message.kte"
	@JvmField val JTE_LINE_INFO = intArrayOf(0,0,0,1,2,3,4,6,6,6,6,6,8,8,8,9,9,10,10,14,15,15,15,15,18,21,21,21,24,24,24,24,24,24,28,30,30,30,34,34,34,6,6,6,6,6)
	@JvmStatic fun render(jteOutput:gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor:gg.jte.html.HtmlInterceptor?, model:MessageTemplate) {
		jteOutput.writeContent("\n")
		val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
		jteOutput.writeContent("\n")
		val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
		jteOutput.writeContent("\n")
		val borderColors = listOf("red", "orange", "yellow", "green", "blue", "pink" )
		jteOutput.writeContent("\n\n<div class=\"message-group mb-3 animate-slide-in\">\n    <div class=\"flex relative px-3 py-1 hover:bg-neutral-800/30 rounded transition-colors\">\n        ")
		jteOutput.writeContent("\n        <div class=\"absolute left-0 top-1/2 -translate-y-1/2 w-1 h-3/4 rounded-r message-border-")
		jteOutput.setContext("div", "class")
		jteOutput.writeUserContent(borderColors[model.message.id.hashCode().absoluteValue % borderColors.size])
		jteOutput.setContext("div", null)
		jteOutput.writeContent("\"></div>\n\n        <div class=\"flex-1 pl-3 text-ellipsis text-wrap break-all\">\n            ")
		jteOutput.writeContent("\n            <div class=\"flex flex-wrap items-baseline gap-2 mb-1\">\n                <span class=\"font-semibold text-white\">\n                    ")
		jteOutput.setContext("span", null)
		jteOutput.writeUserContent(model.message.author)
		jteOutput.writeContent("\n                </span>\n                <span class=\"text-xs text-neutral-400\">\n                    ")
		jteOutput.setContext("span", null)
		jteOutput.writeUserContent(dateFormatter.format(model.message.createdAt))
		jteOutput.writeContent(" • ")
		jteOutput.setContext("span", null)
		jteOutput.writeUserContent(timeFormatter.format(model.message.createdAt))
		jteOutput.writeContent("\n                </span>\n            </div>\n\n            ")
		jteOutput.writeContent("\n            <div class=\"text-neutral-200\">\n                ")
		jteOutput.setContext("div", null)
		jteOutput.writeUserContent(model.message.content)
		jteOutput.writeContent("\n            </div>\n        </div>\n    </div>\n</div>")
	}
	@JvmStatic fun renderMap(jteOutput:gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor:gg.jte.html.HtmlInterceptor?, params:Map<String, Any?>) {
		val model = params["model"] as MessageTemplate
		render(jteOutput, jteHtmlInterceptor, model);
	}
}
}
