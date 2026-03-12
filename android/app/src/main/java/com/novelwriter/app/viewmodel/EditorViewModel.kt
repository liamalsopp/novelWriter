package com.novelwriter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelwriter.app.data.model.NWDocument
import com.novelwriter.app.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val document: NWDocument? = null,
    val content: String = "",
    val isModified: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val projectRepo = ProjectRepository(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var currentProjectPath = ""

    fun loadDocument(projectPath: String, handle: String) {
        currentProjectPath = projectPath
        _uiState.value = EditorUiState(isLoading = true)
        viewModelScope.launch {
            val doc = withContext(Dispatchers.IO) {
                projectRepo.readDocument(projectPath, handle)
            }
            if (doc != null) {
                _uiState.value = EditorUiState(document = doc, content = doc.content)
            } else {
                _uiState.value = EditorUiState(message = "Failed to load document")
            }
        }
    }

    fun updateContent(text: String) {
        _uiState.value = _uiState.value.copy(
            content = text,
            isModified = text != (_uiState.value.document?.content ?: "")
        )
    }

    fun saveDocument() {
        val doc = _uiState.value.document ?: return
        val updatedDoc = doc.copy(content = _uiState.value.content)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            withContext(Dispatchers.IO) {
                projectRepo.saveDocument(currentProjectPath, updatedDoc)
            }
            _uiState.value = _uiState.value.copy(
                document = updatedDoc,
                isModified = false,
                isSaving = false,
                message = "Saved"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
