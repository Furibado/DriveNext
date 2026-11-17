package ru.mtuci.drivenext.data

import android.content.Context
import android.util.AtomicFile
import java.io.File

internal class AuthSessionStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "auth_refresh_token"))

    fun read(): String? = try {
        file.readFully().toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
    } catch (_: java.io.FileNotFoundException) {
        null
    }

    fun save(token: String) {
        require(token.isNotBlank())
        val output = file.startWrite()
        try {
            output.write(token.toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    fun clear() = file.delete()
}
