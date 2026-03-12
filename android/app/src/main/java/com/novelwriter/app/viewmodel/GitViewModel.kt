package com.novelwriter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelwriter.app.data.repository.GitRepository
import com.novelwriter.app.data.repository.GitResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GitUiState(
    val changedFiles: List<String> = emptyList(),
    val remoteUrl: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

class GitViewModel(application: Application) : AndroidViewModel(application) {

    private val gitRepo = GitRepository(application)

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    private var projectPath = ""

    fun init(path: String, knownRemoteUrl: String?) {
        projectPath = path
        viewModelScope.launch {
            // Discover remote URL from git config if not supplied
            val remote = knownRemoteUrl ?: gitRepo.getRemoteUrl(path)
            _uiState.value = _uiState.value.copy(remoteUrl = remote)
            refreshStatus()
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = gitRepo.getStatus(projectPath)) {
                is GitResult.Success -> _uiState.value = _uiState.value.copy(
                    changedFiles = result.data,
                    isLoading = false
                )
                is GitResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = result.message
                )
            }
        }
    }

    fun pull() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = gitRepo.pull(projectPath, _uiState.value.remoteUrl)) {
                is GitResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = result.data)
                    refreshStatus()
                }
                is GitResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Pull failed: ${result.message}"
                )
            }
        }
    }

    fun commit(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = gitRepo.commit(projectPath, message)) {
                is GitResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = result.data,
                        changedFiles = emptyList()
                    )
                }
                is GitResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Commit failed: ${result.message}"
                )
            }
        }
    }

    fun push() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = gitRepo.push(projectPath, _uiState.value.remoteUrl)) {
                is GitResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = result.data
                )
                is GitResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Push failed: ${result.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
