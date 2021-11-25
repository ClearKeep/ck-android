package com.clearkeep.presentation.screen.chat.otp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.common.presentation.components.CKSimpleTheme
import com.clearkeep.common.utilities.printlnCK
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OtpActivity : AppCompatActivity(), LifecycleObserver {
    private val otpViewModel: OtpViewModel by viewModels()

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        printlnCK("OtpActivity onCreate")

        setContent {
            CKSimpleTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "password_verify") {
                    composable("password_verify") {
                        OtpVerifyPasswordScreen(
                            otpViewModel,
                            onBackPress = {
                                finish()
                            },
                            onClickNext = {
                                printlnCK("password_verify navigate enter OTP")
                                navController.navigate("enter_otp")
                            }
                        )
                    }
                    composable("enter_otp") {
                        EnterOtpScreen(
                            otpViewModel.verifyOtpResponse,
                            onDismissMessage = {
                                otpViewModel.verifyOtpResponse.value = null

                                if (otpViewModel.isAccountLocked.value == true) {
                                    otpViewModel.resetAccountLock()
                                    finish()
                                }
                            },
                            onClickResend = { otpViewModel.requestResendOtp() },
                            onClickSubmit = { otpViewModel.verifyOtp(it) },
                            onBackPress = { finish() },
                            onClickSave = { finish() },
                        )
                    }
                }
            }
        }
    }
}