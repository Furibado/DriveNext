package ru.mtuci.drivenext.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.mtuci.drivenext.domain.ProfileRepository
import ru.mtuci.drivenext.domain.UserProfile
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

internal class ProfileRepositoryImpl(context: Context, private val client: SupabaseClient) : ProfileRepository {
    private val resolver = context.contentResolver

    override suspend fun getProfile(): UserProfile = withContext(Dispatchers.IO) {
        val user = JSONObject(client.authorizedGet("/auth/v1/user"))
        val id = UUID.fromString(user.getString("id")).toString()
        val rows = JSONArray(client.authorizedGet("/rest/v1/registration_profiles?user_id=eq.$id&select=given_name,surname,gender,avatar_path"))
        val row = rows.optJSONObject(0)
        val metadata = user.optJSONObject("user_metadata") ?: JSONObject()
        val identities = user.optJSONArray("identities") ?: JSONArray()
        val google = (0 until identities.length()).map { identities.getJSONObject(it) }
            .firstOrNull { it.optString("provider") == "google" }
        val name = listOf(row?.optString("given_name").orEmpty(), row?.optString("surname").orEmpty())
            .filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { metadata.optString("full_name").ifBlank { metadata.optString("name") } }
            .ifBlank { user.optString("email").substringBefore('@') }
        UserProfile(id, name, user.optString("email"), user.optString("created_at"),
            row?.optString("gender").orEmpty(), google?.optJSONObject("identity_data")?.optString("email").orEmpty(),
            row?.optString("avatar_path")?.takeUnless { it == "null" }.orEmpty().ifBlank { metadata.optString("avatar_path") },
            metadata.optString("avatar_url").ifBlank { metadata.optString("picture") }, row != null)
    }

    override suspend fun getAvatar(profile: UserProfile): ByteArray? = withContext(Dispatchers.IO) {
        if (profile.avatarPath.isNotBlank()) {
            // Только собственная папка пользователя; метаданные не задают произвольный URL с токеном.
            require(profile.avatarPath.startsWith("${profile.id}/avatar."))
            val path = profile.avatarPath.split('/').joinToString("/") { Uri.encode(it) }
            client.withAccessToken {
                client.download("/storage/v1/object/authenticated/registration-photos/$path?t=${System.currentTimeMillis()}", it)
            }
        } else if (profile.googleAvatarUrl.startsWith("https://")) {
            val connection = URL(profile.googleAvatarUrl).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                if (connection.responseCode !in 200..299) throw IOException("Unable to load Google avatar")
                // Внешнему серверу фотографий токен Supabase не передаётся.
                connection.inputStream.use { it.readBytes() }
            } finally { connection.disconnect() }
        } else null
    }

    override suspend fun updateAvatar(profile: UserProfile, uri: String) = withContext(Dispatchers.IO) {
        val source = uri.toUri()
        val mime = resolver.getType(source)?.takeIf { it.startsWith("image/") }
            ?: throw IOException("Not an image")
        val path = "${UUID.fromString(profile.id)}/avatar.image"
        client.withAccessToken { token ->
            client.request("/storage/v1/object/registration-photos/$path", "POST", token, mime,
                mapOf("x-upsert" to "true", "Cache-Control" to "no-cache")) { connection ->
                val input = resolver.openInputStream(source) ?: throw IOException("Unable to open photo")
                input.use { stream ->
                    connection.setChunkedStreamingMode(64 * 1024)
                    connection.outputStream.use { stream.copyTo(it) }
                }
            }
            if (profile.hasRegistration) {
                client.json("/rest/v1/registration_profiles?user_id=eq.${profile.id}", "PATCH",
                    JSONObject().put("avatar_path", path), token)
            } else {
                // Google-вход не создаёт запись с водительскими документами.
                client.json("/auth/v1/user", "PUT",
                    JSONObject().put("data", JSONObject().put("avatar_path", path)), token)
            }
        }
        Unit
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) { client.signOut() }
}
