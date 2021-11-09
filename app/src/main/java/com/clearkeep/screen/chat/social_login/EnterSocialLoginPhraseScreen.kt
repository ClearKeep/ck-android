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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.components.LocalColorMapping
import com.clearkeep.components.base.*
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
    val securityPhrase = rememberSaveable {mutableStateOf("") }
    val isSecurityPhraseValid = viewModel.isSecurityPhraseValid.observeAsState()
    val isConfirmForgotPassphraseDialogVisible = rememberSaveable {mutableStateOf(false) }
    val verifyResponse = viewModel.verifyPassphraseResponse.observeAsState()
    val isLoading = viewModel.isLoading.observeAsState()

    val error = if (verifyResponse.value?.status == Status.ERROR) {
        stringResource(R.string.social_pin_incorrect)
    } else {
        null
    }

    if (verifyResponse.value?.status == Status.SUCCESS) {
        onVerifySuccess()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(58.sdp()))
            CKTopAppBarSample(
                title = stringResource(R.string.enter_social_pin_screen_title)
            ) { onBackPress() }
            Spacer(Modifier.height(30.sdp()))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.sdp())
                    .fillMaxSize(),
            ) {
                CKText(
                    text = stringResource(R.string.enter_social_pin),
                    style = MaterialTheme.typography.caption,
                    color = LocalColorMapping.current.topAppBarContent,
                    fontSize = 16.sdp().toNonScalableTextSize()
                )
                Spacer(Modifier.height(32.sdp()))
                CKTextInputField(
                    stringResource(R.string.security_phrase),
                    securityPhrase, keyboardType = KeyboardType.Password,
                    error = error,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null,
                            colorFilter = LocalColorMapping.current.textFieldIconFilter
                        )
                    }
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
                    color = LocalColorMapping.current.clickableBodyText,
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
                        text = stringResource(R.string.reset_social_login_pin_warning),
                        confirmTitle = stringResource(R.string.reset),
                        dismissTitle = stringResource(R.string.cancel),
                        onDismissButtonClick = {
                            isConfirmForgotPassphraseDialogVisible.value =
                                false && isLoading.value == false
                        },
                        onConfirmButtonClick = {
                            onConfirmForgotPassphrase()
                        }
                    )
                }
            }
            Spacer(Modifier.height(58.sdp()))
        }

        if (isLoading.value == true) {
            CKCircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}