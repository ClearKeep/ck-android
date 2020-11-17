package com.clearkeep.screen.splash

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.clearkeep.screen.auth.AuthRepository

class SplashViewModel @ViewModelInject constructor(
    loginRepository: AuthRepository
): ViewModel() {

    val isUserRegistered = loginRepository.isUserRegistered()
}
