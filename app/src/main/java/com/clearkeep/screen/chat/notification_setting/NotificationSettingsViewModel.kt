package com.clearkeep.screen.chat.notification_setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearkeep.db.clear_keep.model.UserPreference
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.PeopleRepository
import com.clearkeep.screen.chat.repo.UserPreferenceRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationSettingsViewModel @Inject constructor(
    private val environment: Environment,
    private val userPreferenceRepository: UserPreferenceRepository
) : ViewModel() {

    private lateinit var _userPreference : LiveData<UserPreference>
    val userPreference : LiveData<UserPreference>
        get() = _userPreference

    init {
        getUserPreference()
    }

    private fun getUserPreference() {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            _userPreference = userPreferenceRepository.getUserPreferenceLiveData(domain, userId)
        }
    }

    fun toggleShowPreview(enabled: Boolean) {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            userPreferenceRepository.updateShowNotificationPreview(domain, userId, enabled)
        }
    }

    fun toggleSoundAndVibrate(enabled: Boolean) {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            userPreferenceRepository.updateNotificationSoundVibrate(domain, userId, enabled)
        }
    }

    fun toggleDoNotDisturb(enabled: Boolean) {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            userPreferenceRepository.updateDoNotDisturb(domain, userId, enabled)
        }
    }
}