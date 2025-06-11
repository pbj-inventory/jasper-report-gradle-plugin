package ca.cleaningdepot.tools.jasperreports

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType

open class JasperTask : DefaultTask() {
    @TaskAction
    fun run() {
        val extension = project.extensions.getByType<JasperPluginExtension>()
        JasperReportCompiler(extension).compileReports()
    }
}
