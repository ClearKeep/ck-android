package com.clearkeep.screen.chat.profile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.clearkeep.presentation.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.screen.chat.change_pass_word.ChangePasswordActivity
import com.clearkeep.screen.chat.otp.OtpActivity
import com.clearkeep.screen.chat.room.image_picker.ImagePickerScreen

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity(), LifecycleObserver {
    private val profileViewModel: ProfileViewModel by viewModels()

    @ExperimentalMaterialApi
        @ExperimentalFoundationApi
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
                    composable("pick_avatar") {
                        ImagePickerScreen(
                            profileViewModel.imageUriSelected.map { listOf(it) },
                            navController,
                            onlyPickOne = true,
                            insetEnabled = false,
                            onSetSelectedImages = {
                                if (it.isNotEmpty()) {
                                    profileViewModel.setSelectedImage(it[0])
                                }
                            })
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