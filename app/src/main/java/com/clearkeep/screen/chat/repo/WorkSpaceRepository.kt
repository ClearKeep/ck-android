package com.clearkeep.screen.chat.repo

import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import workspace.WorkspaceOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkSpaceRepository @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val paramAPIProvider: ParamAPIProvider
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

    suspend fun getWorkspaceInfo(domain: String): String = withContext(Dispatchers.IO) {
        try {
            val request = WorkspaceOuterClass
                .WorkspaceInfoRequest
                .newBuilder()
                .setWorkspaceDomain(domain)
                .build()
            val response =
                paramAPIProvider.provideWorkspaceBlockingStub(ParamAPI(domain)).workspaceInfo(request)

            printlnCK("getWorkspaceInfo response error? ${response.error}")
            return@withContext response.error
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            return@withContext message
        } catch (e: Exception) {
            return@withContext e.toString()
        }
    }
}