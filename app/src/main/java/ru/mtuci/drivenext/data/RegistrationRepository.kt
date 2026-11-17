package ru.mtuci.drivenext.data

import android.content.Context
import android.net.Uri
import ru.mtuci.drivenext.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

internal class RegistrationRepository(context: Context, private val client: SupabaseClient) : AuthRepository {
    private val resolver = context.contentResolver
    private val sessions = AuthSessionStore(context)

    override suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        val credentials = JSONObject().put("email", email).put("password", password)
        val session = client.json("/auth/v1/token?grant_type=password", "POST", credentials)
        requireCompletedProfile(session)
        saveSession(session)
    }

    override suspend fun exchangeGoogleCode(code: String, verifier: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("auth_code", code).put("code_verifier", verifier)
        saveSession(client.json("/auth/v1/token?grant_type=pkce", "POST", body))
    }

    override suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val token = sessions.read() ?: return@withContext false
        try {
            val credentials = JSONObject().put("refresh_token", token)
            val session = client.json("/auth/v1/token?grant_type=refresh_token", "POST", credentials)
            saveSession(session)
            requireCompletedProfile(session)
            true
        } catch (error: ServerError) {
            if (error.code in setOf("refresh_token_not_found", "refresh_token_already_used", "session_not_found", "session_expired")) {
                sessions.clear()
            }
            false
        } catch (error: NoConnectionException) {
            throw error
        } catch (_: IOException) {
            false
        }
    }

    private fun requireCompletedProfile(session: JSONObject) {
        val user = session.getJSONObject("user")
        val providers = user.optJSONObject("app_metadata")?.optJSONArray("providers")
        val googleAccount = providers?.let { values ->
            (0 until values.length()).any { values.optString(it) == "google" }
        } == true
        if (googleAccount) return
        val userId = UUID.fromString(user.getString("id"))
        val response = client.request("/rest/v1/registration_profiles?user_id=eq.$userId&select=user_id",
            "GET", session.getString("access_token"))
        if (JSONArray(response).length() == 0) throw IncompleteRegistrationException()
    }

    private fun saveSession(session: JSONObject) {
        client.saveSession(session)
    }
    override suspend fun register(email: String, password: String, profile: RegistrationData, photos: Map<String, String>) =
        withContext(Dispatchers.IO) {
            val document = JSONObject()
                .put("surname", profile.surname)
                .put("given_name", profile.givenName)
                .put("patronymic", profile.patronymic ?: JSONObject.NULL)
                .put("birth_date", profile.birthDate)
                .put("gender", profile.gender)
                .put("license_number", profile.licenseNumber)
                .put("license_issue_date", profile.licenseIssueDate)
            val credentials = JSONObject().put("email", email).put("password", password)
            val existingSession = try {
                client.json("/auth/v1/token?grant_type=password", "POST", credentials)
            } catch (error: ServerError) {
                if (error.code == "email_not_confirmed") throw ConfirmationRequired()
                if (error.code != "invalid_credentials") throw error
                null
            }
            val session = existingSession ?: client.json("/auth/v1/signup", "POST", credentials)
            val token = session.optString("access_token")
            if (token.isBlank()) throw ConfirmationRequired()

            val userId = UUID.fromString(session.getJSONObject("user").getString("id")).toString()
            val profileRoute = "/rest/v1/registration_profiles"
            val rows = client.request("$profileRoute?user_id=eq.$userId&select=user_id", "GET", token)
            if (JSONArray(rows).length() != 0) {
                saveSession(session)
                return@withContext
            }

            for ((kind, value) in photos) {
                val uri = Uri.parse(value)
                val path = "$userId/$kind.image"
                val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
                client.request("/storage/v1/object/registration-photos/$path", "POST", token, mime,
                    mapOf("x-upsert" to "true")) { connection ->
                    val input = resolver.openInputStream(uri) ?: throw IOException("Unable to read image")
                    input.use { source ->
                        connection.setChunkedStreamingMode(64 * 1024)
                        connection.outputStream.use { source.copyTo(it) }
                    }
                }
                document.put(if (kind == "avatar") "avatar_path" else "${kind}_photo_path", path)
            }
            document.put("user_id", userId)
            client.json(profileRoute, "POST", document, token)
            saveSession(session)
            Unit
        }

    class ConfirmationRequired : Exception()
    class ProfileExists : Exception()
    class ServerError(val status: Int, val code: String) : IOException()

    companion object {
        const val BASE_URL = SupabaseClient.BASE_URL
    }
}
