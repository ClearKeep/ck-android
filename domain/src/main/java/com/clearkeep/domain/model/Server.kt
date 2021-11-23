package com.clearkeep.domain.model

data class Server(
    val id: Int? = null,
    val serverName: String,
    val serverDomain: String,
    val ownerClientId: String,
    val serverAvatar: String?,
    val loginTime: Long,
    val accessKey: String,
    val hashKey: String,
    val refreshToken: String,
    val isActive: Boolean = false,
    val profile: Profile,
) {
    override fun toString(): String {
        return "${serverName}, $serverDomain, $ownerClientId   \n profile.userId: ${profile.userId}"
    }
}