package com.clearkeep.presentation.screen.chat.social_login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.base.*
import com.clearkeep.presentation.components.grayscaleOffWhite
import com.clearkeep.presentation.screen.auth.login.LoginViewModel
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun ConfirmSocialLoginPhraseScreen(
    viewModel: LoginViewModel,
    onBackPress: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val securityPhrase = rememberSaveable {mutableStateOf("") }
    val isSecurityPhraseValid = viewModel.isConfirmSecurityPhraseValid.observeAsState()
    val registerResponse = viewModel.registerSocialPinResponse.observeAsState()
    val isLoading = viewModel.isLoading.observeAsState()

    BackHandler {
        onBackPress()
    }

    if (registerResponse.value?.status == Status.SUCCESS) {
        onLoginSuccess()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(58.sdp()))
            CKTopAppBarSample(
                title = stringResource(R.string.confirm_social_pin_screen_title)
            ) { onBackPress() }
            Spacer(Modifier.height(30.sdp()))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.sdp())
                    .fillMaxSize(),
            ) {
                CKText(
                    text = stringResource(R.string.social_pin_reminder),
                    style = MaterialTheme.typography.caption,
                    color = grayscaleOffWhite,
                    fontSize = 16.sdp().toNonScalableTextSize()
                )
                Spacer(Modifier.height(32.sdp()))
                CKTextInputField(
                    stringResource(R.string.confirm_social_pin),
                    securityPhrase, keyboardType = KeyboardType.Password,
                    error = if (isSecurityPhraseValid.value == true || securityPhrase.value.isBlank()) null else "Security phrase and confirm security phrase do not match. Please try again",
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null,
                            colorFilter = LocalColorMapping.current.textFieldIconFilter
                        )
                    }
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
                    enabled = isSecurityPhraseValid.value == true && isLoading.value == false,
                    buttonType = ButtonType.White
                )
            }
            Spacer(Modifier.height(58.sdp()))
        }

        if (isLoading.value == true) {
            CKCircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}