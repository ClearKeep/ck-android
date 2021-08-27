package com.clearkeep.screen.chat.repo

import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.utilities.network.Resource
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

    suspend fun getWorkspaceInfo(domain: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val request = WorkspaceOuterClass
                .WorkspaceInfoRequest
                .newBuilder()
                .setWorkspaceDomain(domain)
                .build()

            val response =
                paramAPIProvider.provideWorkspaceBlockingStub(ParamAPI(domain)).workspaceInfo(request)

            return@withContext if (response.error.isEmpty()) Resource.success("") else Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            printlnCK("getWorkspaceInfo response exception? $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            return@withContext Resource.error(e.toString(), null)
        }
    }
}