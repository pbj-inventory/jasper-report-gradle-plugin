package ca.cleaningdepot.tools.jasperreports

import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JasperCompileManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.*

/**
 * A task that compiles a Jasper sourcefile.
 */
class CompileCallable
/**
 * @param source The source file.
 * @param destination The destination file.
 */ internal constructor(private val source: Path, private val destination: Path) :
    Callable<Unit> {
    /**
     * Compile the source file.
     *
     * @return Debug output of the compile action.
     * @throws Exception when anything goes wrong while compiling.
     */
    @Throws(Exception::class)
    override fun call() {
        try {
            this.destination.parent.createDirectories()
            this.destination.outputStream().buffered().use { out ->
                this.source.inputStream().buffered().use { `in` ->
                    compiler.compileToStream(`in`, out)
                    if (verbose) {
                        LOGGER.info("Compiling source file {}", this.source.absolutePathString())
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Could not compile source file {}", this.source.absolutePathString(), e)
            this.destination.deleteIfExists()
            throw JRException("Could not compile " + this.source.absolutePathString(), e)
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CompileCallable::class.java)

        private var verbose: Boolean = false
        private lateinit var compiler: JasperCompileManager

        fun init(compiler: JasperCompileManager, verbose: Boolean) {
            this.compiler = compiler
            this.verbose = verbose
        }
    }
}
