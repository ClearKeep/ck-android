package com.clearkeep.screen.auth.forgot

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.lightThemeColors
import com.clearkeep.utilities.network.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class ForgotActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val forgotViewModel: ForgotViewModel by viewModels {
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

        val onForgotPressed: (String) -> Unit = { email ->
            lifecycleScope.launch {
                val res = forgotViewModel.recoverPassword(email)
                if (res.status == Status.SUCCESS) {
                } else if (res.status == Status.ERROR) {
                    setShowDialog(res.message ?: "unknown")
                }
            }
        }
        Box() {
            Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
            ) {
                Row(/*modifier = Modifier.weight(1.0f, true)*/) {
                    ForgotScreen(
                            onForgotPressed = onForgotPressed,
                            onBackPress = {
                                finish()
                            }
                    )
                }
            }
            ErrorDialog(showDialog, setShowDialog)
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
}





