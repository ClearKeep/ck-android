package com.clearkeep.screen.chat.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.ProfileRepository
import com.clearkeep.screen.chat.utils.getLinkFromPeople
import com.clearkeep.utilities.files.*
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    private val environment: Environment,
    private val profileRepository: ProfileRepository
): ViewModel() {

    val profile: LiveData<Profile?> = liveData(viewModelScope.coroutineContext + IO) {
        emit(environment.getServer().profile)
    }

    private var _currentPhotoUri: Uri? = null

    private val _imageUriSelected = MutableLiveData<String>()
    val imageUriSelected: LiveData<String>
        get() = _imageUriSelected

    val uploadAvatarResponse = MutableLiveData<Resource<String>>()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String>
        get() = _username

    private val _email = MutableLiveData<String>()
    val email: LiveData<String>
        get() = _email

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String>
        get() = _phoneNumber

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

    fun setEmail(email: String) {
        _email.value = email
    }

    fun setPhoneNumber(phoneNumber: String) {
        _phoneNumber.value = phoneNumber
    }

    fun setUsername(username: String) {
        _username.value = username
    }

    fun setSelectedImage(uris: String) {
        _imageUriSelected.value = uris
    }

    fun setTakePhoto() {
        _imageUriSelected.value = _currentPhotoUri.toString()
        _currentPhotoUri = null
    }

    fun updateProfileDetail(context: Context, displayName: String, phoneNumber: String) {
        val avatarToUpload = _imageUriSelected.value
        val server = environment.getServer()
        if (!avatarToUpload.isNullOrEmpty()) {
            //Update avatar case
            if (!isValidFileSizes(context, Uri.parse(avatarToUpload))) {
                printlnCK("uploadAvatar exceed file size limit")
                uploadAvatarResponse.value =
                    Resource.error("Failed to upload avatar - File is larger than 4 MB", null)
                return
            }

            GlobalScope.launch {
                uploadAvatarImage(avatarToUpload, context)
                profileRepository.updateProfile(
                    Owner(server.serverDomain, server.profile.userId),
                    displayName,
                    phoneNumber,
                    avatarToUpload
                )
            }
        } else {
            //Update normal data only
            viewModelScope.launch {
                profileRepository.updateProfile(
                    Owner(server.serverDomain, server.profile.userId),
                    displayName,
                    phoneNumber,
                    null
                )
            }
        }
    }

    private suspend fun uploadAvatarImage(avatarToUpload: String, context: Context) {
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

    fun getPhotoUri(context: Context): Uri {
        if (_currentPhotoUri == null) {
            _currentPhotoUri = generatePhotoUri(context)
        }
        return _currentPhotoUri!!
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
