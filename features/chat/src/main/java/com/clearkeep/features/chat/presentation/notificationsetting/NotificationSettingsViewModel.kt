package com.clearkeep.features.chat.presentation.notificationsetting

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.domain.model.UserPreference
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.usecase.preferences.GetUserPreferenceUseCase
import com.clearkeep.domain.usecase.preferences.UpdateDoNotDisturbUseCase
import com.clearkeep.domain.usecase.preferences.UpdateShowNotificationPreviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val environment: Environment,
    private val getUserPreferenceUseCase: GetUserPreferenceUseCase,
    private val updateShowNotificationPreviewUseCase: UpdateShowNotificationPreviewUseCase,
    private val updateDoNotDisturbUseCase: UpdateDoNotDisturbUseCase
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
            _userPreference = getUserPreferenceUseCase.asState(domain, userId)
        }
    }

    fun toggleShowPreview(enabled: Boolean) {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            updateShowNotificationPreviewUseCase(domain, userId, enabled)
        }
    }

    fun toggleDoNotDisturb(enabled: Boolean) {
        viewModelScope.launch {
            val domain = environment.getServer().serverDomain
            val userId = environment.getServer().profile.userId
            updateDoNotDisturbUseCase(domain, userId, enabled)
        }
    }
}