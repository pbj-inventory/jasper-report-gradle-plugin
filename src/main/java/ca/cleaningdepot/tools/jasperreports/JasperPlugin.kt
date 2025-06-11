package ca.cleaningdepot.tools.jasperreports

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.slf4j.LoggerFactory

class JasperPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val configuration = project.extensions.create<JasperPluginExtension>("jasperreports")
        if (configuration.verbose.get()) {
            logConfiguration(configuration)
        }
        project.tasks.register<JasperTask>("jasperreports") { this.configuration = configuration }
    }

    private fun logConfiguration(configuration: JasperPluginExtension) {
        LOGGER.info("Generating Jasper reports")
        LOGGER.info("Output dir: {}", configuration.outputDirectory.toFile().canonicalPath)
        LOGGER.info("Source dir: {}", configuration.sourceDirectory.toFile().canonicalPath)
        LOGGER.info("Output ext: {}", configuration.outputFileExt.orNull)
        LOGGER.info("Source ext: {}", configuration.sourceFileExt.orNull)
        LOGGER.info("Additional properties: {}", configuration.additionalProperties.orNull)
        LOGGER.info("XML Validation: {}", configuration.xmlValidation.orNull)
        LOGGER.info("JasperReports Compiler: {}", configuration.compiler.orNull)
        LOGGER.info("Number of threads: {}", configuration.numberOfThreads.orNull)
        LOGGER.info("classpathElements: {}", configuration.classpathElements.orNull)
        LOGGER.info("Additional Classpath: {}", configuration.additionalClasspath.orNull)
        LOGGER.info("Source Scanner: {}", configuration.sourceScanner.orNull)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(JasperPlugin::class.java)
    }
}
