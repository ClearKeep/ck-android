package com.clearkeep.domain.model

class GroupClientKeyObject(val workspaceDomain: String, val clientId: String, val deviceId: Int, val clientKeyDistribution: ByteArray)

data class GroupGetClientKeyResponse(val groupId: Long, val clientKey: GroupClientKeyObject)