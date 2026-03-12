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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novelwriter.app.data.model.LocalProject
import com.novelwriter.app.viewmodel.ProjectsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onOpenProject: (LocalProject) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCloneDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Novels") },
                actions = {
                    IconButton(onClick = { viewModel.loadProjects() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showCloneDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Clone repository")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.projects.isEmpty() && !uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { showCloneDialog = true },
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    text = { Text("Clone Repo") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Cloning repository…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                uiState.projects.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "No novels yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Clone your novel's git repository to start writing on the go.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.projects, key = { it.path }) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onOpenProject(project) },
                                onRemove = { viewModel.removeProject(project.path) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCloneDialog) {
        CloneDialog(
            onDismiss = { showCloneDialog = false },
            onClone = { url, username, token ->
                showCloneDialog = false
                viewModel.cloneProject(url, username, token)
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: LocalProject,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!project.remoteUrl.isNullOrBlank()) {
                    Text(
                        project.remoteUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Remove Project") },
            text = { Text("Remove \"${project.name}\" from the list?\n\nFiles on your device won't be deleted.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onRemove() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CloneDialog(
    onDismiss: () -> Unit,
    onClone: (url: String, username: String, token: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clone Novel Repository") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Repository URL") },
                    placeholder = { Text("https://github.com/you/my-novel.git") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token / Password (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (tokenVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (tokenVisible) "Hide token" else "Show token"
                            )
                        }
                    }
                )
                Text(
                    "For GitHub, generate a Personal Access Token with repo scope and use it as the password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onClone(url.trim(), username.trim(), token) },
                enabled = url.isNotBlank()
            ) {
                Text("Clone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
