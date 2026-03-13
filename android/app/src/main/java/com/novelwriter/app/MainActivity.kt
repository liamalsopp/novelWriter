package com.novelwriter.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novelwriter.app.ui.screens.EditorScreen
import com.novelwriter.app.ui.screens.ExplorerScreen
import com.novelwriter.app.ui.screens.GitScreen
import com.novelwriter.app.ui.screens.ProjectsScreen
import com.novelwriter.app.ui.theme.NovelWriterTheme
import com.novelwriter.app.viewmodel.EditorViewModel
import com.novelwriter.app.viewmodel.ExplorerViewModel
import com.novelwriter.app.viewmodel.GitViewModel
import com.novelwriter.app.viewmodel.ProjectsViewModel
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashHandler()
        val previousCrash = readAndClearCrash()
        enableEdgeToEdge()
        setContent {
            NovelWriterTheme {
                if (previousCrash != null) {
                    CrashDialog(previousCrash)
                } else {
                    NovelWriterApp()
                }
            }
        }
    }

    private val crashFile get() = File(filesDir, "last_crash.txt")

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                crashFile.writeText(sw.toString())
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun readAndClearCrash(): String? {
        if (!crashFile.exists()) return null
        val text = crashFile.readText()
        crashFile.delete()
        return text.ifBlank { null }
    }
}

@Composable
private fun CrashDialog(crashInfo: String) {
    var dismissed by remember { mutableStateOf(false) }
    if (!dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("App Crashed — Tap to copy error") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("The app crashed on the last launch. Error:")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        crashInfo.take(3000),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(onClick = { dismissed = true }) { Text("Retry") }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) { Text("Dismiss") }
            }
        )
    }
}

// Route helpers — use Uri encoding to handle arbitrary file paths safely
private fun encodePath(path: String): String = Uri.encode(path)
private fun decodePath(encoded: String): String = Uri.decode(encoded)

@Composable
fun NovelWriterApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "projects") {

        // ── Projects list ─────────────────────────────────────────────────
        composable("projects") {
            val vm: ProjectsViewModel = viewModel()
            ProjectsScreen(
                viewModel = vm,
                onOpenProject = { project ->
                    navController.navigate(
                        "explorer/${encodePath(project.path)}/${Uri.encode(project.name)}/${Uri.encode(project.remoteUrl ?: "")}"
                    )
                }
            )
        }

        // ── Project explorer (tree) ───────────────────────────────────────
        composable(
            route = "explorer/{projectPath}/{projectName}/{remoteUrl}",
            arguments = listOf(
                navArgument("projectPath") { type = NavType.StringType },
                navArgument("projectName") { type = NavType.StringType },
                navArgument("remoteUrl") { type = NavType.StringType }
            )
        ) { back ->
            val projectPath = decodePath(back.arguments?.getString("projectPath") ?: "")
            val projectName = Uri.decode(back.arguments?.getString("projectName") ?: "")
            val remoteUrl = Uri.decode(back.arguments?.getString("remoteUrl") ?: "").ifBlank { null }

            val vm: ExplorerViewModel = viewModel()
            ExplorerScreen(
                viewModel = vm,
                projectPath = projectPath,
                projectName = projectName,
                onOpenDocument = { handle, docName ->
                    navController.navigate(
                        "editor/${encodePath(projectPath)}/$handle/${Uri.encode(docName)}"
                    )
                },
                onOpenGit = {
                    navController.navigate(
                        "git/${encodePath(projectPath)}/${Uri.encode(projectName)}/${Uri.encode(remoteUrl ?: "")}"
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Document editor ───────────────────────────────────────────────
        composable(
            route = "editor/{projectPath}/{handle}/{docName}",
            arguments = listOf(
                navArgument("projectPath") { type = NavType.StringType },
                navArgument("handle") { type = NavType.StringType },
                navArgument("docName") { type = NavType.StringType }
            )
        ) { back ->
            val projectPath = decodePath(back.arguments?.getString("projectPath") ?: "")
            val handle = back.arguments?.getString("handle") ?: ""

            val vm: EditorViewModel = viewModel()
            EditorScreen(
                viewModel = vm,
                projectPath = projectPath,
                handle = handle,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Git sync screen ───────────────────────────────────────────────
        composable(
            route = "git/{projectPath}/{projectName}/{remoteUrl}",
            arguments = listOf(
                navArgument("projectPath") { type = NavType.StringType },
                navArgument("projectName") { type = NavType.StringType },
                navArgument("remoteUrl") { type = NavType.StringType }
            )
        ) { back ->
            val projectPath = decodePath(back.arguments?.getString("projectPath") ?: "")
            val projectName = Uri.decode(back.arguments?.getString("projectName") ?: "")
            val remoteUrl = Uri.decode(back.arguments?.getString("remoteUrl") ?: "").ifBlank { null }

            val vm: GitViewModel = viewModel()
            GitScreen(
                viewModel = vm,
                projectPath = projectPath,
                projectName = projectName,
                remoteUrl = remoteUrl,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
