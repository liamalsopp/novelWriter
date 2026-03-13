package com.novelwriter.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CloudSyncRepository(private val context: Context) {

    fun takePersistablePermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try { context.contentResolver.takePersistableUriPermission(uri, flags) } catch (_: Exception) {}
    }

    fun getFolderName(uri: Uri): String =
        DocumentFile.fromTreeUri(context, uri)?.name ?: "Cloud Project"

    /** Copy nwProject.nwx + content/*.nwd from a cloud folder into [destDir]. */
    suspend fun copyFromCloud(uri: Uri, destDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val tree = DocumentFile.fromTreeUri(context, uri) ?: return@withContext false
            destDir.mkdirs()

            tree.findFile("nwProject.nwx")?.let { f ->
                readDocFile(f, File(destDir, "nwProject.nwx"))
            }

            tree.findFile("content")?.takeIf { it.isDirectory }?.let { contentDoc ->
                val localContent = File(destDir, "content").also { it.mkdirs() }
                contentDoc.listFiles().forEach { f ->
                    if (f.name?.endsWith(".nwd") == true)
                        readDocFile(f, File(localContent, f.name!!))
                }
            }

            File(destDir, "nwProject.nwx").exists()
        } catch (_: Exception) { false }
    }

    /** Copy nwProject.nwx + content/*.nwd from [localDir] back to the cloud folder. */
    suspend fun copyToCloud(localDir: File, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val tree = DocumentFile.fromTreeUri(context, uri) ?: return@withContext false

            File(localDir, "nwProject.nwx").takeIf { it.exists() }?.let { src ->
                val dest = tree.findFile("nwProject.nwx")
                    ?: tree.createFile("application/xml", "nwProject.nwx")
                dest?.let { writeDocFile(src, it) }
            }

            File(localDir, "content").takeIf { it.exists() }?.let { localContent ->
                val contentDoc = tree.findFile("content")
                    ?: tree.createDirectory("content")
                contentDoc?.let { dir ->
                    localContent.listFiles()?.forEach { src ->
                        if (src.name.endsWith(".nwd")) {
                            val dest = dir.findFile(src.name)
                                ?: dir.createFile("text/plain", src.name)
                            dest?.let { writeDocFile(src, it) }
                        }
                    }
                }
            }

            true
        } catch (_: Exception) { false }
    }

    private fun readDocFile(src: DocumentFile, dest: File) {
        context.contentResolver.openInputStream(src.uri)?.use { it.copyTo(dest.outputStream()) }
    }

    private fun writeDocFile(src: File, dest: DocumentFile) {
        context.contentResolver.openOutputStream(dest.uri, "wt")?.use { src.inputStream().copyTo(it) }
    }
}
