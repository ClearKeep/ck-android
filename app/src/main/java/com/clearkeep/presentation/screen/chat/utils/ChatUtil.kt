package com.clearkeep.presentation.screen.chat.utils

import com.clearkeep.domain.model.User
import com.clearkeep.common.utilities.printlnCK

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