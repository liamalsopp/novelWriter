package com.novelwriter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.novelwriter.app.data.model.NWItem
import com.novelwriter.app.data.model.NWItemType
import com.novelwriter.app.data.model.NWProject
import com.novelwriter.app.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExplorerUiState(
    val project: NWProject? = null,
    val expandedHandles: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val projectRepo = ProjectRepository(application)

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    fun loadProject(projectPath: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val project = projectRepo.parseProject(projectPath)
        if (project != null) {
            // Auto-expand root items
            val expanded = project.items
                .filter { it.type == NWItemType.ROOT }
                .map { it.handle }
                .toSet()
            _uiState.value = ExplorerUiState(
                project = project,
                expandedHandles = expanded,
                isLoading = false
            )
        } else {
            _uiState.value = ExplorerUiState(isLoading = false, error = "Could not load project")
        }
    }

    fun reload(projectPath: String) {
        val expanded = _uiState.value.expandedHandles
        loadProject(projectPath)
        // Re-apply previous expansion state after reload
        _uiState.value = _uiState.value.copy(
            expandedHandles = _uiState.value.expandedHandles + expanded
        )
    }

    fun toggleExpanded(handle: String) {
        val current = _uiState.value.expandedHandles
        _uiState.value = _uiState.value.copy(
            expandedHandles = if (handle in current) current - handle else current + handle
        )
    }

    fun getRootItems(): List<NWItem> =
        _uiState.value.project?.items
            ?.filter { it.type == NWItemType.ROOT }
            ?.sortedBy { it.order }
            ?: emptyList()

    fun getChildren(parentHandle: String): List<NWItem> =
        _uiState.value.project?.items
            ?.filter { it.parent == parentHandle }
            ?.sortedBy { it.order }
            ?: emptyList()
}
