package com.novelwriter.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.novelwriter.app.data.model.LocalProject
import com.novelwriter.app.data.model.NWDocument
import com.novelwriter.app.data.model.NWProject
import com.novelwriter.app.data.parser.NWDocumentParser
import com.novelwriter.app.data.parser.NWProjectParser
import java.io.File

class ProjectRepository(context: Context) {

    val projectsDir = File(context.filesDir, "projects")
    private val prefs = context.getSharedPreferences("novelwriter_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val projectParser = NWProjectParser()
    private val documentParser = NWDocumentParser()

    init {
        projectsDir.mkdirs()
    }

    fun getProjects(): List<LocalProject> {
        val json = prefs.getString("projects", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalProject>>() {}.type
            gson.fromJson<List<LocalProject>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addProject(project: LocalProject) {
        val projects = getProjects().toMutableList()
        if (projects.none { it.path == project.path }) {
            projects.add(project)
            saveProjects(projects)
        }
    }

    fun removeProject(path: String) {
        saveProjects(getProjects().filter { it.path != path })
    }

    fun updateRemoteUrl(path: String, remoteUrl: String) {
        val projects = getProjects().map {
            if (it.path == path) it.copy(remoteUrl = remoteUrl) else it
        }
        saveProjects(projects)
    }

    private fun saveProjects(projects: List<LocalProject>) {
        prefs.edit().putString("projects", gson.toJson(projects)).apply()
    }

    fun parseProject(projectPath: String): NWProject? {
        return projectParser.parse(File(projectPath, "nwProject.nwx"))
    }

    fun readDocument(projectPath: String, handle: String): NWDocument? {
        return documentParser.read(File(projectPath, "content"), handle)
    }

    fun saveDocument(projectPath: String, document: NWDocument) {
        documentParser.write(File(projectPath, "content"), document)
    }

    fun isNovelWriterProject(path: String): Boolean {
        return File(path, "nwProject.nwx").exists()
    }
}
