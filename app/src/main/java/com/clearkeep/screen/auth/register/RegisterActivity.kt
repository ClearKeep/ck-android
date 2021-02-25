package com.clearkeep.screen.auth.register

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.components.lightThemeColors
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }

    @Composable
    fun MyApp() {
        MaterialTheme(
                colors = lightThemeColors
        ) {
            AppContent()
        }
    }

    @Composable
    fun AppContent() {
        val (showDialog, setShowDialog) = remember { mutableStateOf("") }
        val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

        val onRegisterPressed: (String, String, String) -> Unit = { userName, password, email ->
            lifecycleScope.launch {
                val res = registerViewModel.register(this@RegisterActivity, userName, password, email)
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
                        CKCircularProgressIndicator(
                                color = Color.Blue
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorDialog(showDialog: String, setShowDialog: (String) -> Unit) {
        if (showDialog.isNotEmpty()) {
            CKAlertDialog(
                    title = {
                        Text("Error")
                    },
                    text = {
                        Text(showDialog)
                    },
                    dismissButton = {
                        Button(
                                onClick = {
                                    // Change the state to close the dialog
                                    setShowDialog("")
                                },
                        ) {
                            Text("OK")
                        }
                    },
            )
        }
    }

    @Composable
    fun ReminderDialog(showReminder: Boolean) {
        if (showReminder) {
            CKAlertDialog(
                    title = {
                        Text("Register successfully")
                    },
                    text = {
                        Text("Please check your email to activate account")
                    },
                    dismissButton = {
                        Button(
                                onClick = {
                                    finish()
                                },
                        ) {
                            Text("OK")
                        }
                    },
            )
        }
    }
}