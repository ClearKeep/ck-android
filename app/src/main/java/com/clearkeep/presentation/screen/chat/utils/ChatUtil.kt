package com.clearkeep.presentation.screen.chat.utils

import com.clearkeep.domain.model.User
import com.clearkeep.utilities.printlnCK

fun getGroupType(isGroup: Boolean): String {
    return if (isGroup) "group" else "peer"
}

fun getLinkFromPeople(people: com.clearkeep.domain.model.User): String {
    return "${people.domain}/${people.userName}/${people.userId}"
}

fun getPeopleFromLink(link: String): com.clearkeep.domain.model.User? {
    val args = link.split("/")
    if (args.size != 3) {
        return null
    }
    printlnCK("${args[2]}, ${args[1]}, ${args[0]}")
    return com.clearkeep.domain.model.User(userId = args[2], userName = args[1], domain = args[0])
}