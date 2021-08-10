package com.clearkeep.screen.chat.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.ProfileRepository
import com.clearkeep.screen.chat.room.RoomViewModel
import com.clearkeep.screen.chat.utils.getLinkFromPeople
import com.clearkeep.utilities.files.byteArrayToMd5HashString
import com.clearkeep.utilities.files.getFileMimeType
import com.clearkeep.utilities.files.getFileName
import com.clearkeep.utilities.files.getFileSize
import com.clearkeep.utilities.network.Resource
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    private val environment: Environment,
    private val profileRepository: ProfileRepository
): ViewModel() {

    val profile: LiveData<Profile?> = liveData(viewModelScope.coroutineContext + IO) {
        emit(environment.getServer().profile)
    }

    var avatarToUpload = ""
    val uploadAvatarResponse = MutableLiveData<Resource<String>>()

    fun getProfileLink(): String {
        val server = environment.getServer()
        return getLinkFromPeople(
            User(
                userId = server.profile.userId,
                userName = server.profile.userName ?: "",
                domain = server.serverDomain
            )
        )
    }

    fun updateProfileDetail(displayName: String, phoneNumber: String) {
        viewModelScope.launch {
            val server = environment.getServer()
            profileRepository.updateProfile(
                Owner(server.serverDomain, server.profile.userId),
                displayName,
                phoneNumber
            )
        }
    }

    fun uploadAvatarImage(context: Context) {
        if (!avatarToUpload.isNullOrEmpty()) {
            val avatarToUpload = avatarToUpload
            if (!isValidFileSizes(context, Uri.parse(avatarToUpload))) {
                uploadAvatarResponse.value =
                    Resource.error("Failed to upload avatar - File is larger than 4 MB", null)
                return
            }

            GlobalScope.launch {
                //TODO: Save temp profile avatar
                val fileUrls = mutableListOf<String>()
                val filesSizeInBytes = mutableListOf<Long>()
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
                val server = environment.getServer()
                val url = profileRepository.uploadAvatar(
                    Owner(server.serverDomain, server.profile.userId),
                    mimeType,
                    fileName.replace(" ", "_"),
                    byteStrings,
                    fileHashString
                )
                fileUrls.add(url)
                filesSizeInBytes.add(fileSize)
            }
        }
    }
    

    private fun isValidFileSizes(
        context: Context,
        uri: Uri,
    ): Boolean {
        return uri.getFileSize(context, false) <= AVATAR_MAX_SIZE
    }

    companion object {
        private const val AVATAR_MAX_SIZE = 4_000_000 //4MB
        private const val FILE_UPLOAD_CHUNK_SIZE = 4_000_000 //4MB
    }
}
