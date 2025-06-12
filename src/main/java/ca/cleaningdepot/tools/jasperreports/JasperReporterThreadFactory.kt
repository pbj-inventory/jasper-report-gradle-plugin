package ca.cleaningdepot.tools.jasperreports

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread factory the compile threads. Sets the thread name and marks it as a daemon thread.
 */
class JasperReporterThreadFactory(val configuration: JasperPluginExtension) : ThreadFactory {
    private val classpath: Array<URL> by lazy {
        val files = configuration.additionalClasspath.orNull
        if (files == null || files.isEmpty) return@lazy emptyArray()
        val classpath = arrayListOf<URL>()
        for (file in files) {
            try {
                if (!file.exists()) throw FileNotFoundException(file.absolutePath)
                classpath.add(file.toURI().toURL())
            } catch (e: Exception) {
                LOGGER.error("Error setting classpath $file ${e.message}")
            }
        }
        classpath.toTypedArray()
    }

    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, THREAD_PREFIX + THREAD_COUNTER.incrementAndGet())
        thread.setDaemon(true)
        if (classpath.isNotEmpty()) thread.contextClassLoader = URLClassLoader(classpath, thread.contextClassLoader)
        return thread
    }

    companion object {
        const val THREAD_PREFIX: String = "jasper-compiler-"

        private val LOGGER: Logger = LoggerFactory.getLogger(JasperReporterThreadFactory::class.java)
        private val THREAD_COUNTER = AtomicInteger()
    }
}
