package com.clearkeep.screen.chat.social_login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun ConfirmSocialLoginPhraseScreen(viewModel: LoginViewModel, onBackPress: () -> Unit, onLoginSuccess: () -> Unit) {
    val securityPhrase = remember { mutableStateOf("") }
    val isSecurityPhraseValid = viewModel.isConfirmSecurityPhraseValid.observeAsState()
    val registerResponse = viewModel.registerSocialPinResponse.observeAsState()

    if (registerResponse.value?.status == Status.SUCCESS) {
        onLoginSuccess()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(title = "Confirm Your Security Phrase", onBackPressed = { onBackPress() })
        Spacer(Modifier.height(30.sdp()))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.sdp())
                .fillMaxSize(),
        ) {
            CKText(
                text = "Remember this security phrase. If you forget it, you’ll need to reset your account and all data will be erased.",
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(32.sdp()))
            CKTextInputField(
                "Confirm Security Phrase",
                securityPhrase, singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null
                    )
                },
                keyboardType = KeyboardType.Password,
                error = if (isSecurityPhraseValid.value == true || securityPhrase.value.isBlank()) null else "Security phrase and confirm security phrase do not match. Please try again"
            ) {
                viewModel.setConfirmSecurityPhrase(it)
            }
            Spacer(Modifier.height(104.sdp()))
            CKButton(
                stringResource(R.string.btn_next),
                onClick = {
                    viewModel.onSubmitNewPin()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSecurityPhraseValid.value == true,
                buttonType = ButtonType.White
            )

//            if (isEmptyEmail.value || isInvalidEmailFormat.value) {
//                CKAlertDialog(
//                    title = if (isEmptyEmail.value) stringResource(R.string.email_blank_error) else stringResource(
//                        R.string.email_invalid),
//                    text = stringResource(R.string.pls_check_again),
//                    onDismissButtonClick = {
//                        isEmptyEmail.value = false
//                        isInvalidEmailFormat.value = false
//                    }
//                )
//            }
        }
    }
}