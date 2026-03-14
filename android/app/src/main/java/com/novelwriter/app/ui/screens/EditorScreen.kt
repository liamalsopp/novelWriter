package com.novelwriter.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novelwriter.app.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    projectPath: String,
    handle: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var initialScrollDone by remember { mutableStateOf(false) }

    LaunchedEffect(handle) {
        viewModel.loadDocument(projectPath, handle)
    }

    LaunchedEffect(uiState.document) {
        if (uiState.document != null && !initialScrollDone) {
            initialScrollDone = true
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    fun handleBack() {
        if (uiState.isModified) showUnsavedDialog = true else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.document?.name ?: "Loading…",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.isModified) {
                            Text(
                                "Unsaved changes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.saveDocument() },
                            enabled = uiState.isModified
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.document == null && !uiState.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text("Could not load document", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    TextField(
                        value = uiState.content,
                        onValueChange = { viewModel.updateContent(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                            .imePadding(),
                        scrollState = scrollState,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        placeholder = {
                            Text(
                                "Start writing…",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    )
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Save before leaving?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveDocument()
                    showUnsavedDialog = false
                    onBack()
                }) {
                    Text("Save & Leave")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showUnsavedDialog = false; onBack() }) {
                        Text("Discard")
                    }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
