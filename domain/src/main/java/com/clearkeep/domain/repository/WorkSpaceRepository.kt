package com.clearkeep.domain.repository

import com.clearkeep.common.utilities.network.Resource

interface WorkSpaceRepository {
    suspend fun getWorkspaceInfo(currentDomain: String?, domain: String): Resource<String>
}