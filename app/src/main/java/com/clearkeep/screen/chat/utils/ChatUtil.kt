package com.clearkeep.screen.chat.utils

import com.clearkeep.db.clearkeep.model.User
import com.clearkeep.utilities.printlnCK

fun isGroup(groupType: String): Boolean {
    return groupType == "group"
}

fun getGroupType(isGroup: Boolean): String {
    return if (isGroup) "group" else "peer"
}

fun getLinkFromPeople(people: User): String {
    return "${people.domain}/${people.userName}/${people.userId}"
}

fun getPeopleFromLink(link: String): User? {
    val args = link.split("/")
    if (args.size != 3) {
        return null
    }
    printlnCK("${args[2]}, ${args[1]}, ${args[0]}")
    return User(userId = args[2], userName = args[1], domain = args[0])
}