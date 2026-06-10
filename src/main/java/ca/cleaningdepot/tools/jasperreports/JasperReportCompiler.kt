package ca.cleaningdepot.tools.jasperreports

import net.sf.jasperreports.engine.*
import net.sf.jasperreports.engine.design.JRCompiler
import org.gradle.api.file.FileSystemLocationProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.io.path.*

class JasperReportCompiler(private val configuration: JasperPluginExtension) {
    val classpath by lazy {
        val files = this.configuration.additionalClasspath.orNull ?: return@lazy null
        val classpath = files.mapNotNull {
            val file = it.toPath()
            if (file.notExists()) {
                LOGGER.error("Error setting classpath $file")
                return@mapNotNull null
            }
            file.toUri().toURL()
        }.toTypedArray()
        if (classpath.isEmpty()) return@lazy null
        return@lazy classpath
    }
    val context by lazy {
        val thread = Thread.currentThread()
        if (this.classpath != null) {
            thread.contextClassLoader = URLClassLoader(this.classpath, thread.contextClassLoader)
        }

        val context = SimpleJasperReportsContext()

        val compiler = this.configuration.compiler.orNull
        if (!compiler.isNullOrBlank()) {
            context.setProperty(JRCompiler.COMPILER_PREFIX + JRReport.LANGUAGE_JAVA, compiler)
        }

        val properties = this.configuration.additionalProperties.orNull
        if (properties != null) {
            for ((key, value) in properties) {
                context.setProperty(key, value)
            }
        }

        context
    }

    @Throws(Exception::class)
    fun compileReports() {
        if (this.configuration.skip.get()) {
            LOGGER.info("Compiling Jasper reports is skipped.")
            return
        }

        this.checkOutDirWritable()

        val files = this.getFilesToCompile()
        if (files.isEmpty()) {
            LOGGER.info("Nothing to compile - all Jasper reports are up to date")
            return
        }

        LOGGER.info("Compiling {} Jasper reports design files.", files.size)

        val tasks = this.getTasks(files, this.context)
        if (tasks.isEmpty()) {
            LOGGER.info("Nothing to compile")
            return
        }

        this.executeTasks(tasks, this.classpath)
    }

    /**
     * Check if the output directory exist and is writable. If not, try to
     * create an output dir and see if that is writable.
     *
     * @throws Exception When the output directory is not writable
     */
    @Throws(Exception::class)
    private fun checkOutDirWritable() {
        val outputDir = this.configuration.outputDirectory.toPath()
        if (outputDir.notExists()) {
            outputDir.createDirectories()
        }
        if (!outputDir.isWritable()) {
            throw Exception("Output folder is not writable: " + outputDir.absolute())
        }
        if (this.configuration.verbose.get()) {
            LOGGER.info("Output dir check OK")
        }
    }

    /**
     * Determines source files to be compiled.
     *
     * @return set of jxml files to compile
     *
     * @throws Exception When there's trouble with the input
     */
    @Throws(Exception::class)
    private fun getFilesToCompile(): Map<Path, Path> {
        val sourceDirectory = this.configuration.sourceDirectory.toPath()
        if (!sourceDirectory.isDirectory()) {
            require(!this.configuration.failOnMissingSourceDirectory.get()) {
                "Configured source directory $sourceDirectory is not a directory"
            }
            LOGGER.warn(
                "Configured source directory {} is not a directory, skipping JasperReports reports compilation.",
                sourceDirectory
            )
            return emptyMap()
        }
        val inputExt = this.configuration.sourceFileExt.get()
        @OptIn(ExperimentalPathApi::class)//
        return sourceDirectory.walk().mapNotNull { inputFile ->
            // exclude directories
            if (!inputFile.isRegularFile()) return@mapNotNull null
            // exclude files that don't end with '.jrxml'
            if (!inputFile.name.endsWith(inputExt)) return@mapNotNull null
            val outputFile = this.getDestinationFile(inputFile)
            // include files that don't have an output file
            if (outputFile.notExists()) return@mapNotNull inputFile to outputFile
            // include input files that have been modified more recently the output file
            if (inputFile.getLastModifiedTime() > outputFile.getLastModifiedTime()) return@mapNotNull inputFile to outputFile
            return@mapNotNull null
        }.toMap()
    }

    @Throws(Exception::class)
    private fun getTasks(files: Map<Path, Path>, context: JasperReportsContext): List<CompileCallable> {
        val compiler = JasperCompileManager.getInstance(context)
        val verbose = this.configuration.verbose.get()
        CompileCallable.init(compiler, verbose)
        return files.map { (source, destination) -> CompileCallable(source, destination) }
    }

    @Throws(Exception::class)
    private fun executeTasks(tasks: List<CompileCallable>, classpath: Array<URL>?) {
        val threadPool = Executors.newFixedThreadPool(
            this.configuration.numberOfThreads.get(), JasperReporterThreadFactory(classpath)
        )
        try {
            val startTime = System.currentTimeMillis()
            val output = threadPool.invokeAll(tasks)
            val duration = System.currentTimeMillis() - startTime
            LOGGER.info("Generated {} jasper reports in {} seconds", output.size, duration / 1000.0)
            for (future in output) {
                future.get()
            }
        } catch (e: InterruptedException) {
            LOGGER.error("Failed to compile Japser reports: Interrupted!", e)
            throw Exception("Error while compiling Jasper reports", e)
        } catch (e: ExecutionException) {
            if (e.cause is JRException) {
                throw Exception(ERROR_JRE_COMPILE_ERROR, e)
            }
            throw Exception("Error while compiling Jasper reports", e)
        } finally {
            threadPool.shutdown()
        }
    }

    @Throws(Exception::class)
    private fun getDestinationFile(sourceFile: Path): Path {
        val sourceDir = this.configuration.sourceDirectory.toPath().toRealPath()
        val outputDir = this.configuration.outputDirectory.toPath().toRealPath()
        val outFile = outputDir.resolve(sourceDir.relativize(sourceFile.toRealPath()))
        val sourceExt = this.configuration.sourceFileExt.get()
        val outputExt = this.configuration.outputFileExt.get()
        return outFile.resolveSibling(outFile.name.removeSuffix(sourceExt) + outputExt)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(JasperReportCompiler::class.java)

        const val ERROR_JRE_COMPILE_ERROR: String =
            "Some Jasper reports could not be compiled. See log above for details."
    }
}

private fun FileSystemLocationProperty<*>.toPath() = this.get().asFile.toPath()
