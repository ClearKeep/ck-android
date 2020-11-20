package com.clearkeep.screen.chat.utils

fun isGroup(groupType: String) : Boolean {
    return groupType == "group"
}

fun getGroupType(isGroup: Boolean) : String {
    return if (isGroup) "group" else "peer"
}