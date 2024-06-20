package com.clearkeep.data.repository.file

import com.clearkeep.domain.model.response.GetUploadFileLinkResponse
import upload_file.UploadFileOuterClass

fun UploadFileOuterClass.GetUploadFileLinkResponse.toEntity() = GetUploadFileLinkResponse(uploadedFileUrl, downloadFileUrl, objectFilePath)