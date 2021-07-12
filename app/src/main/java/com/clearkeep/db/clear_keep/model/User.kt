package com.clearkeep.db.clear_keep.model

data class User(
    val userId: String,
    val userName: String,
    val domain: String,
) {
    override fun toString(): String {
        return "id = $userId, userName = $userName, workspace_domain = $domain"
    }
}