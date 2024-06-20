package com.clearkeep.data.repository.auth

import auth.AuthOuterClass
import com.clearkeep.domain.model.response.AuthChallengeRes
import com.clearkeep.domain.model.response.AuthRes
import com.clearkeep.domain.model.response.PeerGetClientKeyResponse
import com.clearkeep.domain.model.response.SocialLoginRes

fun AuthOuterClass.SocialLoginRes.toEntity() = SocialLoginRes(userName, requireAction, resetPincodeToken)

fun AuthOuterClass.PeerGetClientKeyResponse.toEntity() = PeerGetClientKeyResponse(
    clientId,
    workspaceDomain,
    registrationId,
    deviceId,
    identityKeyPublic.toByteArray(),
    preKeyId,
    preKey.toByteArray(),
    signedPreKeyId,
    signedPreKey.toByteArray(),
    signedPreKeySignature.toByteArray(),
    identityKeyEncrypted
)

fun AuthOuterClass.AuthRes.toEntity() = AuthRes(
    workspaceDomain,
    workspaceName,
    accessToken,
    expiresIn,
    refreshExpiresIn,
    refreshToken,
    tokenType,
    sessionState,
    scope,
    hashKey,
    sub,
    preAccessToken,
    requireAction,
    salt,
    clientKeyPeer.toEntity(),
    ivParameter,
    error
)

fun AuthOuterClass.AuthChallengeRes.toEntity() = AuthChallengeRes(salt, publicChallengeB)