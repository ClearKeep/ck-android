package com.clearkeep.domain.model.response

data class AuthRes(
    val workspaceDomain: String,
    val workspaceName: String,
    val accessToken: String,
    val expiresIn: Long,
    val refreshExpiresIn: Long,
    val refreshToken: String,
    val tokenType: String,
    val sessionState: String,
    val scope: String,
    val hashKey: String,
    val sub: String,
    val preAccessToken: String,
    val requireAction: String,
    val salt: String,
    val clientKeyPeer: PeerGetClientKeyResponse,
    val ivParameter: String,
    val error: String
)