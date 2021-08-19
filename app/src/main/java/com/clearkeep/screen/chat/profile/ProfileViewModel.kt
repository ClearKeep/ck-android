package com.clearkeep.screen.chat.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.ProfileRepository
import com.clearkeep.screen.chat.repo.UserPreferenceRepository
import com.clearkeep.screen.chat.utils.getLinkFromPeople
import com.clearkeep.utilities.files.*
import com.clearkeep.utilities.isValidCountryCode
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import com.google.i18n.phonenumbers.CountryCodeToRegionCodeMap
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

class ProfileViewModel @Inject constructor(
    private val environment: Environment,
    private val profileRepository: ProfileRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    serverRepository: ServerRepository
): ViewModel() {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    val profile: LiveData<Profile?> = serverRepository.getDefaultServerProfileAsState().map {
        _username.postValue(it.userName)
        _email.postValue(it.email)

        printlnCK("Profile full phone ${it.phoneNumber}")
        try {
            val numberProto: PhoneNumber = phoneUtil.parse(it.phoneNumber, "")
            val countryCode = numberProto.countryCode
            printlnCK("Profile country code $countryCode")
            val phoneNum = it.phoneNumber?.replace("+$countryCode", "")
            printlnCK("Profile phone num $phoneNum")
            _countryCode.postValue(countryCode.toString())
            _phoneNumber.postValue(phoneNum)
        } catch(e: Exception) {
            _phoneNumber.postValue(it.phoneNumber)
        }
        it
    }

    private lateinit var _userPreference : LiveData<UserPreference>
    val userPreference : LiveData<UserPreference>
        get() = _userPreference

    private var _currentPhotoUri: Uri? = null
    private var isAvatarChanged = false

    private val _imageUriSelected = MutableLiveData<String>()
    val imageUriSelected: LiveData<String>
        get() = _imageUriSelected

    val uploadAvatarResponse = MutableLiveData<Resource<String>>()

    val updateMfaSettingResponse = MutableLiveData<Boolean>()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String>
        get() = _username

    private val _email = MutableLiveData<String>()
    val email: LiveData<String>
        get() = _email

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String>
        get() = _phoneNumber

    private val _countryCode = MutableLiveData<String>()
    val countryCode: LiveData<String>
        get() = _countryCode

    val unsavedChangeDialogVisible = MutableLiveData<Boolean>()

    fun getMfaDetail() {
        val server = environment.getServer()
        viewModelScope.launch {
            profileRepository.getMfaSettingsFromAPI(Owner(server.serverDomain, server.profile.userId))
        }
        _userPreference = userPreferenceRepository.getUserPreferenceLiveData(server.serverDomain, server.profile.userId)
    }

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
        _phoneNumber.value = phoneNumber.filter { it.isDigit() }
    }

    fun setCountryCode(countryCode: String) {
        _countryCode.value = countryCode.filter { it.isDigit() }
    }

    fun setUsername(username: String) {
        _username.value = username
    }

    fun setSelectedImage(uris: String) {
        _imageUriSelected.value = uris
        isAvatarChanged = true
    }

    fun setTakePhoto() {
        _imageUriSelected.value = _currentPhotoUri.toString()
        _currentPhotoUri = null
        isAvatarChanged = true
    }

    fun updateProfileDetail(context: Context, displayName: String, phoneNumber: String, countryCode: String) {
        isAvatarChanged = false
        val avatarToUpload = imageUriSelected.value
        val server = environment.getServer()
        val isPhoneNumberChanged = phoneNumber != profile.value?.phoneNumber
        val shouldUpdateMfaSetting = isPhoneNumberChanged && userPreference.value?.mfa == true

        if (!isValidUsername(displayName)) {
            uploadAvatarResponse.value =
                Resource.error("You can’t leave Username blank", null)
            undoProfileChanges()
            return
        }

        if (!isValidCountryCode(_countryCode.value ?: "")) {
            uploadAvatarResponse.value = Resource.error("Invalid country code", null)
            undoProfileChanges()
            return
        }

        try {
            val numberProto = phoneUtil.parse("+${countryCode}${phoneNumber}", null)
            if (!phoneUtil.isValidNumber(numberProto)) {
                uploadAvatarResponse.value = Resource.error("Invalid phone number", null)
                undoProfileChanges()
                return
            }
        } catch (e: Exception) {
            printlnCK("updateProfileDetail error $e")
            if (e is NumberParseException) {
                val errorMessage = when (e.errorType) {
                    NumberParseException.ErrorType.INVALID_COUNTRY_CODE -> {
                        "Invalid country code"
                    }
                    NumberParseException.ErrorType.TOO_LONG -> {
                        "Too long"
                    }
                    NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD -> {
                        "TOO_SHORT_AFTER_IDD"
                    }
                    NumberParseException.ErrorType.TOO_SHORT_NSN -> {
                        "TOO_SHORT_NSN"
                    }
                    else -> "Error!"
                }
                uploadAvatarResponse.value =
                    Resource.error(errorMessage, null)
            }
            undoProfileChanges()
            return
        }

        if (!avatarToUpload.isNullOrEmpty()) {
            //Update avatar case
            if (!isValidFileSizes(context, Uri.parse(avatarToUpload))) {
                printlnCK("uploadAvatar exceed file size limit")
                uploadAvatarResponse.value =
                    Resource.error("Failed to upload avatar - File is larger than 5 MB", null)
                undoProfileChanges()
                return
            }

            GlobalScope.launch {
                val avatarUrl = uploadAvatarImage(avatarToUpload, context)
                if (profile.value != null) {
                    profileRepository.updateProfile(
                        Owner(server.serverDomain, server.profile.userId),
                        profile.value!!.copy(userName = displayName, phoneNumber = "+${countryCode}${phoneNumber}", avatar = avatarUrl, updatedAt = Calendar.getInstance().timeInMillis)
                    )
                    if (shouldUpdateMfaSetting) {
                        val response = profileRepository.updateMfaSettings(getOwner(), false)
                        if (response) {
                            userPreferenceRepository.updateMfa(server.serverDomain, server.profile.userId, false)
                        }
                    }
                }
            }
        } else {
            //Update normal data only
            viewModelScope.launch {
                if (profile.value != null) {
                    profileRepository.updateProfile(
                        Owner(server.serverDomain, server.profile.userId),
                        profile.value!!.copy(userName = displayName, phoneNumber = "+${countryCode}${phoneNumber}")
                    )
                    if (shouldUpdateMfaSetting) {
                        val response = profileRepository.updateMfaSettings(getOwner(), false)
                        if (response) {
                            userPreferenceRepository.updateMfa(server.serverDomain, server.profile.userId, false)
                        }
                    }
                }
            }
        }
    }

    fun hasUnsavedChanges() : Boolean {
        val usernameChanged = _username.value != profile.value?.userName
        val emailChanged = _email.value != profile.value?.email
        val phoneNumberChanged = _phoneNumber.value != profile.value?.phoneNumber

        val hasUnsavedChange = usernameChanged || emailChanged || phoneNumberChanged || isAvatarChanged
        unsavedChangeDialogVisible.value = hasUnsavedChange
        return hasUnsavedChange
    }

    fun undoProfileChanges() {
        val oldProfile = profile.value
        _phoneNumber.value = oldProfile?.phoneNumber
        _email.value = oldProfile?.email
        _username.value = oldProfile?.userName
        _imageUriSelected.value = null
        isAvatarChanged = false
    }

    fun canEnableMfa() : Boolean = !profile.value?.phoneNumber.isNullOrEmpty()

    fun updateMfaSettings(enabled: Boolean) {
        viewModelScope.launch {
            val isSuccess = profileRepository.updateMfaSettings(getOwner(), enabled)
            updateMfaSettingResponse.value = isSuccess
        }
    }

    private suspend fun uploadAvatarImage(avatarToUpload: String, context: Context) : String {
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
        return profileRepository.uploadAvatar(
            Owner(server.serverDomain, server.profile.userId),
            mimeType,
            fileName.replace(" ", "_"),
            byteStrings,
            fileHashString
        )
    }

    fun getPhotoUri(context: Context): Uri {
        if (_currentPhotoUri == null) {
            _currentPhotoUri = generatePhotoUri(context)
        }
        return _currentPhotoUri!!
    }

    private fun getOwner() : Owner {
        val server = environment.getServer()
        return Owner(server.serverDomain, server.profile.userId)
    }

    private fun isValidFileSizes(
        context: Context,
        uri: Uri,
    ): Boolean {
        return uri.getFileSize(context, false) <= AVATAR_MAX_SIZE
    }

    private fun isValidUsername(username: String?) : Boolean = !username.isNullOrEmpty()

    companion object {
        private const val AVATAR_MAX_SIZE = 4_000_000 //4MB
        private const val FILE_UPLOAD_CHUNK_SIZE = 4_000_000 //4MB
    }
}
