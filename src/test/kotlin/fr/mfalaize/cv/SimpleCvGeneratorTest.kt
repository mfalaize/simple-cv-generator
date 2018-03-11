package fr.mfalaize.cv

import org.apache.velocity.exception.ResourceNotFoundException
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SimpleCvGeneratorTest {

    @Test
    fun parseYamlShouldFailIfFileNotFound() {
        assertFailsWith<FileNotFoundException> {
            parseYaml("file_that_does_not_exists.yml")
        }
    }

    @Test
    fun parseYamlShouldFailIfFileIsNotYaml() {
        assertFailsWith<IllegalArgumentException> {
            parseYaml(ClassLoader.getSystemResource("cv.xml").file)
        }
    }

    @Test
    fun parseYamlShouldSucceedIfFileExistsAndIsYaml() {
        val map = parseYaml(ClassLoader.getSystemResource("cv.yml").file)
        assertEquals("success", (map["cv"] as Map<*, *>)["test"])
    }

    @Test
    fun getThemePathShouldReturnTheThemePath() {
        assertEquals("themes/default/index.html", getThemePath("default"))
        assertEquals("themes/anothertheme/index.html", getThemePath("anothertheme"))
    }

    @Test
    fun getThemeURLShouldFailIfTheThemeFileDoesNotExist() {
        assertFailsWith<FileNotFoundException> {
            getThemeURL(getThemePath("themeDoesNotExist"))
        }
    }

    @Test
    fun getThemeURLShouldReturnTheThemeFromWorkingDirectoryIfItDoesNotExistInClasspath() {
        val workingDir = File(File(".").absolutePath.substringBeforeLast("/.")).toURI().toURL().toString()
        assertEquals("${workingDir}themes/test/index.html", getThemeURL(getThemePath("test")))
    }

    @Test
    fun getThemeShouldReturnTheThemeChoosenInArgs() {
        assertEquals("mytheme", getTheme(arrayOf("--theme", "mytheme")))
        assertEquals("mytheme", getTheme(arrayOf("-t", "mytheme")))
        assertEquals("mytheme", getTheme(arrayOf("someargs", "andanother", "-t", "mytheme")))
        assertEquals("mytheme", getTheme(arrayOf("someargs", "andanother", "--theme", "mytheme")))
        assertEquals("mytheme", getTheme(arrayOf("-t", "mytheme", "someargs", "andanother")))
        assertEquals("mytheme", getTheme(arrayOf("--theme", "mytheme", "someargs", "andanother")))
        assertEquals("mytheme", getTheme(arrayOf("someargs", "-t", "mytheme", "andanother")))
        assertEquals("mytheme", getTheme(arrayOf("someargs", "--theme", "mytheme", "andanother")))
    }

    @Test
    fun getThemeShouldReturnDefaultIfThereIsNoChoosenThemeInArgs() {
        assertEquals("default", getTheme(arrayOf()))
        assertEquals("default", getTheme(arrayOf("someargs", "andanother")))
        assertEquals("default", getTheme(arrayOf("someargs")))
        assertEquals("default", getTheme(arrayOf("--any", "any")))
    }

    @Test
    fun getThemeShouldFailIfNoArgumentIsFoundAfterThemePreArg() {
        assertFailsWith<IllegalArgumentException> {
            getTheme(arrayOf("--theme"))
        }
        assertFailsWith<IllegalArgumentException> {
            getTheme(arrayOf("-t"))
        }
    }

    @Test
    fun getCvFileShouldReturnArgWithYamlExtension() {
        assertEquals("mycv.yml", getCvFile(arrayOf("any", "another", "mycv.pdf", "mycv.yml", "mycv.xml")))
        assertEquals("mycv.yml", getCvFile(arrayOf("mycv.yml")))
    }

    @Test
    fun getCvFileShouldReturnDefaultIfThereIsNoYamlFileInArgs() {
        assertEquals("cv.yml", getCvFile(arrayOf()))
        assertEquals("cv.yml", getCvFile(arrayOf("any", "another", "mycv.pdf", "mycv.xml")))
    }

    @Test
    fun getPdfFileShouldReturnArgWithPdfExtension() {
        assertEquals("mycv.pdf", getPdfFile(arrayOf("any", "another", "mycv.pdf", "mycv.yml", "mycv.xml"), "cv.yml"))
        assertEquals("mycv.pdf", getPdfFile(arrayOf("mycv.pdf"), "cv.yml"))
    }

    @Test
    fun getPdfFileShouldReturnCvFilenameWithPdfExtensionIfThereIsNoPdfFileInArgs() {
        assertEquals("cv.pdf", getPdfFile(arrayOf(), "cv.yml"))
        assertEquals("mycv.pdf", getPdfFile(arrayOf("any", "another", "mycv.xml"), "mycv.yml"))
    }

    @Test
    fun mergeTemplateShouldReturnContentOfTemplateMerged() {
        val workingDirURL = File(".").toURI().toURL()
        val themePath = getThemePath("anothertheme")
        val yamlParsed = parseYaml(ClassLoader.getSystemResource("cv.yml").file)
        assertEquals("""<!DOCTYPE html>
<html>
<body>
success, $workingDirURL
</body>
</html>""", mergeTemplate(themePath, yamlParsed))
    }

    @Test
    fun mergeTemplateShouldFailIfThemePathIsNotCorrect() {
        val yamlParsed = parseYaml(ClassLoader.getSystemResource("cv.yml").file)
        assertFailsWith<ResourceNotFoundException> {
            mergeTemplate("pathDoesNotExist", yamlParsed)
        }
    }

    @Test
    fun renderPDFShouldRenderAPdf() {
        val themePath = getThemePath("anothertheme")
        val yamlParsed = parseYaml(ClassLoader.getSystemResource("cv.yml").file)
        renderPdf(mergeTemplate(themePath, yamlParsed), "cv.pdf", themePath)
        val pdf = File("cv.pdf")
        assertTrue(pdf.exists())
        pdf.delete()
    }

    @Test
    fun mainShouldRenderPdfAtDefaultPlaceIfNoArg() {
        main(emptyArray())
        val pdf = File("cv.pdf")
        assertTrue(pdf.exists())
        pdf.delete()
    }

    @Test
    fun mainShouldRenderPdfAtDeclaredPlaceInArgs() {
        main(arrayOf("-t", "anothertheme", ClassLoader.getSystemResource("anothercv.yml").file))
        val pdf = File(ClassLoader.getSystemResource("anothercv.pdf").file)
        assertTrue(pdf.exists())
        pdf.delete()
    }

    @Test
    fun mainShouldFailIfAnyDeclaredPlaceInArgsIsNotFound() {
        assertFailsWith<FileNotFoundException> {
            main(arrayOf("-t", "notExists"))
        }
        assertFailsWith<FileNotFoundException> {
            main(arrayOf("notExists.yml"))
        }
    }
}