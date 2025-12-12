package at.dokkae.homepage.extensions

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import org.http4k.template.JTETemplates
import org.http4k.template.ViewModel
import org.http4k.template.ViewNotFound
import java.io.File

fun JTETemplates.Precompiled(classTemplateDir: String) =
    fun(viewModel: ViewModel): String {
        val templateName = viewModel.template() + ".kte"

        val templateEngine = TemplateEngine.createPrecompiled(File(classTemplateDir).toPath(), ContentType.Html)

        return if (templateEngine.hasTemplate(templateName))
            StringOutput().also { templateEngine.render(templateName, viewModel, it); }.toString()
        else throw ViewNotFound(viewModel)
    }