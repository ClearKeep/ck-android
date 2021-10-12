package com.clearkeep.screen.chat.repo

import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.utilities.REQUEST_DEADLINE_SECONDS
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import workspace.WorkspaceOuterClass
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkSpaceRepository @Inject constructor(
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val paramAPIProvider: ParamAPIProvider,
    private val serverRepository: ServerRepository
) {
    suspend fun leaveServer(): WorkspaceOuterClass.BaseResponse? = withContext(Dispatchers.IO) {
        try {
            val request = WorkspaceOuterClass.LeaveWorkspaceRequest.newBuilder().build()
            return@withContext dynamicAPIProvider.provideWorkSpaceBlockingStub()
                .leaveWorkspace(request)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("leaveServer token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            printlnCK("leaveServer =====EROR: ${e.message}")
            return@withContext null
        }
    }

    suspend fun getWorkspaceInfo(currentDomain: String? = null, domain: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val request = WorkspaceOuterClass
                .WorkspaceInfoRequest
                .newBuilder()
                .setWorkspaceDomain(domain)
                .build()

            val response =
                paramAPIProvider.provideWorkspaceBlockingStub(ParamAPI(currentDomain ?: domain))
                    .withDeadlineAfter(
                        REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS
                    ).workspaceInfo(request)

            return@withContext if (response.error.isEmpty()) Resource.success("") else Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val (message, code) = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("getWorkspaceInfo token expired")
                    serverRepository.isLogout.postValue(true)
                    "" to 0
                }
                else -> {
                    if (e.status.code == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                        "Wrong server URL. Please try again" to 0
                    } else  {
                        parsedError.message to parsedError.code
                    }
                }
            }
            printlnCK("getWorkspaceInfo response exception? $message")
            return@withContext Resource.error(message, null, code)
        } catch (e: Exception) {
            return@withContext Resource.error(e.toString(), null)
        }
    }
}