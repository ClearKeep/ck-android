package com.clearkeep.screen.chat.otp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import com.clearkeep.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtpActivity: AppCompatActivity(), LifecycleObserver {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        setContent {
            CKSimpleTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "otp_verify") {
                    composable("otp_verify") {
                        OtpVerifyPasswordScreen(
                            onBackPress = {
                                finish()
                            },
                            onClickNext = {
                                navController.navigate("enter_otp")
                            }
                        )
                    }
                    composable("enter_otp") {
                        EnterOtpScreen(onBackPress = {
                            navController.popBackStack()
                        }, onClickSave = {
                            finish()
                        })
                    }
                }
            }
        }
    }
}