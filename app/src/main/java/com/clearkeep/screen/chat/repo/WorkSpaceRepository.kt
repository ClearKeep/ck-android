package com.clearkeep.screen.chat.repo

import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import workspace.WorkspaceOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkSpaceRepository @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider,
) {
    suspend fun leaveServer(): WorkspaceOuterClass.BaseResponse? = withContext(Dispatchers.IO) {
        try {
            val request = WorkspaceOuterClass.LeaveWorkspaceRequest.newBuilder().build()
            return@withContext dynamicAPIProvider.provideWorkSpaceBlockingStub()
                .leaveWorkspace(request)
        } catch (e: Exception) {
            e.printStackTrace()
            printlnCK("leaveServer =====EROR: ${e.message}")
            return@withContext null
        }
    }
}