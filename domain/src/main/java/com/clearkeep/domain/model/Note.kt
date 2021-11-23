package com.clearkeep.domain.model

data class Note(
    val generateId: Long? = null,
    val content: String,
    val createdTime: Long,
    val ownerDomain: String,
    val ownerClientId: String,
    val isTemp: Boolean = false,
)