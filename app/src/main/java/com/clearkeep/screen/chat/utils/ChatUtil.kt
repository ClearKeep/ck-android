package com.clearkeep.screen.chat.utils

import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.utilities.printlnCK

fun isGroup(groupType: String) : Boolean {
    return groupType == "group"
}

fun getGroupType(isGroup: Boolean) : String {
    return if (isGroup) "group" else "peer"
}

fun getLinkFromPeople(people: People) : String {
    return "${people.workspace}/${people.userName}/${people.id}"
}

fun getPeopleFromLink(link: String) : People? {
    val args = link.split("/")
    if (args.size != 3) {
        return null
    }
    printlnCK("${args[2]}, ${args[1]}, ${args[0]}")
    return People(args[2], args[1], args[0])
}