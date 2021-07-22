package com.clearkeep.db.clear_keep.model

data class User(
    val userId: String,
    val userName: String,
    val domain: String,
    var status: String? = UserStateTypeInGroup.ACTIVE.value
) {
    override fun toString(): String {
        return "id = $userId, userName = $userName, workspace_domain = $domain state: $status"
    }
}

enum class UserStateTypeInGroup(val value: String) {
    ACTIVE("active"),
    REMOVE("remove")
}