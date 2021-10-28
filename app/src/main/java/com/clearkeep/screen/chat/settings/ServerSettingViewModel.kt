package com.clearkeep.screen.chat.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.repo.ServerRepository
import javax.inject.Inject

class ServerSettingViewModel @Inject constructor(
    serverRepository: ServerRepository,
) : ViewModel() {
    val server: LiveData<Server?> = serverRepository.getDefaultServerAsState()
}
