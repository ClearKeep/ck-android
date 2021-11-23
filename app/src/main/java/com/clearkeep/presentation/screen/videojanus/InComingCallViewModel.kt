package com.clearkeep.presentation.screen.videojanus

import androidx.lifecycle.ViewModel
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.usecase.call.CancelCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InComingCallViewModel @Inject constructor(
    private val cancelCallUseCase: CancelCallUseCase
): ViewModel() {
    suspend fun cancelCall(groupId: Int, owner: com.clearkeep.domain.model.Owner) {
        cancelCallUseCase(groupId, owner)
    }
}