package com.clearkeep.presentation.screen.chat.notification_setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.domain.model.UserPreference
import com.clearkeep.domain.repository.UserPreferenceRepository
import com.clearkeep.data.remote.dynamicapi.Environment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val environment: Environment,
    private val userPreferenceRepository: UserPreferenceRepository
) : ViewModel() {
    private lateinit var _userPreference: LiveData<UserPreference>
    val userPreference: LiveData<UserPreference>
        get() = _userPreference

    init {
        getUserPreference()
    }

    private fun getUserPreference() {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            _userPreference = userPreferenceRepository.getUserPreferenceState(domain, userId)
        }
    }

    fun toggleShowPreview(enabled: Boolean) {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            userPreferenceRepository.updateShowNotificationPreview(domain, userId, enabled)
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