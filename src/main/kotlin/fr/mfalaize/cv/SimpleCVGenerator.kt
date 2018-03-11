package fr.mfalaize.cv

import com.esotericsoftware.yamlbeans.YamlReader
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.exception.ResourceNotFoundException
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.log.NullLogChute
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import org.xhtmlrenderer.pdf.ITextRenderer
import org.xhtmlrenderer.resource.XMLResource
import java.io.*
import java.util.*


fun main(args: Array<String>) {
    val theme = getTheme(args)
    val themePath = getThemePath(theme)
    val themeURL = getThemeURL(themePath)
    val cvFile = getCvFile(args)
    val pdfFile = getPdfFile(args, cvFile)
    val ymlParsed = parseYaml(cvFile)
    val html = mergeTemplate(themePath, ymlParsed)
    renderPdf(html, pdfFile, themeURL)
}

fun getTheme(args: Array<String>): String {
    val it = args.iterator()
    while (it.hasNext()) {
        val arg = it.next()
        if (arg == "--theme" || arg == "-t") {
            if (it.hasNext()) {
                return it.next()
            } else {
                throw IllegalArgumentException("-t or --theme cannot be empty")
            }
        }
    }
    return "default"
}

fun getThemePath(theme: String): String {
    return "themes/$theme/index.html"
}

fun getThemeURL(themePath: String): String {
    // Search in classpath first
    val classpathFile = ClassLoader.getSystemResource(themePath)
    if (classpathFile == null) {
        // If not found in classpath, search in working directory
        val file = File(themePath)
        if (!file.exists()) {
            throw FileNotFoundException("Theme \"$themePath\" was not found neither in the classpath or in ${file.absolutePath}")
        }
        return file.toURI().toURL().toString()
    }
    return classpathFile.toString()
}

fun getCvFile(args: Array<String>): String {
    return args.find { it.endsWith(".yml") } ?: "cv.yml"
}

fun getPdfFile(args: Array<String>, cvFilename: String): String {
    return args.find { it.endsWith(".pdf") } ?: cvFilename.replace(".yml", ".pdf")
}

fun parseYaml(filePath: String): Map<*, *> {
    val yml = File(filePath).readText()
    val parsed = YamlReader(yml).read()
    if (parsed is Map<*, *>) {
        return parsed
    } else {
        throw IllegalArgumentException("Cannot parse file $filePath. Maybe it is not in yaml format ?")
    }
}

fun mergeTemplate(themePath: String, parsedYaml: Map<*, *>): String {
    // First we try to get template from classpath
    val templateClasspathEngine = VelocityEngine()
    val propsClasspath = Properties()
    propsClasspath.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
    propsClasspath.setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.canonicalName)
    // Deactivate velocity logs to make sure no logs will output if classpath resource is not found
    propsClasspath.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute::class.java.canonicalName)
    templateClasspathEngine.init(propsClasspath)

    val template = try {
        templateClasspathEngine.getTemplate(themePath, Charsets.UTF_8.name())
    } catch (ex: ResourceNotFoundException) {
        // If not found, we try to get template from filepath
        val templateFileEngine = VelocityEngine()
        templateFileEngine.init()
        templateFileEngine.getTemplate(themePath, Charsets.UTF_8.name())
    }

    val context = VelocityContext(parsedYaml)
    // Adding the working directory URL to make possible to add additional images
    context.put("workingDirectory", File(".").toURI().toURL().toString())
    val writer = StringWriter()
    template.merge(context, writer)
    return writer.toString()
}

fun renderPdf(html: String, pdfFile: String, themeURL: String) {
    var os: OutputStream? = null
    try {
        val renderer = ITextRenderer()
        val doc = XMLResource.load(StringReader(html)).document
        renderer.setDocument(doc, themeURL)
        renderer.layout()
        os = FileOutputStream(pdfFile)
        renderer.createPDF(os)
    } finally {
        os?.close()
    }
}