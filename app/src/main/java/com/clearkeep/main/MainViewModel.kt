package com.clearkeep.main

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.clearkeep.auth.AuthRepository

class MainViewModel @ViewModelInject constructor(
    loginRepository: AuthRepository
): ViewModel() {

    val isUserRegistered = loginRepository.isUserRegistered()
}
