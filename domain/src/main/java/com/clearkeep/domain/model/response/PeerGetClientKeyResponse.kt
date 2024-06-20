package com.clearkeep.domain.model.response

class PeerGetClientKeyResponse(
    val clientId: String,
    val workspaceDomain: String,
    val registrationId: Int,
    val deviceId: Int,
    val identityKeyPublic: ByteArray,
    val preKeyId: Int,
    val preKey: ByteArray,
    val signedPreKeyId: Int,
    val signedPreKey: ByteArray,
    val signedPreKeySignature: ByteArray,
    val identityKeyEncrypted: String
)