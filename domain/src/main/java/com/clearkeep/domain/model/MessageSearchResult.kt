package com.clearkeep.domain.model

data class MessageSearchResult(val message: Message, val user: User?, val group: ChatGroup?)