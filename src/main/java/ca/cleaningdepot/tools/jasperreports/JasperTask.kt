package ca.cleaningdepot.tools.jasperreports

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
open class JasperTask : DefaultTask() {
    @get:Input
    lateinit var configuration: JasperPluginExtension

    @TaskAction
    fun run() {
        JasperReportCompiler(this.configuration).compileReports()
    }
}
