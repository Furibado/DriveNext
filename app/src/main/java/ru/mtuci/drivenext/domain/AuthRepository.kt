package ru.mtuci.drivenext.domain

data class RegistrationData(
    val surname: String,
    val givenName: String,
    val patronymic: String?,
    val birthDate: String,
    val gender: String,
    val licenseNumber: String,
    val licenseIssueDate: String
)

interface AuthRepository {
    suspend fun signIn(email: String, password: String)
    suspend fun restoreSession(): Boolean
    suspend fun exchangeGoogleCode(code: String, verifier: String)
    suspend fun register(email: String, password: String, profile: RegistrationData, photos: Map<String, String>)
}
class IncompleteRegistrationException : Exception()
class NoConnectionException : Exception()
