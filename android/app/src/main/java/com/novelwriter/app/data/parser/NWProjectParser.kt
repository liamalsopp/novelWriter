package com.novelwriter.app.data.parser

import android.util.Xml
import com.novelwriter.app.data.model.NWItem
import com.novelwriter.app.data.model.NWItemClass
import com.novelwriter.app.data.model.NWItemLayout
import com.novelwriter.app.data.model.NWItemType
import com.novelwriter.app.data.model.NWProject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream

class NWProjectParser {

    fun parse(projectFile: File): NWProject? {
        if (!projectFile.exists()) return null
        return try {
            FileInputStream(projectFile).use { input ->
                val parser = Xml.newPullParser()
                parser.setInput(input, "UTF-8")
                parseDocument(parser)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDocument(parser: XmlPullParser): NWProject {
        var projectId = ""
        var projectName = ""
        var projectAuthor = ""
        val items = mutableListOf<NWItem>()
        var inProjectTag = false
        var inContentTag = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "project" -> {
                        projectId = parser.getAttributeValue(null, "id") ?: ""
                        inProjectTag = true
                    }
                    "content" -> inContentTag = true
                    "name" -> if (inProjectTag && !inContentTag) {
                        parser.next()
                        if (parser.eventType == XmlPullParser.TEXT) {
                            projectName = parser.text ?: ""
                        }
                    }
                    "author" -> if (inProjectTag) {
                        parser.next()
                        if (parser.eventType == XmlPullParser.TEXT) {
                            projectAuthor = parser.text ?: ""
                        }
                    }
                    "item" -> if (inContentTag) {
                        items.add(parseItem(parser))
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "project" -> inProjectTag = false
                    "content" -> inContentTag = false
                }
            }
            event = parser.next()
        }
        return NWProject(projectId, projectName, projectAuthor, items)
    }

    private fun parseItem(parser: XmlPullParser): NWItem {
        val handle = parser.getAttributeValue(null, "handle") ?: ""
        val parent = parser.getAttributeValue(null, "parent")?.takeIf { it != "None" }
        val root = parser.getAttributeValue(null, "root") ?: ""
        val order = parser.getAttributeValue(null, "order")?.toIntOrNull() ?: 0
        val type = enumOrDefault(parser.getAttributeValue(null, "type"), NWItemType.FILE)
        val itemClass = enumOrDefault(parser.getAttributeValue(null, "class"), NWItemClass.NOVEL)
        val layout = parser.getAttributeValue(null, "layout")?.let {
            try { NWItemLayout.valueOf(it) } catch (_: IllegalArgumentException) { null }
        }

        var itemName = ""
        var wordCount = 0
        var charCount = 0
        var active = true

        val startDepth = parser.depth
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.depth == startDepth)) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "meta" -> {
                        wordCount = parser.getAttributeValue(null, "wordCount")?.toIntOrNull() ?: 0
                        charCount = parser.getAttributeValue(null, "charCount")?.toIntOrNull() ?: 0
                    }
                    "name" -> {
                        active = parser.getAttributeValue(null, "active") != "no"
                        parser.next()
                        if (parser.eventType == XmlPullParser.TEXT) {
                            itemName = parser.text ?: ""
                        }
                    }
                }
            }
            event = parser.next()
        }

        return NWItem(handle, parent, root, order, type, itemClass, layout, itemName, wordCount, charCount, active)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T {
        if (value == null) return default
        return try { enumValueOf(value) } catch (_: IllegalArgumentException) { default }
    }
}

class NWDocumentParser {

    fun read(contentDir: File, handle: String): NWDocument? {
        val file = File(contentDir, "$handle.nwd")
        if (!file.exists()) return null
        return try {
            val lines = file.readLines(Charsets.UTF_8)
            val headerLines = lines.takeWhile { it.startsWith("%%~") }
            val meta = headerLines.associate { line ->
                val sep = line.indexOf(": ")
                if (sep > 0) line.substring(3, sep) to line.substring(sep + 2)
                else line.substring(3) to ""
            }
            // Content starts after header (skip one blank separator line if present)
            val afterHeader = lines.drop(headerLines.size)
            val contentLines = if (afterHeader.firstOrNull()?.isEmpty() == true) {
                afterHeader.drop(1)
            } else {
                afterHeader
            }
            NWDocument(
                handle = handle,
                name = meta["name"] ?: "",
                kind = meta["kind"] ?: "",
                content = contentLines.joinToString("\n")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun write(contentDir: File, document: NWDocument) {
        val file = File(contentDir, "${document.handle}.nwd")
        if (!file.exists()) return

        val existingLines = file.readLines(Charsets.UTF_8)
        val headerLines = existingLines.takeWhile { it.startsWith("%%~") }.toMutableList()

        // Update the hash to match new content
        val newHash = sha1(document.content)
        val hashIdx = headerLines.indexOfFirst { it.startsWith("%%~hash:") }
        if (hashIdx >= 0) {
            headerLines[hashIdx] = "%%~hash: $newHash"
        }

        // Update modification date
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        val dateIdx = headerLines.indexOfFirst { it.startsWith("%%~date:") }
        if (dateIdx >= 0) {
            val existing = headerLines[dateIdx]
            val created = existing.substringAfter("%%~date:").trim().substringBefore("/")
            headerLines[dateIdx] = "%%~date: $created/$now"
        }

        val sb = StringBuilder()
        headerLines.forEach { sb.appendLine(it) }
        sb.appendLine()
        sb.append(document.content)

        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    private fun sha1(text: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-1")
        return md.digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
