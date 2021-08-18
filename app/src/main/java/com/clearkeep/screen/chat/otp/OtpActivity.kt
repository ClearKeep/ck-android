package com.clearkeep.screen.chat.otp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import com.clearkeep.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OtpActivity: AppCompatActivity(), LifecycleObserver {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val otpViewModel: OtpViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        setContent {
            CKSimpleTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "otp_verify") {
                    composable("otp_verify") {
                        OtpVerifyPasswordScreen(
                            otpViewModel,
                            onBackPress = {
                                finish()
                            },
                            onClickNext = {
                                navController.navigate("enter_otp")
                            }
                        )
                    }
                    composable("enter_otp") {
                        EnterOtpScreen(
                            otpViewModel.verifyOtpResponse,
                            onDismissMessage = { otpViewModel.verifyOtpResponse.value = null },
                            onClickResend = { otpViewModel.requestResendOtp() },
                            onClickSubmit = { otpViewModel.verifyOtp(it) },
                        onBackPress = {
                            finish()
                        }, onClickSave = {
                            finish()
                        })
                    }
                }
            }
        }
    }
}