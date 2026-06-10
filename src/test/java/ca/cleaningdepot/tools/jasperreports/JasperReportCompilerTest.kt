package ca.cleaningdepot.tools.jasperreports

import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.util.NullOutputStream
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries

internal class JasperReportCompilerTest {
    @Test
    fun pluginAddsTaskToProject() {
        val project = createProject()
        Assertions.assertInstanceOf(JasperTask::class.java, project.tasks.getByName("jasperreports"))
    }

    /**
     * Test the normal generation of Jasper reports. The files are retrieved from the official
     * jasper examples folder. No errors or warnings should occur.
     *
     * @throws Exception When an unexpexted error occures.
     */
    @Test
    @Throws(Exception::class)
    fun testValidReportGeneration() {
        val project = createProject()
        val configuration = project.getExtension()
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/sampleReports").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testValidReportGeneration").absoluteFile)

        JasperReportCompiler(configuration).compileReports()

        Assertions.assertEquals(
            configuration.sourceDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.sourceFileExt.get()}").size,
            configuration.outputDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.outputFileExt.get()}").size,
            "Files from sourcefolder do not correspond to files in the destinationFolder"
        )
        assertAllFilesAreCompiled(configuration.sourceDirectory.toFile(), configuration.outputDirectory.toFile())
    }

    @Test
    @Throws(Exception::class)
    fun testCompilerConfiguration() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/sampleReports").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testCompilerConfiguration").absoluteFile)
        configuration.compiler.set(TestCompilerConfigurationCompiler::class.java.name)

        // when
        val compiler = JasperReportCompiler(configuration)
        compiler.compileReports()

        // then
        // test that a custom compiler works
        Assertions.assertEquals(
            configuration.sourceDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.sourceFileExt.get()}").size,
            configuration.outputDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.outputFileExt.get()}").size,
            "Files from sourcefolder do not correspond to files in the destinationFolder"
        )
        assertAllFilesAreCompiled(
            configuration.sourceDirectory.toFile(), configuration.outputDirectory.toFile()
        )
        Assertions.assertTrue(compiler.context.properties.containsKey("testcompiler.called"))

        // given
        configuration.outputDirectory.set(File("./build/unitTestReports/testCompilerConfiguration2").absoluteFile)
        configuration.compiler.set("compiler")

        // then
        // test that an invalid configuration leads to a broken build.
        Assertions.assertThrows(Exception::class.java) { JasperReportCompiler(configuration).compileReports() }

        // given
        configuration.outputDirectory.set(File("./build/unitTestReports/testCompilerConfiguration3").absoluteFile)
        configuration.compiler.value(null as String?)

        // when
        JasperReportCompiler(configuration).compileReports()

        // then
        // test that a custom compiler works
        Assertions.assertEquals(
            configuration.sourceDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.sourceFileExt.get()}").size,
            configuration.outputDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.outputFileExt.get()}").size,
            "Files from sourcefolder do not correspond to files in the destinationFolder"
        )
        assertAllFilesAreCompiled(configuration.sourceDirectory.toFile(), configuration.outputDirectory.toFile())
    }

    /**
     * Test the normal generation of Jasper reports with additional properties. The files are
     * retrieved from the official jasper examples folder. No errors or warnings should occur.
     *
     * @throws Exception When an unexpexted error occures.
     */
    @Test
    @Throws(Exception::class)
    fun testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationAndExportToPdfExpectNoErrors() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/sampleReports").absoluteFile)
        configuration.outputDirectory.set(
            File(
                "build/unitTestReports/testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationAndExportToPdfExpectNoErrors"
            )
        )
        val properties = hashMapOf<String, String>()
        properties.put("net.sf.jasperreports.awt.ignore.missing.font", "true")
        properties.put("net.sf.jasperreports.default.pdf.font.name", "Courier")
        properties.put("net.sf.jasperreports.default.pdf.encoding", "UTF-8")
        properties.put("net.sf.jasperreports.default.pdf.embedded", "true")
        configuration.additionalProperties.set(properties)

        // when
        val compiler = JasperReportCompiler(configuration)
        compiler.compileReports()

        // then
        val defaultPdfFontName = compiler.context.getProperty("net.sf.jasperreports.default.pdf.font.name")
        val pdfEmbeddedValue = compiler.context.getProperty("net.sf.jasperreports.default.pdf.embedded")

        Assertions.assertEquals(
            configuration.sourceDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.sourceFileExt.get()}").size,
            configuration.outputDirectory.toFile().toPath()
                .listDirectoryEntries("*${configuration.outputFileExt.get()}").size,
            "Files from sourcefolder do not correspond to files in the destinationFolder"
        )
        assertAllFilesAreCompiled(configuration.sourceDirectory.toFile(), configuration.outputDirectory.toFile())
        Assertions.assertEquals("Courier", defaultPdfFontName, "default pdf font name has not been set properly")
        Assertions.assertEquals(
            "true", pdfEmbeddedValue, "net.sf.jasperreports.default.pdf.embedded has not been set properly"
        )
        Assertions.assertTrue(configuration.outputDirectory.toFile().isDirectory, "Destination is not a directory")
        val testFiles = configuration.outputDirectory.toFile().listFiles { pathname ->
            pathname.toString().contains("PlainTextReportWithDefaultFontReport")
        }.toList()

        Assertions.assertEquals(1, testFiles.size)

        // when
        val file =
            File(configuration.outputDirectory.toFile().path + "/" + "PlainTextReportWithDefaultFontReport.jasper")
        try {
            val print = JasperFillManager.fillReport(FileInputStream(file), HashMap<String?, Any?>())
            JasperExportManager.exportReportToPdfStream(print, NullOutputStream())
        } catch (e: Exception) {
            Assertions.fail<Any?>("Unable to createpdf", e)
        }
    }

    /**
     * For this method to work all files need to be in one folder. The could be enhanced later to
     * also search all subfolders.
     */
    private fun assertAllFilesAreCompiled(sourceFolder: File, destinationFolder: File) {
        Assertions.assertTrue(sourceFolder.isDirectory, "Source folder is not a directory")
        Assertions.assertTrue(destinationFolder.isDirectory, "Destination is not a directory")
        val filenames: MutableSet<String?> = HashSet()
        for (file in sourceFolder.listFiles()) {
            if (file.isFile && file.name.endsWith(".jrxml")) {
                filenames.add(file.name.removeSuffix(".jrxml"))
            }
        }
        for (file in destinationFolder.listFiles()) {
            if (file.isFile && file.name.endsWith(".jasper")) {
                filenames.remove(file.name.removeSuffix(".jasper"))
            }
        }
        Assertions.assertTrue(
            filenames.isEmpty(), "Files from sourcefolder do not correspond to files in the destinationFolder"
        )
    }

    /**
     * Test that an invalid Jasper file should stop the build completely by throwing an
     * [Exception].
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    @Throws(Exception::class)
    fun testInvalidFilesStopBuild() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.sourceDirectory.set(File("./src/test/resources/exampleFolders/brokenReports").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testInvalidFilesStopBuild").absoluteFile)

        try {
            JasperReportCompiler(configuration).compileReports()
            Assertions.fail<Any?>("An exception should have been thrown")
        } catch (e: Exception) {
            // then
            Assertions.assertEquals(JasperReportCompiler.ERROR_JRE_COMPILE_ERROR, e.message, e.stackTraceToString())
        }
    }

    /**
     * Test that skipping the plugin does not compile any Jasper file.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    @Throws(Exception::class)
    fun testSkipDoesntCompile() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.skip.set(true)
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/sampleReports").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testSkipDoesntCompile").absoluteFile)

        // when
        JasperReportCompiler(configuration).compileReports()

        // then
        Assertions.assertFalse(configuration.outputDirectory.toFile().exists(), "Output folder should not exist")
    }

    /**
     * Test that all files with an invalid suffix are not compiled.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    @Throws(Exception::class)
    fun testWrongSuffixDoesntCompile() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.skip.set(true)
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/wrongExtensions").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testWrongSuffixDoesntCompile").absoluteFile)

        // when
        JasperReportCompiler(configuration).compileReports()

        // then
        Assertions.assertEquals(0, configuration.outputDirectory.toFile().length(), "Output folder should be empty")
    }

    /**
     * Test that an empty folder doesn't create errors but just does nothing.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    @Throws(Exception::class)
    fun testEmptyDoesNothing() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.skip.set(true)
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/emptyFolder").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testEmptyDoesNothing").absoluteFile)

        // when
        JasperReportCompiler(configuration).compileReports()

        // then
        Assertions.assertEquals(0, configuration.outputDirectory.toFile().length(), "Output folder should be empty")
    }


    /**
     * Test that a non-existent sourceDirectory fails the build.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    @Throws(Exception::class)
    fun testNonExistentFolderStopBuild() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/nonExistentFolder").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testNonExistentFolderStopBuild").absoluteFile)

        try {
            // when
            JasperReportCompiler(configuration).compileReports()
            Assertions.fail<Any?>("An exception should have been thrown")
        } catch (e: IllegalArgumentException) {
            Assertions.assertTrue(e.message!!.contains("nonExistentFolder"), e.stackTraceToString())
        }
    }

    /**
     * Test that a non-existent sourceDirectory are just skipped if failOnMissingSourceDirectory=true.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    @Throws(Exception::class)
    fun testNonExistentFolderAllowed() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.failOnMissingSourceDirectory.set(false)
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/nonExistentFolder").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testNonExistentFolderAllowed").absoluteFile)

        // when
        JasperReportCompiler(configuration).compileReports()

        // then
        Assertions.assertEquals(0, configuration.outputDirectory.toFile().list().size, "Output folder should be empty")
    }

    /**
     * Test that the folder structure of the output is the same as the folder structure of the
     * input.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    @Throws(Exception::class)
    fun testFolderStructure() {
        // given
        val project = createProject()
        val configuration = project.getExtension()
        configuration.failOnMissingSourceDirectory.set(false)
        configuration.sourceDirectory.set(File("./build/resources/test/exampleFolders/folderStructure").absoluteFile)
        configuration.outputDirectory.set(File("./build/unitTestReports/testFolderStructure").absoluteFile)

        // when
        JasperReportCompiler(configuration).compileReports()

        // then
        val filenames = detectFolderStructure(configuration.outputDirectory.toFile())
        val relativePath = configuration.outputDirectory.toFile().absolutePath + '/'
        val fileMissing = "A file in the folderstructure is missing"
        Assertions.assertTrue(filenames.remove(File(relativePath + "LandscapeReport.jasper").absolutePath), fileMissing)
        Assertions.assertTrue(
            filenames.remove(File(relativePath + "level.1/level.2.1/LateOrdersReport.jasper").absolutePath), fileMissing
        )
        Assertions.assertTrue(
            filenames.remove(File(relativePath + "level.1/level.2.2/MasterReport.jasper").absolutePath), fileMissing
        )
        Assertions.assertTrue(
            filenames.remove(File(relativePath + "level.1/level.2.2/Level.3/LineChartReport.jasper").absolutePath),
            fileMissing
        )
        Assertions.assertTrue(filenames.isEmpty(), "There were more files found then expected")
    }

    private fun detectFolderStructure(folderToSearch: File): MutableSet<String?> {
        val set: MutableSet<String?> = HashSet()
        for (f in folderToSearch.listFiles()) {
            if (f.isDirectory) {
                set.addAll(detectFolderStructure(f))
            } else {
                set.add(f.absolutePath)
            }
        }
        return set
    }

    companion object {
        @BeforeAll
        @Throws(IOException::class)
        @JvmStatic
        fun beforeAll() {
            @OptIn(ExperimentalPathApi::class)//
            Path("build/unitTestReports").deleteRecursively()
        }
    }
}

private fun Project.getExtension(): JasperPluginExtension = this.extensions.getByType(JasperPluginExtension::class.java)
private fun createProject(): Project = ProjectBuilder.builder().build()
    .also { it.pluginManager.apply("ca.cleaningdepot.tools.jasperreports-gradle-plugin") }
