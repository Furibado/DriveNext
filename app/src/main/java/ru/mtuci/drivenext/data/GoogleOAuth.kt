package ru.mtuci.drivenext.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

internal class GoogleOAuth(context: Context) {
    private val pending = AtomicFile(File(context.noBackupFilesDir, "google_oauth_verifier"))

    fun start(): Uri {
        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val verifier = encode(random)
        val challenge = encode(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val output = pending.startWrite()
        try {
            output.write(verifier.toByteArray(Charsets.US_ASCII))
            pending.finishWrite(output)
        } catch (error: Exception) {
            pending.failWrite(output)
            throw error
        }
        return Uri.parse(RegistrationRepository.BASE_URL + "/auth/v1/authorize").buildUpon()
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", CALLBACK)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .build()
    }

    fun verifier(): String? = try {
        if (System.currentTimeMillis() - pending.baseFile.lastModified() in 0..600_000L) {
            pending.readFully().toString(Charsets.US_ASCII)
        } else null
    } catch (_: java.io.FileNotFoundException) {
        null
    }

    fun clear() = pending.delete()

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    companion object {
        private const val CALLBACK = "ru.mtuci.drivenext://auth-callback"

        fun matches(uri: Uri): Boolean =
            uri.scheme == "ru.mtuci.drivenext" && uri.encodedAuthority == "auth-callback" &&
                uri.path.isNullOrEmpty()
    }
}
