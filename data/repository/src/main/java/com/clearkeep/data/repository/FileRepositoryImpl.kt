package com.clearkeep.data.repository

import android.content.Context
import com.clearkeep.data.remote.service.DownloadService
import com.clearkeep.data.remote.service.UploadFileService
import com.clearkeep.domain.repository.FileRepository
import com.clearkeep.utilities.parseError
import com.clearkeep.utilities.printlnCK
import dagger.hilt.android.qualifiers.ApplicationContext
import io.grpc.StatusRuntimeException
import jdk.internal.loader.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.BaseRequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest
import upload_file.UploadFileOuterClass
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadFileService: UploadFileService,
    private val downloadService: DownloadService
) : FileRepository {
    override suspend fun uploadFile(
        mimeType: String,
        fileName: String,
        fileUri: String
    ): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                val uploadFileLink = getUploadedFileUrl(fileName, mimeType)
                val uploadId =
                    BinaryUploadRequest(context, serverUrl = uploadFileLink.uploadedFileUrl)
                        .setMethod("PUT")
                        .setFileToUpload(fileUri)
                        .startUpload()

                val isSuccessful = suspendCoroutine<Boolean> { cont ->
                    val requestObserverDelegate = object : RequestObserverDelegate {
                        override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                            printlnCK("uploadFile onCompleted")
                            try {
                                cont.resume(true)
                            } catch (e: IllegalStateException) {
                                printlnCK("uploadFile already resumed!")
                            }
                        }

                        override fun onCompletedWhileNotObserving() {
                            printlnCK("uploadFile onCompletedWhileNotObserving")
                        }

                        override fun onError(
                            context: Context,
                            uploadInfo: UploadInfo,
                            exception: Throwable
                        ) {
                            printlnCK("uploadFile onError")
                            cont.resume(false)
                        }

                        override fun onProgress(context: Context, uploadInfo: UploadInfo) {}

                        override fun onSuccess(
                            context: Context,
                            uploadInfo: UploadInfo,
                            serverResponse: ServerResponse
                        ) {
                            printlnCK("uploadFile onSuccess")
                        }
                    }

                    val requestObserver = BaseRequestObserver(context, requestObserverDelegate) {
                        it.uploadId == uploadId
                    }

                    requestObserver.register()
                }

                return@withContext if (isSuccessful) {
                    com.clearkeep.common.utilities.network.Resource.success(uploadFileLink.downloadFileUrl)
                } else {
                    com.clearkeep.common.utilities.network.Resource.error("Error uploading files!", null, 0)
                }
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                return@withContext com.clearkeep.common.utilities.network.Resource.error(
                    parsedError.message,
                    null,
                    parsedError.code,
                    parsedError.cause
                )
            } catch (e: Exception) {
                printlnCK("uploadFile exception $e")
                return@withContext com.clearkeep.common.utilities.network.Resource.error(e.toString(), null)
            }
        }
    }

    override suspend fun getUploadedFileUrl(
        fileName: String,
        mimeType: String
    ): UploadFileOuterClass.GetUploadFileLinkResponse = withContext(Dispatchers.IO) {
        uploadFileService.getUploadFileUrl(fileName, mimeType)
    }

    override fun downloadFile(fileName: String, url: String) {
        downloadService.downloadFile(fileName, url)
    }
}