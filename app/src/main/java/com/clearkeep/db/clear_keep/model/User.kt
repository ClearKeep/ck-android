package com.clearkeep.db.clear_keep.model

data class User(
    val userId: String,
    val userName: String,
    val domain: String,
    var userState: String? = UserStateTypeInGroup.ACTIVE.value,
    var userStatus: String?=UserStatus.ONLINE.value
) {
    override fun toString(): String {
        return "id = $userId, userName = $userName, workspace_domain = $domain state: $userState"
    }
}

enum class UserStateTypeInGroup(val value: String) {
    ACTIVE("active"),
    REMOVE("remove")
}
enum class UserStatus(val value:String){
    ONLINE("Online"),
    OFFLINE("Offline"),
    BUSY("Busy")
}
