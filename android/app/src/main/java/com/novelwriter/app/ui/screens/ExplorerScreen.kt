package com.novelwriter.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novelwriter.app.data.model.NWItem
import com.novelwriter.app.data.model.NWItemClass
import com.novelwriter.app.data.model.NWItemLayout
import com.novelwriter.app.data.model.NWItemType
import com.novelwriter.app.viewmodel.ExplorerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel,
    projectPath: String,
    projectName: String,
    isCloudProject: Boolean = false,
    onOpenDocument: (handle: String, name: String) -> Unit,
    onOpenSync: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectPath) {
        viewModel.loadProject(projectPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(projectName)
                        uiState.project?.let {
                            Text(
                                it.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reload(projectPath) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                    IconButton(onClick = onOpenSync) {
                        Icon(
                            if (isCloudProject) Icons.Default.Cloud else Icons.Default.Sync,
                            contentDescription = if (isCloudProject) "Cloud sync" else "Git sync"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                uiState.project != null -> {
                    val roots = viewModel.getRootItems()
                    if (roots.isEmpty()) {
                        Text(
                            "Empty project",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                            items(roots, key = { it.handle }) { root ->
                                RootSection(root, viewModel, onOpenDocument)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RootSection(
    root: NWItem,
    viewModel: ExplorerViewModel,
    onOpenDocument: (handle: String, name: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = root.handle in uiState.expandedHandles
    val children = viewModel.getChildren(root.handle)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.toggleExpanded(root.handle) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = classIcon(root.itemClass),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                root.name.uppercase(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isExpanded) {
            children.forEach { child ->
                TreeItem(
                    item = child,
                    depth = 1,
                    viewModel = viewModel,
                    onOpenDocument = onOpenDocument
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun TreeItem(
    item: NWItem,
    depth: Int,
    viewModel: ExplorerViewModel,
    onOpenDocument: (handle: String, name: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = item.handle in uiState.expandedHandles
    val children = viewModel.getChildren(item.handle)
    val hasChildren = children.isNotEmpty()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    when {
                        item.type == NWItemType.FILE && item.layout != null ->
                            onOpenDocument(item.handle, item.name)
                        hasChildren || item.type == NWItemType.FOLDER ->
                            viewModel.toggleExpanded(item.handle)
                    }
                }
                .padding(
                    start = (12 + depth * 20).dp,
                    end = 16.dp,
                    top = 7.dp,
                    bottom = 7.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/collapse indicator
            if (hasChildren || item.type == NWItemType.FOLDER) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(20.dp))
            }

            Icon(
                imageVector = itemIcon(item),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    !item.active -> MaterialTheme.colorScheme.outline
                    item.type == NWItemType.FOLDER -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(Modifier.width(8.dp))

            Text(
                item.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.active) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outline
            )

            if (item.type == NWItemType.FILE && item.wordCount > 0) {
                Text(
                    "${item.wordCount}w",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (isExpanded && hasChildren) {
            children.forEach { child ->
                TreeItem(
                    item = child,
                    depth = depth + 1,
                    viewModel = viewModel,
                    onOpenDocument = onOpenDocument
                )
            }
        }
    }
}

private fun classIcon(itemClass: NWItemClass) = when (itemClass) {
    NWItemClass.NOVEL -> Icons.Default.MenuBook
    NWItemClass.CHARACTER -> Icons.Default.Person
    NWItemClass.PLOT -> Icons.Default.Timeline
    NWItemClass.WORLD -> Icons.Default.Public
    NWItemClass.TIMELINE -> Icons.Default.Schedule
    NWItemClass.OBJECT -> Icons.Default.Category
    NWItemClass.ENTITY -> Icons.Default.AccountTree
    NWItemClass.ARCHIVE -> Icons.Default.Archive
    NWItemClass.TRASH -> Icons.Default.Delete
    else -> Icons.Default.Folder
}

private fun itemIcon(item: NWItem) = when {
    item.type == NWItemType.FOLDER -> Icons.Default.Folder
    item.layout == NWItemLayout.NOTE -> Icons.Default.StickyNote2
    else -> Icons.Default.Description
}
