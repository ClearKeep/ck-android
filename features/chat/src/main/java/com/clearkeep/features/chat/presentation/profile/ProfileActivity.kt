package com.clearkeep.features.chat.presentation.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.map
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.common.presentation.components.CKSimpleTheme
import com.clearkeep.features.chat.presentation.changepassword.ChangePasswordActivity
import com.clearkeep.features.chat.presentation.home.HomeViewModel
import com.clearkeep.features.chat.presentation.otp.OtpActivity
import com.clearkeep.features.chat.presentation.room.imagepicker.ImagePickerScreen
import com.clearkeep.navigation.NavigationUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity(), LifecycleObserver {
    private val profileViewModel: ProfileViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        profileViewModel.getMfaDetail()

        setContent {
            CKSimpleTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "profile") {
                    composable("profile") {
                        MainComposable(navController)
                    }

                    composable("country_code_picker") {
                        CountryCodePicker(onPick = {
                            profileViewModel.setCountryCode(it)
                            navController.popBackStack()
                        }) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }

    @ExperimentalMaterialApi

    @Composable
    private fun MainComposable(navController: NavController) {
        ProfileScreen(
            navController,
            profileViewModel = profileViewModel,
            onCloseView = {
                if (!profileViewModel.hasUnsavedChanges()) {
                    finish()
                }
            },
            onChangePassword = {
                navigateToChangePassword()
            },
            onCopyToClipBoard = {
                copyProfileLinkToClipBoard("profile link", profileViewModel.getProfileLink())
            },
            onNavigateToOtp = {
                navigateToOtp()
            },
            onDeleteUser = {
                profileViewModel.deleteUser()
                profileViewModel.deleteUserSuccess.observe(this) {
                    if (it) profileViewModel.signOut()
                }
                profileViewModel.shouldReLogin.observe(this) {
                    if (it) NavigationUtils.navigateToSplashActivity(this)
                }
            }
        )
    }

    private fun navigateToChangePassword() {
        val intent = Intent(this, ChangePasswordActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToOtp() {
        val intent = Intent(this, OtpActivity::class.java)
        startActivity(intent)
    }

    private fun copyProfileLinkToClipBoard(label: String, text: String) {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(applicationContext, "You copied", Toast.LENGTH_SHORT).show()
    }
}