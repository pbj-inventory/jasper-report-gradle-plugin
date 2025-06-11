package ca.cleaningdepot.tools.jasperreports

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class JasperTask : DefaultTask() {
    @TaskAction
    fun run() {
        val extension = project.extensions.getByType(JasperPluginExtension::class.java)
        JasperReportCompiler(extension).compileReports()
    }
}
