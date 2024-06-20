package com.clearkeep.domain.model

data class UserEntity(
    val generateId: Int? = null,
    val userId: String,
    val userName: String,
    val domain: String,
    val ownerClientId: String,
    val ownerDomain: String,
    var userStatus: String? = UserStatus.ONLINE.value,
    var phoneNumber: String? = "",
    var avatar: String? = "",
    var email: String? = "",
) {
    override fun toString(): String {
        return "id = $userId, userName = $userName, workspace_domain = $domain"
    }
}