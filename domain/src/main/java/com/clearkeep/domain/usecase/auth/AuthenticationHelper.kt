package com.clearkeep.domain.usecase.auth

import java.util.*

class AuthenticationHelper {
    companion object {
        fun getUUID(groupId: String, clientId: String): UUID {
            return UUID.fromString(clientId.replaceRange(0, groupId.length, groupId))
        }
    }
}