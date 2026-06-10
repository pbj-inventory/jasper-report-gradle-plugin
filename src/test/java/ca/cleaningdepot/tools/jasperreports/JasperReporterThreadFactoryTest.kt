package ca.cleaningdepot.tools.jasperreports

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

internal class JasperReporterThreadFactoryTest {
    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testThreadNumbering() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ca.cleaningdepot.tools.jasperreports-gradle-plugin")
        val tasks = arrayListOf(ThreadNameRetrieverTask(), ThreadNameRetrieverTask(), ThreadNameRetrieverTask())

        val config = project.extensions.getByType(JasperPluginExtension::class.java)
        val classpath = config.additionalClasspath.orNull?.map { it.toURI().toURL() }?.toTypedArray()
        val executorService = Executors.newFixedThreadPool(2, JasperReporterThreadFactory(classpath))
        val output = executorService.invokeAll(tasks)

        Assertions.assertTrue(output[0].get()?.startsWith(JasperReporterThreadFactory.THREAD_PREFIX) ?: false)
        Assertions.assertTrue(output[1].get()?.startsWith(JasperReporterThreadFactory.THREAD_PREFIX) ?: false)
        Assertions.assertTrue(output[2].get()?.startsWith(JasperReporterThreadFactory.THREAD_PREFIX) ?: false)


        val threadNumbers = arrayListOf<Int?>()

        for (future in output) {
            threadNumbers.add(future.get()?.substring(JasperReporterThreadFactory.THREAD_PREFIX.length)?.toInt())
        }

        Assertions.assertTrue(threadNumbers[0]!! < threadNumbers[1]!!)
        Assertions.assertTrue(threadNumbers[2]!! <= threadNumbers[1]!!)
    }

    private class ThreadNameRetrieverTask : Callable<String> {
        @Throws(Exception::class)
        override fun call(): String {
            Thread.sleep(500)
            return Thread.currentThread().name
        }
    }
}
