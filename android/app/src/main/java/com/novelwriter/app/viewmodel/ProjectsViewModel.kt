package com.novelwriter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelwriter.app.data.repository.GitCredentials
import com.novelwriter.app.data.model.LocalProject
import com.novelwriter.app.data.repository.GitRepository
import com.novelwriter.app.data.repository.GitResult
import com.novelwriter.app.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ProjectsUiState(
    val projects: List<LocalProject> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val projectRepo = ProjectRepository(application)
    private val gitRepo = GitRepository(application)

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        _uiState.value = _uiState.value.copy(projects = projectRepo.getProjects())
    }

    fun cloneProject(url: String, username: String, token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)

            val repoName = url.substringAfterLast("/").removeSuffix(".git")
            val targetDir = File(projectRepo.projectsDir, repoName)

            val credentials = if (username.isNotBlank()) GitCredentials(username, token) else null

            when (val result = gitRepo.clone(url, targetDir, credentials)) {
                is GitResult.Success -> {
                    if (credentials != null) {
                        gitRepo.saveCredentials(url, credentials)
                    }
                    // Determine project name from nwProject.nwx if available
                    val project = if (projectRepo.isNovelWriterProject(result.data.path)) {
                        val parsed = projectRepo.parseProject(result.data.path)
                        result.data.copy(name = parsed?.name?.takeIf { it.isNotBlank() } ?: result.data.name)
                    } else {
                        result.data
                    }
                    projectRepo.addProject(project)
                    loadProjects()
                    _uiState.value = _uiState.value.copy(isLoading = false, message = "Cloned \"${project.name}\" successfully!")
                }
                is GitResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Clone failed: ${result.message}"
                    )
                }
            }
        }
    }

    fun openLocalProject(path: String) {
        val dir = File(path)
        if (!dir.exists() || !projectRepo.isNovelWriterProject(path)) {
            _uiState.value = _uiState.value.copy(message = "Not a valid novelWriter project")
            return
        }
        val parsed = projectRepo.parseProject(path)
        val remoteUrl = if (gitRepo.hasGitRepo(path)) {
            projectRepo.getProjects().firstOrNull { it.path == path }?.remoteUrl
        } else null
        val project = LocalProject(
            name = parsed?.name?.takeIf { it.isNotBlank() } ?: dir.name,
            path = path,
            remoteUrl = remoteUrl
        )
        projectRepo.addProject(project)
        loadProjects()
    }

    fun removeProject(path: String) {
        projectRepo.removeProject(path)
        loadProjects()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
