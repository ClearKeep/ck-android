package com.clearkeep.presentation.screen.videojanus

import androidx.lifecycle.ViewModel
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.usecase.call.CancelCallUseCase
import com.clearkeep.domain.usecase.call.RequestVideoCallUseCase
import com.clearkeep.domain.usecase.call.SwitchAudioToVideoCallUseCase
import com.clearkeep.domain.usecase.group.GetGroupByIdUseCase
import com.clearkeep.domain.usecase.server.GetDefaultServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InCallViewModel @Inject constructor(
    private val requestVideoCallUseCase: RequestVideoCallUseCase,
    private val cancelCallUseCase: CancelCallUseCase,
    private val switchAudioToVideoCallUseCase: SwitchAudioToVideoCallUseCase,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val getDefaultServerUseCase: GetDefaultServerUseCase,
) :
    ViewModel() {
    suspend fun requestVideoCall(groupId: Int, isAudioMode: Boolean, owner: com.clearkeep.domain.model.Owner) =
        requestVideoCallUseCase(groupId, isAudioMode, owner)

    suspend fun cancelCall(groupId: Int, owner: com.clearkeep.domain.model.Owner) = cancelCallUseCase(groupId, owner)

    suspend fun switchAudioToVideoCall(groupId: Int, owner: com.clearkeep.domain.model.Owner) = switchAudioToVideoCallUseCase(groupId, owner)

    suspend fun getGroupById(groupId: Long, domain: String, ownerId: String) = getGroupByIdUseCase(groupId, domain, ownerId)

    suspend fun getDefaultServer() = getDefaultServerUseCase()
}