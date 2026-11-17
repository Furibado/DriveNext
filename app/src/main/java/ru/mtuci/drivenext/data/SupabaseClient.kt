package ru.mtuci.drivenext.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import ru.mtuci.drivenext.domain.NoConnectionException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Общие запросы Supabase и обновление сессии для авторизованных запросов. */
internal class SupabaseClient(context: Context) {
    private val connectionRepository = ConnectionRepositoryImpl(context)
    private val sessions = AuthSessionStore(context)
    private val refreshLock = Mutex()
    private var accessToken: String? = null
    private var expiresAt = 0L

    fun saveSession(session: JSONObject) {
        sessions.save(session.getString("refresh_token"))
        accessToken = session.getString("access_token")
        expiresAt = System.currentTimeMillis() + session.optLong("expires_in", 3600) * 1000
    }

    suspend fun <T> withAccessToken(operation: (String) -> T): T = refreshLock.withLock {
        if (accessToken == null || System.currentTimeMillis() >= expiresAt - 60_000) {
            val refresh = sessions.read() ?: throw IOException("No active session")
            saveSession(json("/auth/v1/token?grant_type=refresh_token", "POST",
                JSONObject().put("refresh_token", refresh)))
        }
        try {
            operation(checkNotNull(accessToken))
        } catch (error: RegistrationRepository.ServerError) {
            if (error.status != 401) throw error
            val refresh = sessions.read() ?: throw error
            saveSession(json("/auth/v1/token?grant_type=refresh_token", "POST",
                JSONObject().put("refresh_token", refresh)))
            operation(checkNotNull(accessToken))
        }
    }

    suspend fun authorizedGet(path: String): String = withAccessToken { request(path, "GET", it) }

    suspend fun signOut() {
        try {
            withAccessToken { request("/auth/v1/logout?scope=local", "POST", it) }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            // Даже без сети выход с этого устройства должен удалить локальную сессию.
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                refreshLock.withLock {
                    sessions.clear()
                    accessToken = null
                    expiresAt = 0L
                }
            }
        }
    }

    fun download(path: String, token: String): ByteArray {
        if (!connectionRepository.isInternetAvailable()) throw NoConnectionException()
        val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Cache-Control", "no-cache")
            if (connection.responseCode !in 200..299) throw IOException("Unable to download avatar")
            return connection.inputStream.use { it.readBytes() }
        } finally { connection.disconnect() }
    }

    fun json(path: String, method: String, body: JSONObject, token: String? = null): JSONObject {
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val response = request(path, method, token, "application/json") { connection ->
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
        }
        return if (response.isBlank()) JSONObject() else JSONObject(response)
    }

    fun request(
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
                throw RegistrationRepository.ServerError(status, code)
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (error: IOException) {
            if (!connectionRepository.isInternetAvailable()) throw NoConnectionException()
            throw error
        } finally {
            connection.disconnect()
        }
    }


    companion object {
        const val BASE_URL = "https://mrxeldpxftrrvjuymogk.supabase.co"
        private const val PUBLISHABLE_KEY = "sb_publishable_hoak7D0oW9Xip62v9uLAmA_E0ftSXyf"
    }
}
