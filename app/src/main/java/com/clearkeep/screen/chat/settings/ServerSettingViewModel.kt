package com.clearkeep.screen.chat.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.clearkeep.db.clearkeep.model.Server
import com.clearkeep.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ServerSettingViewModel @Inject constructor(
    serverRepository: ServerRepository,
) : ViewModel() {
    val server: LiveData<Server?> = serverRepository.getDefaultServerAsState()
}
