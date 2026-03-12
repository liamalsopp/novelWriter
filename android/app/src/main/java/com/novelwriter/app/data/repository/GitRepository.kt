package com.novelwriter.app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.novelwriter.app.data.model.LocalProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

sealed class GitResult<out T> {
    data class Success<T>(val data: T) : GitResult<T>()
    data class Error(val message: String) : GitResult<Nothing>()
}

data class GitCredentials(val username: String, val token: String)

class GitRepository(context: Context) {

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "git_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(remoteUrl: String, credentials: GitCredentials) {
        val key = credKey(remoteUrl)
        encryptedPrefs.edit()
            .putString("${key}_user", credentials.username)
            .putString("${key}_token", credentials.token)
            .apply()
    }

    fun getCredentials(remoteUrl: String): GitCredentials? {
        val key = credKey(remoteUrl)
        val username = encryptedPrefs.getString("${key}_user", null) ?: return null
        val token = encryptedPrefs.getString("${key}_token", null) ?: return null
        return GitCredentials(username, token)
    }

    private fun credKey(remoteUrl: String) = "creds_${remoteUrl.hashCode()}"

    private fun credentialsProvider(remoteUrl: String?): UsernamePasswordCredentialsProvider? {
        val url = remoteUrl ?: return null
        val creds = getCredentials(url) ?: return null
        return UsernamePasswordCredentialsProvider(creds.username, creds.token)
    }

    suspend fun clone(
        url: String,
        targetDir: File,
        credentials: GitCredentials?
    ): GitResult<LocalProject> = withContext(Dispatchers.IO) {
        try {
            val cmd = Git.cloneRepository()
                .setURI(url)
                .setDirectory(targetDir)
            if (credentials != null) {
                cmd.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(credentials.username, credentials.token)
                )
            }
            cmd.call().use { _ ->
                GitResult.Success(
                    LocalProject(
                        name = targetDir.name,
                        path = targetDir.absolutePath,
                        remoteUrl = url
                    )
                )
            }
        } catch (e: Exception) {
            // Clean up partial clone
            targetDir.deleteRecursively()
            GitResult.Error(e.message ?: "Clone failed")
        }
    }

    suspend fun pull(projectPath: String, remoteUrl: String?): GitResult<String> =
        withContext(Dispatchers.IO) {
            try {
                Git.open(File(projectPath)).use { git ->
                    val effectiveUrl = remoteUrl ?: getRemoteUrl(projectPath)
                    val result = git.pull()
                        .apply { credentialsProvider(effectiveUrl)?.let { setCredentialsProvider(it) } }
                        .call()
                    if (result.isSuccessful) {
                        val status = result.mergeResult?.mergeStatus?.toString() ?: "Already up to date"
                        GitResult.Success("Pull successful: $status")
                    } else {
                        GitResult.Error("Pull failed: merge conflict or error")
                    }
                }
            } catch (e: Exception) {
                GitResult.Error(e.message ?: "Pull failed")
            }
        }

    suspend fun commit(
        projectPath: String,
        message: String,
        authorName: String = "NovelWriter Android",
        authorEmail: String = "app@novelwriter"
    ): GitResult<String> = withContext(Dispatchers.IO) {
        try {
            Git.open(File(projectPath)).use { git ->
                git.add().addFilepattern(".").call()
                val commit = git.commit()
                    .setMessage(message)
                    .setAuthor(authorName, authorEmail)
                    .call()
                GitResult.Success("Committed: ${commit.abbreviate(7).name()}")
            }
        } catch (e: Exception) {
            GitResult.Error(e.message ?: "Commit failed")
        }
    }

    suspend fun push(projectPath: String, remoteUrl: String?): GitResult<String> =
        withContext(Dispatchers.IO) {
            try {
                Git.open(File(projectPath)).use { git ->
                    val effectiveUrl = remoteUrl ?: getRemoteUrl(projectPath)
                    git.push()
                        .apply { credentialsProvider(effectiveUrl)?.let { setCredentialsProvider(it) } }
                        .call()
                    GitResult.Success("Push successful")
                }
            } catch (e: Exception) {
                GitResult.Error(e.message ?: "Push failed")
            }
        }

    suspend fun getStatus(projectPath: String): GitResult<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                Git.open(File(projectPath)).use { git ->
                    val status = git.status().call()
                    val changes = buildList {
                        status.modified.forEach { add("M  $it") }
                        status.added.forEach { add("A  $it") }
                        status.changed.forEach { add("C  $it") }
                        status.removed.forEach { add("D  $it") }
                        status.untracked.forEach { add("?  $it") }
                        status.missing.forEach { add("!  $it") }
                    }
                    GitResult.Success(changes)
                }
            } catch (e: Exception) {
                GitResult.Error(e.message ?: "Status check failed")
            }
        }

    suspend fun getRemoteUrl(projectPath: String): String? = withContext(Dispatchers.IO) {
        try {
            Git.open(File(projectPath)).use { git ->
                git.remoteList().call()
                    .firstOrNull { it.name == "origin" }
                    ?.urIs?.firstOrNull()?.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun hasGitRepo(path: String): Boolean = File(path, ".git").exists()
}
