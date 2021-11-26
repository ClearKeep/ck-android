package com.clearkeep.data.remote.utils

import auth.AuthOuterClass
import com.clearkeep.domain.model.response.*
import group.GroupOuterClass
import message.MessageOuterClass
import upload_file.UploadFileOuterClass

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

fun AuthOuterClass.SocialLoginRes.toEntity() = SocialLoginRes(userName, requireAction, resetPincodeToken)

fun UploadFileOuterClass.GetUploadFileLinkResponse.toEntity() = GetUploadFileLinkResponse(uploadedFileUrl, downloadFileUrl, objectFilePath)


fun GroupOuterClass.GroupObjectResponse.toEntity() = GroupObjectResponse(
    groupId,
    groupName,
    groupAvatar,
    groupType,
    lstClientList.map {
        GroupObjectResponse.ClientInGroupResponse(
            it.id,
            it.displayName,
            it.workspaceDomain,
            it.status
        )
    },
    lastMessageAt,
    lastMessage = GroupObjectResponse.MessageObjectResponse(
        lastMessage.id,
        lastMessage.groupId,
        lastMessage.groupType,
        lastMessage.fromClientId,
        lastMessage.clientId,
        lastMessage.message.toByteArray(),
        lastMessage.lstClientReadList.map {
            ClientReadObject(
                it.id,
                it.displayName,
                it.avatar
            )
        }
    ),
    createdByClientId,
    createdAt,
    updatedByClientId,
    updatedAt,
    groupRtcToken,
    hasUnreadMessage,
    clientKey = GroupObjectResponse.GroupClientKeyObject(
        clientKey.workspaceDomain,
        clientKey.clientId,
        clientKey.deviceId,
        clientKey.clientKeyDistribution.toByteArray(),
        clientKey.senderKeyId,
        clientKey.senderKey.toByteArray(),
        clientKey.publicKey.toByteArray(),
        clientKey.privateKey
    )
)

fun MessageOuterClass.ClientReadObject.toEntity() = ClientReadObject(id, displayName, avatar)

fun MessageOuterClass.MessageObjectResponse.toEntity() = MessageObjectResponse(
    id,
    groupId,
    groupType,
    fromClientId,
    fromClientWorkspaceDomain,
    clientId,
    clientWorkspaceDomain,
    message.toByteArray(),
    lstClientReadList.map { it.toEntity() },
    createdAt,
    updatedAt,
    senderMessage.toByteArray()
)

fun MessageOuterClass.GetMessagesInGroupResponse.toEntity() =
    GetMessagesInGroupResponse(lstMessageList.map { it.toEntity() })