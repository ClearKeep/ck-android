package com.clearkeep.screen.auth.register

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.auth.advance_setting.CustomServerScreen
import com.clearkeep.utilities.network.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val registerViewModel: RegisterViewModel by viewModels {
        viewModelFactory
    }

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }

    @ExperimentalAnimationApi
    @Composable
    fun MyApp() {
        CKTheme {
            AppContent()
        }
    }

    @ExperimentalAnimationApi
    @Composable
    fun AppContent() {
        val navController = rememberNavController()
        NavHost(navController, startDestination = "register"){
            composable("register"){
                MainContent(navController)
            }
            composable("advance_setting") {
                CustomServerScreen(
                    onBackPress = { isCustom, url ->
                        registerViewModel.isCustomServer = isCustom
                        registerViewModel.url = url
                        onBackPressed()
                    },
                    registerViewModel.isCustomServer, registerViewModel.url, registerViewModel.port
                )
            }
        }
    }

    @Composable
    fun MainContent(navController: NavHostController) {
        val (showDialog, setShowDialog) = remember { mutableStateOf("") }
        val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

        val onRegisterPressed: (String, String, String, String) -> Unit = { email, userName, password, confirmPass ->
            lifecycleScope.launch {
                val res = registerViewModel.register(this@RegisterActivity, email, userName, password, confirmPass)
                    ?: return@launch
                if (res.status == Status.SUCCESS) {
                    setShowReminderDialog(true)
                } else if (res.status == Status.ERROR) {
                    setShowDialog(res.message ?: "unknown")
                    /*showLoginError(res.message ?: "unknown")*/
                }
            }
        }
        val isLoadingState = registerViewModel.isLoading.observeAsState()
        Box() {
            RegisterScreen(
                registerViewModel,
                onRegisterPressed = onRegisterPressed,
                onBackPress = {
                    finish()
                },
                advanceSetting = {
                    navigateToAdvanceSetting(navController)
                },
                isLoading = isLoadingState.value ?: false
            )
            ErrorDialog(showDialog, setShowDialog)
            ReminderDialog(showReminder)
            isLoadingState.value?.let {
                if (it) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CKCircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorDialog(showDialog: String, setShowDialog: (String) -> Unit) {
        if (showDialog.isNotEmpty()) {
            CKAlertDialog(
                title = "Error",
                text = showDialog,
                onDismissButtonClick = {
                    // Change the state to close the dialog
                    setShowDialog("")
                },
            )
        }
    }

    @Composable
    fun ReminderDialog(showReminder: Boolean) {
        if (showReminder) {
            CKAlertDialog(
                title = "Register successfully",
                text = "Please check your email to activate account",
                onDismissButtonClick = {
                    finish()
                },
            )
        }
    }

    private fun navigateToAdvanceSetting(navController: NavController){
        navController.navigate("advance_setting")
    }
}