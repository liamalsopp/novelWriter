package com.novelwriter.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novelwriter.app.viewmodel.GitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitScreen(
    viewModel: GitViewModel,
    projectPath: String,
    projectName: String,
    remoteUrl: String?,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var commitMessage by remember { mutableStateOf("") }

    LaunchedEffect(projectPath) {
        viewModel.init(projectPath, remoteUrl)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync — $projectName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Remote URL info
            if (!uiState.remoteUrl.isNullOrBlank()) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudQueue, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.remoteUrl!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pull / Push row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.pull() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && !uiState.remoteUrl.isNullOrBlank()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Pull")
                }
                Button(
                    onClick = { viewModel.push() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && !uiState.remoteUrl.isNullOrBlank()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Push")
                }
            }

            HorizontalDivider()

            // Changed files section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Changed Files",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    IconButton(onClick = { viewModel.refreshStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (uiState.changedFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircleOutline, null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(4.dp))
                            Text("No changes", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp)) {
                        items(uiState.changedFiles) { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Commit section
            Text("Commit Changes", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = commitMessage,
                onValueChange = { commitMessage = it },
                label = { Text("Commit message") },
                placeholder = { Text("e.g., Add chapter 3, revise ending") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Button(
                onClick = {
                    viewModel.commit(commitMessage)
                    commitMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = commitMessage.isNotBlank()
                        && !uiState.isLoading
                        && uiState.changedFiles.isNotEmpty()
            ) {
                Icon(Icons.Default.Commit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Commit")
            }
        }
    }
}
