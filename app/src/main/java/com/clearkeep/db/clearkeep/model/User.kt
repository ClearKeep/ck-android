package com.clearkeep.db.clearkeep.model

data class User(
    val userId: String,
    var userName: String,
    val domain: String,
    var userState: String? = UserStateTypeInGroup.ACTIVE.value,
    var userStatus: String? = UserStatus.ONLINE.value,
    var phoneNumber: String? = "",
    var avatar: String? = "",
    var email: String? = "",
) {
    override fun toString(): String {
        return "id = $userId, userName = $userName, workspace_domain = $domain state: $userState"
    }
}

enum class UserStateTypeInGroup(val value: String) {
    ACTIVE("active"),
    REMOVE("remove")
}

enum class UserStatus(val value: String) {
    ONLINE("Online"),
    OFFLINE("Offline"),
    BUSY("Busy"),
    UNDEFINED("Undefined"),
}
