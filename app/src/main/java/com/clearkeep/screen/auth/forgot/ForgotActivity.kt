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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.clearkeep.R
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.utilities.ERROR_CODE_TIMEOUT
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
        val (showDialog, setShowDialog) = remember { mutableStateOf(0 to "") }
        val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }
        val emailState = rememberSaveable { mutableStateOf("") }

        val onForgotPressed: (String) -> Unit = { email ->
            lifecycleScope.launch {
                val res = forgotViewModel.recoverPassword(email)
                if (res.status == Status.SUCCESS) {
                    setShowReminderDialog(true)
                    emailState.value = email
                } else if (res.status == Status.ERROR) {
                    setShowDialog(res.errorCode to (res.message ?: "unknown"))
                }
            }
        }
        val isLoadingState = forgotViewModel.isLoading.observeAsState()

        Box {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row {
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
    fun ErrorDialog(showDialog: Pair<Int, String>, setShowDialog: (Pair<Int, String>) -> Unit) {
        if (showDialog.second.isNotBlank() || showDialog.first != 0) {
            val (title, text, dismissText) = if (showDialog.first == ERROR_CODE_TIMEOUT) {
                Triple(
                    stringResource(R.string.network_error_dialog_title),
                    stringResource(R.string.network_error_dialog_text),
                    stringResource(R.string.ok)
                )
            } else {
                Triple(
                    stringResource(R.string.error),
                    stringResource(R.string.reset_password_error_title),
                    stringResource(R.string.close)
                )
            }
            CKAlertDialog(
                title = title,
                text = text,
                dismissTitle = dismissText,
                onDismissButtonClick = {
                    // Change the state to close the dialog
                    setShowDialog(0 to "")
                },
            )
        }
    }

    @Composable
    fun ReminderDialog(showReminder: Boolean) {
        if (showReminder) {
            CKAlertDialog(
                title = stringResource(R.string.reset_password_success_title),
                text = stringResource(R.string.reset_password_success_content),
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





