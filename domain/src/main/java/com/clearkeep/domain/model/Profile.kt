package com.clearkeep.domain.model

data class Profile(
    val generateId: Int? = null,
    val userId: String,
    val userName: String?,
    val email: String?,
    val phoneNumber: String?,
    val updatedAt: Long,
    var avatar: String?,
)