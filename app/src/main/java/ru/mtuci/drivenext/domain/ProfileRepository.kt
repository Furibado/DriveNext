package ru.mtuci.drivenext.domain

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: String,
    val gender: String,
    val googleEmail: String,
    val avatarPath: String,
    val googleAvatarUrl: String,
    val hasRegistration: Boolean
)

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun getAvatar(profile: UserProfile): ByteArray?
    suspend fun updateAvatar(profile: UserProfile, uri: String)
    suspend fun signOut()
}
