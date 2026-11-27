package com.example

import com.example.util.SoundingFormatters
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class AboutSafetyTest {

    @Test
    fun allFormattedStringsInStringsXml_haveValidJavaFormatSpecifiers() {
        val candidates = listOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml")
        )
        val stringsFile = candidates.firstOrNull { it.exists() }
            ?: error("Could not locate strings.xml for format validation")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        val nodes = doc.getElementsByTagName("string")
        val positionalArgRegex = Regex("%(\\d+)\\$([sd])")

        for (i in 0 until nodes.length) {
            val el = nodes.item(i)
            val name = el.attributes?.getNamedItem("name")?.nodeValue ?: "unknown"
            val value = el.textContent ?: continue
            val matches = positionalArgRegex.findAll(value).toList()
            if (matches.isEmpty()) continue

            val maxPos = matches.maxOf { it.groupValues[1].toInt() }
            val args = Array<Any>(maxPos) { idx ->
                val spec = matches.firstOrNull { it.groupValues[1].toInt() == idx + 1 }?.groupValues?.get(2)
                if (spec == "d") 42 else "sample"
            }

            // Must NOT throw UnknownFormatConversionException (e.g. unescaped '100%' alongside '%1$d')
            val formatted = String.format(Locale.US, value, *args)
            assertTrue("Formatted string '$name' should not be empty", formatted.isNotEmpty())
            assertFalse(
                "Formatted string '$name' should not contain broken format tokens",
                formatted.contains("%1$")
            )
        }
    }

    @Test
    fun dateFormatters_handleEdgeTimestampsWithoutThrowing() {
        val zeroDate = SoundingFormatters.formatLocalDate(0L)
        val zeroDateTime = SoundingFormatters.formatDisplayDateTime(0L)
        val currentDate = SoundingFormatters.formatLocalDate(1_764_200_000_000L)

        assertTrue(zeroDate.isNotBlank())
        assertTrue(zeroDateTime.isNotBlank())
        assertTrue(currentDate.isNotBlank())
    }
}
