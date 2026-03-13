package com.novelwriter.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelwriter.app.data.repository.CloudSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CloudSyncUiState(
    val isSyncing: Boolean = false,
    val message: String? = null
)

class CloudSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val cloudRepo = CloudSyncRepository(application)

    private val _uiState = MutableStateFlow(CloudSyncUiState())
    val uiState: StateFlow<CloudSyncUiState> = _uiState.asStateFlow()

    fun pullFromCloud(projectPath: String, cloudUri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val success = cloudRepo.copyFromCloud(Uri.parse(cloudUri), File(projectPath))
            _uiState.value = CloudSyncUiState(
                message = if (success) "Pulled latest from cloud" else "Pull failed — check the folder is still accessible"
            )
        }
    }

    fun pushToCloud(projectPath: String, cloudUri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val success = cloudRepo.copyToCloud(File(projectPath), Uri.parse(cloudUri))
            _uiState.value = CloudSyncUiState(
                message = if (success) "Pushed changes to cloud" else "Push failed — check the folder is still accessible"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
