package ca.cleaningdepot.tools.jasperreports

import net.sf.jasperreports.engine.DefaultJasperReportsContext
import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JRPropertiesUtil
import net.sf.jasperreports.engine.JRReport
import net.sf.jasperreports.engine.design.JRCompiler
import net.sf.jasperreports.engine.design.JRJdtCompiler
import net.sf.jasperreports.engine.xml.JRReportSaxParserFactory
import org.codehaus.plexus.compiler.util.scan.InclusionScanException
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class JasperReportCompiler(private val configuration: JasperPluginExtension) {
    @Throws(Exception::class)
    fun compileReports() {
        if (configuration.skip.get()) {
            LOGGER.info("Compiling Jasper reports is skipped.")
            return
        }

        checkOutDirWritable()

        val mapping: SourceMapping = SuffixMapping(configuration.sourceFileExt.get(), configuration.outputFileExt.get())
        val sources = jrxmlFilesToCompile(mapping)
        if (sources.isEmpty()) {
            LOGGER.info("Nothing to compile - all Jasper reports are up to date")
            return
        }

        LOGGER.info("Compiling {} Jasper reports design files.", sources.size)

        val tasks = generateTasks(sources, mapping)
        if (tasks.isEmpty()) {
            LOGGER.info("Nothing to compile")
            return
        }

        configureJasper()
        executeTasks(tasks)
    }

    /**
     * Determines source files to be compiled.
     *
     * @param mapping The source files
     *
     * @return set of jxml files to compile
     *
     * @throws Exception When there's trouble with the input
     */
    @Throws(Exception::class)
    private fun jrxmlFilesToCompile(mapping: SourceMapping): MutableSet<File> {
        if (!configuration.sourceDirectory.toFile().isDirectory()) {
            require(!configuration.failOnMissingSourceDirectory.get()) {
                "Configured source directory ${configuration.sourceDirectory.toFile()} is not a directory"
            }
            LOGGER.warn(
                "Configured source directory {} is not a directory, skipping JasperReports reports compilation.",
                configuration.sourceDirectory.toFile()
            )
            return mutableSetOf()
        }

        try {
            val scanner: SourceInclusionScanner = createSourceInclusionScanner()
            scanner.addSourceMapping(mapping)
            return scanner.getIncludedSources(
                configuration.sourceDirectory.toFile(), configuration.outputDirectory.toFile()
            )
        } catch (e: InclusionScanException) {
            throw Exception("Error scanning source root: '${configuration.sourceDirectory.get()}'.", e)
        }
    }

    /**
     * Check if the output directory exist and is writable. If not, try to
     * create an output dir and see if that is writable.
     *
     * @throws Exception When the output directory is not writable
     */
    @Throws(Exception::class)
    private fun checkOutDirWritable() {
        if (!configuration.outputDirectory.toFile().exists()) {
            checkIfOutputCanBeCreated()
            checkIfOutputDirIsWritable()
            if (configuration.verbose.get()) {
                LOGGER.info("Output dir check OK")
            }
        } else if (!configuration.outputDirectory.toFile().canWrite()) {
            throw Exception("The output dir exists but was not writable. Try running maven with the 'clean' goal.")
        }
    }

    private fun configureJasper() {
        val jrContext: DefaultJasperReportsContext = DefaultJasperReportsContext.getInstance()

        jrContext.setProperty(
            JRReportSaxParserFactory.COMPILER_XML_VALIDATION, configuration.xmlValidation.get().toString()
        )
        jrContext.setProperty(
            JRCompiler.COMPILER_PREFIX + JRReport.LANGUAGE_JAVA,
            if (configuration.compiler.orNull.isNullOrBlank()) JRJdtCompiler::class.java.getName() else configuration.compiler.get()
        )

        if (configuration.additionalProperties.isPresent) {
            val properties: JRPropertiesUtil = JRPropertiesUtil.getInstance(jrContext)
            for (additionalProperty in configuration.additionalProperties.get().entries) {
                properties.setProperty(additionalProperty.key, additionalProperty.value)
            }
        }
    }

    @Throws(Exception::class)
    private fun generateTasks(sources: MutableSet<File>, mapping: SourceMapping): MutableList<CompileCallable> {
        val tasks = LinkedList<CompileCallable>()
        try {
            val root: kotlin.String = configuration.sourceDirectory.toFile().getCanonicalPath()

            for (src in sources) {
                val srcName = getRelativePath(root, src)
                try {
                    val destination = mapping.getTargetFiles(configuration.outputDirectory.toFile(), srcName).first()
                    createDestination(destination.getParentFile())
                    tasks.add(CompileCallable(src, destination, configuration.verbose.get()))
                } catch (e: InclusionScanException) {
                    throw Exception("Error compiling report design : $src", e)
                }
            }
        } catch (e: IOException) {
            throw Exception(
                "Could not getCanonicalPath from source directory " + configuration.sourceDirectory.get(), e
            )
        }
        return tasks
    }

    @Throws(Exception::class)
    private fun createDestination(destinationDirectory: File) {
        if (!destinationDirectory.exists()) {
            if (destinationDirectory.mkdirs()) {
                LOGGER.debug("Created directory {}", destinationDirectory.getName())
            } else {
                throw Exception("Could not create directory " + destinationDirectory.getName())
            }
        }
    }

    @Throws(Exception::class)
    private fun executeTasks(tasks: MutableList<CompileCallable>) {
        val threadPool: ExecutorService = newThreadPool()
        try {
            val t1 = System.currentTimeMillis()
            val output = threadPool.invokeAll(tasks)
            val time = (System.currentTimeMillis() - t1)
            LOGGER.info("Generated {} jasper reports in {} seconds", output.size, time / 1000.0)
            checkForExceptions(output)
        } catch (e: InterruptedException) {
            LOGGER.error("Failed to compile Japser reports: Interrupted!", e)
            throw Exception("Error while compiling Jasper reports", e)
        } catch (e: ExecutionException) {
            if (e.cause is JRException) {
                throw Exception(ERROR_JRE_COMPILE_ERROR, e)
            } else {
                throw Exception("Error while compiling Jasper reports", e)
            }
        } finally {
            threadPool.shutdown()
        }
    }

    private fun newThreadPool(): ExecutorService {
        return Executors.newFixedThreadPool(
            configuration.numberOfThreads.get(), JasperReporterThreadFactory(configuration)
        )
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    private fun checkForExceptions(output: MutableList<Future<Unit>>) {
        for (future in output) {
            future.get()
        }
    }

    @Throws(Exception::class)
    private fun createSourceInclusionScanner(): SourceInclusionScanner {
        return if (configuration.sourceScanner.get().equals(StaleSourceScanner::class.java.getName())) {
            StaleSourceScanner()
        } else if (configuration.sourceScanner.get().equals(SimpleSourceInclusionScanner::class.java.getName())) {
            SimpleSourceInclusionScanner(mutableSetOf("**/*" + configuration.sourceFileExt.get()), mutableSetOf())
        } else {
            throw Exception("sourceScanner not supported: '" + configuration.sourceScanner.get() + "'.")
        }
    }

    @Throws(Exception::class)
    private fun checkIfOutputCanBeCreated() {
        if (!configuration.outputDirectory.toFile().mkdirs()) {
            throw Exception("Output folder ${configuration.outputDirectory.toFile().absolutePath} is not a folder")
        }
    }

    @Throws(Exception::class)
    private fun checkIfOutputDirIsWritable() {
        if (!configuration.outputDirectory.toFile().canWrite()) {
            throw Exception("Could not write to output folder: " + configuration.outputDirectory.toFile().absolutePath)
        }
    }

    @Throws(Exception::class)
    private fun getRelativePath(root: kotlin.String, file: File): kotlin.String {
        try {
            return file.getCanonicalPath().substring(root.length + 1)
        } catch (e: IOException) {
            throw Exception("Could not getCanonicalPath from file $file", e)
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(JasperReportCompiler::class.java)

        const val ERROR_JRE_COMPILE_ERROR: kotlin.String =
            "Some Jasper reports could not be compiled. See log above for details."
    }
}
