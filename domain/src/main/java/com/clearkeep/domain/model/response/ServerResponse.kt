package com.clearkeep.domain.model.response

data class StunServer(val server: String, val port: Long)

data class TurnServer(
    val server: String,
    val port: Long,
    val type: String,
    val user: String,
    val pwd: String
)

data class ServerResponse(
    val groupRtcUrl: String,
    val groupRtcId: Long,
    val groupRtcToken: String,
    val stunServer: StunServer,
    val turnServer: TurnServer
)