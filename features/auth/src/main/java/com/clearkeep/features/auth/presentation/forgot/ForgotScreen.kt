package com.clearkeep.features.auth.presentation.forgot

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.common.presentation.components.grayscaleOffWhite
import com.clearkeep.common.utilities.isValidEmail
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize
import com.clearkeep.features.auth.R

@Composable
fun ForgotScreen(
    onForgotPressed: (email: String) -> Unit,
    onBackPress: () -> Unit,
    isLoading: Boolean = false
) {
    val email = rememberSaveable { mutableStateOf("") }
    val isEmptyEmail = rememberSaveable { mutableStateOf(false) }
    val isInvalidEmailFormat = rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(80.sdp()))
        CKTopAppBarSample(
            title = stringResource(R.string.forgot_password)
        ) { onBackPress() }
        Spacer(Modifier.height(26.sdp()))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.sdp())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_email_reset_pass),
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(16.sdp()))
            CKTextInputField(
                stringResource(R.string.tv_email),
                email, singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_mail),
                        contentDescription = null,
                        colorFilter = LocalColorMapping.current.textFieldIconFilter
                    )
                }
            )
            Spacer(Modifier.height(24.sdp()))
            CKButton(
                stringResource(R.string.btn_reset_password),
                onClick = {
                    if (email.value.isValidEmail()) {
                        onForgotPressed(email.value)
                    } else {
                        if (email.value.isBlank()) {
                            isEmptyEmail.value = true
                        } else {
                            isInvalidEmailFormat.value = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.value.isValidEmail(),
                buttonType = ButtonType.White
            )
            if (isEmptyEmail.value || isInvalidEmailFormat.value) {
                CKAlertDialog(
                    title = if (isEmptyEmail.value) stringResource(R.string.email_blank_error) else stringResource(
                        R.string.email_invalid
                    ),
                    text = stringResource(R.string.pls_check_again),
                    dismissTitle = stringResource(R.string.close),
                    onDismissButtonClick = {
                        isEmptyEmail.value = false
                        isInvalidEmailFormat.value = false
                    }
                )
            }
        }
        Spacer(Modifier.height(80.sdp()))
    }
}