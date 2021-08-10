package com.clearkeep.screen.auth.forgot

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
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

        val domain = intent.getStringExtra(EXTRA_DOMAIN)
        forgotViewModel.setDomain(domain ?: "")

        setContent {
            MyApp()
        }
    }

    @Composable
    fun MyApp() {
        CKTheme {
            AppContent()
        }
    }

    @Composable
    fun AppContent() {
        val (showDialog, setShowDialog) = remember { mutableStateOf("") }
        val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

        val onForgotPressed: (String) -> Unit = { email ->
            lifecycleScope.launch {
                val res = forgotViewModel.recoverPassword(email)
                if (res.status == Status.SUCCESS) {
                    setShowReminderDialog(true)
                } else if (res.status == Status.ERROR) {
                    setShowDialog(res.message ?: "unknown")
                }
            }
        }
        val isLoadingState = forgotViewModel.isLoading.observeAsState()

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
                            },
                            isLoading = isLoadingState.value ?: false
                    )
                }
            }
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
            ReminderDialog(showReminder)
            ErrorDialog(showDialog, setShowDialog)
        }
    }

    @Composable
    fun ErrorDialog(showDialog: String, setShowDialog: (String) -> Unit) {
        if (showDialog.isNotEmpty()) {
            CKAlertDialog(
                title = "Error",
                text = showDialog,
                onDismissButtonClick = {
                    setShowDialog("")
                },
            )
        }
    }

    @Composable
    fun ReminderDialog(showReminder: Boolean) {
        if (showReminder) {
            CKAlertDialog(
                title = "A verification link has been sent to ",
                text = "To complete reset, please open verification link in your email.",
                onDismissButtonClick = {
                    finish()
                },
            )
        }
    }

    companion object {
        const val EXTRA_DOMAIN = "domain"
    }
}





