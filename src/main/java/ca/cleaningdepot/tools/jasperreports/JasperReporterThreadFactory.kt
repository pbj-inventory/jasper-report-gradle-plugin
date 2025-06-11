package ca.cleaningdepot.tools.jasperreports

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread factory the compile threads. Sets the thread name and marks it as a daemon thread.
 */
class JasperReporterThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, THREAD_PREFIX + THREAD_COUNTER.incrementAndGet())
        thread.setDaemon(true)
        return thread
    }

    companion object {
        const val THREAD_PREFIX: String = "jasper-compiler-"

        private val THREAD_COUNTER = AtomicInteger()
    }
}
