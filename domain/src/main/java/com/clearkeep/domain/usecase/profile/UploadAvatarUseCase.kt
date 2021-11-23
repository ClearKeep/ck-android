package com.clearkeep.domain.usecase.profile

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ProfileRepository
import com.clearkeep.domain.repository.ServerRepository
import com.google.protobuf.ByteString
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(private val profileRepository: ProfileRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(
        owner: com.clearkeep.domain.model.Owner,
        mimeType: String,
        fileName: String,
        byteStrings: List<ByteString>,
        fileHash: String
    ): String {
        val server = serverRepository.getServerByOwner(owner) ?: return ""
        return profileRepository.uploadAvatar(server, mimeType, fileName.replace(" ", "_"), byteStrings, fileHash)
    }
}