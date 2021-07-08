package com.clearkeep.screen.splash

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.utilities.printlnCK

class SplashViewModel @ViewModelInject constructor(
    private val serverRepository: ServerRepository,
    private val environment: Environment
): ViewModel() {

    suspend fun isUserRegistered() : Boolean {
        return serverRepository.getDefaultServer() != null
    }

    suspend fun setupEnvironment() {
        val selectedServer = serverRepository.getDefaultServer()
        if (selectedServer == null) {
            printlnCK("default server must be not NULL")
            throw IllegalArgumentException("default server must be not NULL")
        }
        environment.setUpDomain(selectedServer)
        serverRepository.setActiveServer(selectedServer)
    }
}
