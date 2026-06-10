package ca.cleaningdepot.tools.jasperreports

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

abstract class JasperPluginExtension @Inject constructor(layout: ProjectLayout) {
    /**
     * This is the java compiler used
     */
    @get:Input
    abstract val compiler: Property<String>

    /**
     * This is where the .jasper files are written.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * This is where the xml report design files should be.
     */
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    /**
     * The extension of the source files to look for. Finds files with a .jrxml
     * extension by default.
     */
    @get:Input
    abstract val sourceFileExt: Property<String>

    /**
     * The extension of the compiled report files. Creates files with a .jasper
     * extension by default.
     *
     */
    @get:Input
    abstract val outputFileExt: Property<String>

    /**
     * Check the source files before compiling. Default value is true.
     *
     */
    @get:Input
    abstract val xmlValidation: Property<Boolean>

    /**
     * Set this to "true" to bypass compiling reports. Default value is false.
     *
     */
    @get:Input
    abstract val skip: Property<Boolean>

    /**
     * If verbose is true the plug-in will report which reports it is compiling
     * and which files are being skipped.
     *
     */
    @get:Input
    abstract val verbose: Property<Boolean>

    /**
     * The number of threads the reporting will use. Default is 4 which is good
     * for a lot of reports on a hard drive (instead of SSD). If you only have
     * a few, or if you have SSD, it might be faster to set it to 2.
     *
     */
    @get:Input
    abstract val numberOfThreads: Property<Int>

    /**
     * Use this parameter to add additional properties to the Jasper compiler.
     * For example.
     *
     * <pre>
     * {@code
     * <configuration>
     * 	...
     * 		<additionalProperties>
     * 			<net.sf.jasperreports.awt.ignore.missing.font>true
     * 			</net.sf.jasperreports.awt.ignore.missing.font>
     *          <net.sf.jasperreports.default.pdf.font.name>Courier</net.sf.jasperreports.default.pdf.font.name>
     *          <net.sf.jasperreports.default.pdf.encoding>UTF-8</net.sf.jasperreports.default.pdf.encoding>
     *          <net.sf.jasperreports.default.pdf.embedded>true</net.sf.jasperreports.default.pdf.embedded>
     * </additionalProperties>
     * </configuration>
     * }
     * </pre>
     *
     */
    @get:Input
    abstract val additionalProperties: MapProperty<String, String>

    /**
     * If failOnMissingSourceDirectory is on the plug-in will fail the build if
     * source directory does not exist. Default value is true.
     *
     */
    @get:Input
    abstract val failOnMissingSourceDirectory: Property<Boolean>

    /**
     * Provides the option to add additional JARs to the Classpath for compiling. This is handy in case you have
     * references to external Java-Beans in your JasperReports.
     *
     * <pre>
     * {@code
     * <configuration>
     *  ...
     *      <additionalClasspath>/web/lib/ServiceBeans.jar;/web/lib/WebForms.jar</additionalClasspath>
     *  ...
     * </configuration>
     * }
     * </pre>
     *
     */
    @get:InputFiles
    @get:Classpath
    abstract val additionalClasspath: Property<FileCollection>

    init {
        compiler.convention("net.sf.jasperreports.jdt.JRJdtCompiler")
        outputDirectory.convention(layout.buildDirectory.dir("jasper"))
        sourceDirectory.convention(layout.projectDirectory.dir("src/main/jasperreports"))
        sourceFileExt.convention(".jrxml")
        outputFileExt.convention(".jasper")
        xmlValidation.convention(true)
        skip.convention(false)
        verbose.convention(false)
        numberOfThreads.convention(4)
        failOnMissingSourceDirectory.convention(true)
    }
}

fun DirectoryProperty.toFile(): File = this.asFile.get()