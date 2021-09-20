package com.clearkeep.screen.chat.social_login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.screen.auth.login.LoginViewModel
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun EnterSocialLoginPhraseScreen(
    viewModel: LoginViewModel,
    onBackPress: () -> Unit,
    onVerifySuccess: () -> Unit,
    onConfirmForgotPassphrase: () -> Unit
) {
    val securityPhrase = remember { mutableStateOf("") }
    val isSecurityPhraseValid = viewModel.isSecurityPhraseValid.observeAsState()
    val isConfirmForgotPassphraseDialogVisible = remember { mutableStateOf(false) }
    val verifyResponse = viewModel.verifyPassphraseResponse.observeAsState()

    val error = if (verifyResponse.value?.status == Status.ERROR) {
        "Security phrase is incorrect. Please try again"
    } else  {
        null
    }

    if (verifyResponse.value?.status == Status.SUCCESS) {
        onVerifySuccess()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(title = "Enter Your Security Phrase", onBackPressed = { onBackPress() })
        Spacer(Modifier.height(30.sdp()))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.sdp())
                .fillMaxSize(),
        ) {
            CKText(
                text = "Please enter your security phrase",
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(32.sdp()))
            CKTextInputField(
                "Security Phrase",
                securityPhrase, singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null
                    )
                },
                keyboardType = KeyboardType.Password,
                error = error
            ) {
                viewModel.setSecurityPhrase(it)
            }
            Spacer(Modifier.height(54.sdp()))
            CKText(
                "Forgot passphrase?",
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { isConfirmForgotPassphraseDialogVisible.value = true },
                color = grayscaleOffWhite,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(22.sdp()))
            CKButton(
                stringResource(R.string.verify),
                onClick = {
                     viewModel.verifySocialPin()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSecurityPhraseValid.value == true,
                buttonType = ButtonType.White
            )

            if (isConfirmForgotPassphraseDialogVisible.value) {
                CKAlertDialog(
                    title = stringResource(R.string.warning),
                    text = "Resetting your PIN will reset all your data.",
                    confirmTitle = "Reset",
                    dismissTitle = "Cancel",
                    onDismissButtonClick = {
                        isConfirmForgotPassphraseDialogVisible.value = false
                    },
                    onConfirmButtonClick = {
                        onConfirmForgotPassphrase()
                    }
                )
            }
        }
    }
}