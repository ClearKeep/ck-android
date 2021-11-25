package com.clearkeep.domain.model

data class MessagePagingResponse(
    val isSuccess: Boolean,
    val endOfPaginationReached: Boolean,
    val newestMessageLoadedTimestamp: Long
)