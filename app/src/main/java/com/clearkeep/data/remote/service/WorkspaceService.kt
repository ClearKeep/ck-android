package com.clearkeep.data.remote.service

import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.clearkeep.utilities.REQUEST_DEADLINE_SECONDS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import workspace.WorkspaceOuterClass
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkspaceService @Inject constructor(
    private val paramAPIProvider: ParamAPIProvider,
) {
    suspend fun getWorkspaceInfo(
        workspaceDomain: String,
        currentDomain: String?
    ): WorkspaceOuterClass.WorkspaceInfoResponse = withContext(Dispatchers.IO) {
        val request = WorkspaceOuterClass
            .WorkspaceInfoRequest
            .newBuilder()
            .setWorkspaceDomain(workspaceDomain)
            .build()

        return@withContext paramAPIProvider.provideWorkspaceBlockingStub(
            ParamAPI(
                currentDomain ?: workspaceDomain
            )
        ).withDeadlineAfter(
            REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
        ).workspaceInfo(request)
    }
}