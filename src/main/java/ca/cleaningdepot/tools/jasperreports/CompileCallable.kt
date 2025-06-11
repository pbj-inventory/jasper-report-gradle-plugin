package ca.cleaningdepot.tools.jasperreports

import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JasperCompileManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.util.concurrent.Callable

/**
 * A task that compiles a Jasper sourcefile.
 */
class CompileCallable
/**
 * @param source The source file.
 * @param destination The destination file.
 * @param verbose If the output should be verbose.
 */ internal constructor(private val source: File, private val destination: File, private val verbose: Boolean) :
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
            BufferedOutputStream(FileOutputStream(destination)).use { out ->
                BufferedInputStream(FileInputStream(source)).use { `in` ->
                    JasperCompileManager.compileReportToStream(`in`, out)
                    if (verbose) {
                        LOGGER.info("Compiling source file {}", source.absolutePath)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Could not compile source file {}", source.absolutePath, e)
            if (destination.exists()) destination.delete()
            throw JRException("Could not compile " + source.absolutePath, e)
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CompileCallable::class.java)
    }
}
