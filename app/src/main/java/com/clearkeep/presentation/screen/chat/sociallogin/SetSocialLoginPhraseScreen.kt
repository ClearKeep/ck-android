package com.clearkeep.presentation.screen.chat.sociallogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.clearkeep.R
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.presentation.screen.auth.login.LoginViewModel
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize

@Composable
fun SetSocialLoginPhraseScreen(
    viewModel: LoginViewModel,
    onBackPress: () -> Unit,
    onClickNext: () -> Unit
) {
    val securityPhrase = rememberSaveable {mutableStateOf("") }
    val isSecurityPhraseValid = viewModel.isSecurityPhraseValid.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(
            title = stringResource(R.string.social_login_phrase_screen_title)
        ) { onBackPress() }
        Spacer(Modifier.height(30.sdp()))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.sdp())
                .fillMaxSize(),
        ) {
            CKText(
                text = stringResource(R.string.social_pin_description),
                style = MaterialTheme.typography.caption,
                color = LocalColorMapping.current.topAppBarContent,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(32.sdp()))
            CKTextInputField(
                stringResource(R.string.security_phrase),
                securityPhrase, keyboardType = KeyboardType.Password,
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
            Spacer(Modifier.height(12.sdp()))
            CKText(
                stringResource(R.string.set_social_pin_hint),
                fontSize = 12.sdp().toNonScalableTextSize(),
                color = LocalColorMapping.current.clickableBodyText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(50.sdp()))
            CKButton(
                stringResource(R.string.btn_next),
                onClick = {
                    onClickNext()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSecurityPhraseValid.value == true,
                buttonType = ButtonType.White
            )
        }
        Spacer(Modifier.height(30.sdp()))
    }
}