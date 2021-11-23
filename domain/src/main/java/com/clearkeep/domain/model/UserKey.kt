package com.clearkeep.domain.model

data class UserKey(
    val serverDomain: String,
    val userId: String,
    val salt: String,
    val iv: String
)