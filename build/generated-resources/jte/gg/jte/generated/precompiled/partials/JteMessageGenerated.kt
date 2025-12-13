@file:Suppress("ktlint")
package gg.jte.generated.precompiled.partials
import at.dokkae.homepage.Message
@Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
class JteMessageGenerated {
companion object {
	@JvmField val JTE_NAME = "partials/Message.kte"
	@JvmField val JTE_LINE_INFO = intArrayOf(0,0,0,2,2,2,2,2,5,5,5,5,6,6,6,8,8,8,10,10,10,2,2,2,2,2)
	@JvmStatic fun render(jteOutput:gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor:gg.jte.html.HtmlInterceptor?, message:Message) {
		jteOutput.writeContent("\n<div class=\"message\">\n    <strong>")
		jteOutput.setContext("strong", null)
		jteOutput.writeUserContent(message.author)
		jteOutput.writeContent("</strong>:\n    ")
		jteOutput.setContext("div", null)
		jteOutput.writeUserContent(message.content)
		jteOutput.writeContent("\n    <span style=\"color:#888; font-size:.8rem;\">\n        (")
		jteOutput.setContext("span", null)
		jteOutput.writeUserContent(message.createdAt.toString())
		jteOutput.writeContent(")\n    </span>\n</div>")
	}
	@JvmStatic fun renderMap(jteOutput:gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor:gg.jte.html.HtmlInterceptor?, params:Map<String, Any?>) {
		val message = params["message"] as Message
		render(jteOutput, jteHtmlInterceptor, message);
	}
}
}
