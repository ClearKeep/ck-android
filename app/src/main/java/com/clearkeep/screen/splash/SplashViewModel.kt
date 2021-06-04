package com.clearkeep.screen.splash

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.clearkeep.utilities.UserManager

class SplashViewModel @ViewModelInject constructor(
    userManager: UserManager
): ViewModel() {

    val isUserRegistered = userManager.isUserLoginAlready()
}
