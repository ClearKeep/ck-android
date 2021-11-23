package com.clearkeep.domain.usecase.server

import androidx.lifecycle.MutableLiveData
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.printlnCK
import javax.inject.Inject

class GetActiveServerUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    operator fun invoke(): MutableLiveData<Server> {
        return serverRepository.activeServer
    }
}