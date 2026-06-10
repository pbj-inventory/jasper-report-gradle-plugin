package ca.cleaningdepot.tools.jasperreports

import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JasperReportsContext
import net.sf.jasperreports.jdt.JRJdtCompiler

class TestCompilerConfigurationCompiler(jasperReportsContext: JasperReportsContext) :
    JRJdtCompiler(jasperReportsContext) {
    @Throws(JRException::class)
    override fun checkLanguage(language: String?) {
        // compileReport itself is finale, can not overwrite it therefore
        // but checkLanguage is called first in compileReport
        this.jasperReportsContext.setProperty("testcompiler.called", "true")
        super.checkLanguage(language)
    }
}
