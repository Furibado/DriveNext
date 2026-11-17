package ru.mtuci.drivenext.data

import android.content.Context
import android.net.Uri
import ru.mtuci.drivenext.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

internal class RegistrationRepository(context: Context) : AuthRepository {
    private val connectionRepository = ConnectionRepositoryImpl(context)
    private val resolver = context.contentResolver
    private val sessions = AuthSessionStore(context)

    override suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        val credentials = JSONObject().put("email", email).put("password", password)
        val session = json("/auth/v1/token?grant_type=password", "POST", credentials)
        requireCompletedProfile(session)
        saveSession(session)
    }

    override suspend fun exchangeGoogleCode(code: String, verifier: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("auth_code", code).put("code_verifier", verifier)
        saveSession(json("/auth/v1/token?grant_type=pkce", "POST", body))
    }

    override suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val token = sessions.read() ?: return@withContext false
        try {
            val credentials = JSONObject().put("refresh_token", token)
            val session = json("/auth/v1/token?grant_type=refresh_token", "POST", credentials)
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
        val response = request("/rest/v1/registration_profiles?user_id=eq.$userId&select=user_id",
            "GET", session.getString("access_token"))
        if (JSONArray(response).length() == 0) throw IncompleteRegistrationException()
    }

    private fun saveSession(session: JSONObject) {
        sessions.save(session.getString("refresh_token"))
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
                json("/auth/v1/token?grant_type=password", "POST", credentials)
            } catch (error: ServerError) {
                if (error.code == "email_not_confirmed") throw ConfirmationRequired()
                if (error.code != "invalid_credentials") throw error
                null
            }
            val session = existingSession ?: json("/auth/v1/signup", "POST", credentials)
            val token = session.optString("access_token")
            if (token.isBlank()) throw ConfirmationRequired()

            val userId = UUID.fromString(session.getJSONObject("user").getString("id")).toString()
            val profileRoute = "/rest/v1/registration_profiles"
            val rows = request("$profileRoute?user_id=eq.$userId&select=user_id", "GET", token)
            if (JSONArray(rows).length() != 0) {
                saveSession(session)
                return@withContext
            }

            for ((kind, value) in photos) {
                val uri = Uri.parse(value)
                val path = "$userId/$kind.image"
                val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
                request("/storage/v1/object/registration-photos/$path", "POST", token, mime,
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
            json(profileRoute, "POST", document, token)
            saveSession(session)
            Unit
        }

    private fun json(path: String, method: String, body: JSONObject, token: String? = null): JSONObject {
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val response = request(path, method, token, "application/json") { connection ->
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
        }
        return if (response.isBlank()) JSONObject() else JSONObject(response)
    }

    private fun request(
        path: String,
        method: String,
        token: String?,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap(),
        writeBody: ((HttpURLConnection) -> Unit)? = null
    ): String {
        if (!connectionRepository.isInternetAvailable()) throw NoConnectionException()
        val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (contentType != null) connection.setRequestProperty("Content-Type", contentType)
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (writeBody != null) {
                connection.doOutput = true
                writeBody(connection)
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val code = runCatching { JSONObject(body).optString("error_code") }.getOrDefault("")
                throw ServerError(status, code)
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (error: IOException) {
            if (!connectionRepository.isInternetAvailable()) throw NoConnectionException()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    class ConfirmationRequired : Exception()
    class ProfileExists : Exception()
    class ServerError(val status: Int, val code: String) : IOException()

    companion object {
        const val BASE_URL = "https://mrxeldpxftrrvjuymogk.supabase.co"
        private const val PUBLISHABLE_KEY = "sb_publishable_hoak7D0oW9Xip62v9uLAmA_E0ftSXyf"
    }
}
