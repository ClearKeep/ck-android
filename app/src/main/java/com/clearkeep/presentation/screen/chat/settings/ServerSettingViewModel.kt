package com.clearkeep.presentation.screen.chat.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.usecase.server.GetDefaultServerAsStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ServerSettingViewModel @Inject constructor(
    private val getDefaultServerAsStateUseCase: GetDefaultServerAsStateUseCase
) : ViewModel() {
    val server: LiveData<com.clearkeep.domain.model.Server?> = getDefaultServerAsStateUseCase()
}
