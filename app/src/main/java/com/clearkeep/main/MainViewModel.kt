package com.clearkeep.main

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.clearkeep.login.LoginRepository

class MainViewModel @ViewModelInject constructor(
    loginRepository: LoginRepository
): ViewModel() {

    val isUserRegistered = loginRepository.isUserRegistered()
}
