package com.clearkeep.domain.repository

import com.clearkeep.utilities.network.Resource

interface WorkSpaceRepository {
    suspend fun getWorkspaceInfo(currentDomain: String?, domain: String): Resource<String>
}