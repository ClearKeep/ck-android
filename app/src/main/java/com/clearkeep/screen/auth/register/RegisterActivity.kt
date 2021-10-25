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
class RegisterActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val registerViewModel: RegisterViewModel by viewModels {
        viewModelFactory
    }

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val domain = intent.getStringExtra(DOMAIN)
        if (domain.isNullOrBlank()) {
            throw IllegalArgumentException("domain must be not null for register account")
        }
        registerViewModel.domain = domain

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
        MainContent()
    }

    @Composable
    fun MainContent() {
        val (showDialog, setShowDialog) = remember { mutableStateOf(0 to "") }
        val (showReminder, setShowReminderDialog) = remember { mutableStateOf(false) }

        val onRegisterPressed: (String, String, String, String) -> Unit = { email, userName, password, confirmPass ->
            lifecycleScope.launch {
                val res = registerViewModel.register(this@RegisterActivity, email, userName, password, confirmPass)
                    ?: return@launch
                if (res.status == Status.SUCCESS) {
                    setShowReminderDialog(true)
                } else if (res.status == Status.ERROR) {
                    setShowDialog(res.errorCode to (res.message ?: "unknown"))
                }
            }
        }
        val isLoadingState = registerViewModel.isLoading.observeAsState()
        Box {
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
                        CKCircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorDialog(showDialog: Pair<Int, String>, setShowDialog: (Pair<Int, String>) -> Unit) {
        if (showDialog.second.isNotBlank() || showDialog.first != 0) {
            val title = if (showDialog.first == ERROR_CODE_TIMEOUT) stringResource(R.string.network_error_dialog_title) else stringResource(R.string.error)
            CKAlertDialog(
                title = title,
                text = showDialog.second,
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
                title = stringResource(R.string.register_success_title),
                text = stringResource(R.string.register_success_text),
                onDismissButtonClick = {
                    finish()
                },
            )
        }
    }

    companion object {
        const val DOMAIN = "domain"
    }
}