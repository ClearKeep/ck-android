package com.clearkeep.domain.usecase.profile

import android.content.Context
import android.net.Uri
import com.clearkeep.common.utilities.files.byteArrayToMd5HashString
import com.clearkeep.common.utilities.files.getFileMimeType
import com.clearkeep.common.utilities.files.getFileName
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.google.protobuf.ByteString
import java.security.MessageDigest
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(private val environment: Environment, private val profileRepository: ProfileRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(
        avatarToUpload: String,
        context: Context,
    ): String {
        val currentServer = environment.getServer()
        val owner = Owner(currentServer.serverDomain, currentServer.profile.userId)

        val uri = Uri.parse(avatarToUpload)
        val contentResolver = context.contentResolver
        val mimeType = getFileMimeType(context, uri, false)
        val fileName = uri.getFileName(context, false)
        val byteStrings = mutableListOf<ByteString>()
        val blockDigestStrings = mutableListOf<String>()
        val byteArray = ByteArray(FILE_UPLOAD_CHUNK_SIZE)
        val inputStream = contentResolver.openInputStream(uri)
        var fileSize = 0L
        var size: Int
        size = inputStream?.read(byteArray) ?: 0
        val fileDigest = MessageDigest.getInstance("MD5")
        while (size > 0) {
            val blockDigest = MessageDigest.getInstance("MD5")
            blockDigest.update(byteArray, 0, size)
            val blockDigestByteArray = blockDigest.digest()
            val blockDigestString = byteArrayToMd5HashString(blockDigestByteArray)
            blockDigestStrings.add(blockDigestString)
            fileDigest.update(byteArray, 0, size)
            byteStrings.add(ByteString.copyFrom(byteArray, 0, size))
            fileSize += size
            size = inputStream?.read(byteArray) ?: 0
        }
        val fileHashByteArray = fileDigest.digest()
        val fileHashString = byteArrayToMd5HashString(fileHashByteArray)
        val server = serverRepository.getServerByOwner(owner) ?: return ""
        return profileRepository.uploadAvatar(server, mimeType, fileName.replace(" ", "_"), byteStrings, fileHashString)
    }

    companion object {
        private const val FILE_UPLOAD_CHUNK_SIZE = 4_000_000 //4MB
    }
}