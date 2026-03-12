package com.novelwriter.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovelWriterTheme {
                NovelWriterApp()
            }
        }
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
