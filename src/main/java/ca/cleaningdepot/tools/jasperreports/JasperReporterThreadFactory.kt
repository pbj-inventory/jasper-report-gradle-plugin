package ca.cleaningdepot.tools.jasperreports

import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread factory the compile threads. Sets the thread name and marks it as a daemon thread.
 */
class JasperReporterThreadFactory(val classpath: Array<URL>?) : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, THREAD_PREFIX + THREAD_COUNTER.incrementAndGet())
        thread.isDaemon = true
        if (this.classpath != null) {
            thread.contextClassLoader = URLClassLoader(this.classpath, thread.contextClassLoader)
        }
        return thread
    }

    companion object {
        const val THREAD_PREFIX: String = "jasper-compiler-"
        private val THREAD_COUNTER = AtomicInteger()
    }
}
