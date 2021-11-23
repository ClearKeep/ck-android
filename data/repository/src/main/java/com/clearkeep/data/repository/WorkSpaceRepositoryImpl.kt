package com.clearkeep.data.repository

import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.data.remote.service.WorkspaceService
import com.clearkeep.domain.repository.WorkSpaceRepository
import com.clearkeep.utilities.parseError
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkSpaceRepositoryImpl @Inject constructor(
    private val workspaceService: WorkspaceService
) : WorkSpaceRepository {
    override suspend fun getWorkspaceInfo(
        currentDomain: String?,
        domain: String
    ): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = workspaceService.getWorkspaceInfo(domain, currentDomain)

                return@withContext if (response.error.isEmpty()) Resource.success("") else Resource.error(
                    response.error,
                    null
                )
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                val (message, code) = if (e.status.code == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                    "Wrong server URL. Please try again" to 0
                } else {
                    parsedError.message to parsedError.code
                }
                return@withContext Resource.error(message, null, code, parsedError.cause)
            } catch (e: Exception) {
                return@withContext Resource.error(e.toString(), null)
            }
        }
}