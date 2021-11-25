package com.clearkeep.presentation.screen.chat.otp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize

@Composable
fun OtpVerifyPasswordScreen(
    otpViewModel: OtpViewModel,
    onBackPress: () -> Unit,
    onClickNext: () -> Unit
) {
    val verifyPasswordResponse = otpViewModel.verifyPasswordResponse.observeAsState()

    printlnCK("OtpVerifyPasswordScreen recompose verifyPasswordResponse $verifyPasswordResponse")

    Column(
        Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = LocalColorMapping.current.backgroundBrush
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        val currentPassWord = rememberSaveable { mutableStateOf("") }

        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(
            modifier = Modifier.padding(end = 8.sdp()),
            title = stringResource(R.string.otp_verify_password_title)
        ) {
            onBackPress.invoke()
        }
        Spacer(Modifier.height(30.sdp()))
        Column(Modifier.padding(horizontal = 16.sdp())) {
            Text(
                text = stringResource(R.string.otp_verify_password_description),
                style = MaterialTheme.typography.caption,
                color = LocalColorMapping.current.topAppBarContent,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(16.sdp()))
            CKTextInputField(
                placeholder = stringResource(R.string.current_password),
                textValue = currentPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        colorFilter = LocalColorMapping.current.textFieldIconFilter
                    )
                },
            )
            Spacer(Modifier.height(24.sdp()))
            CKButton(
                stringResource(R.string.btn_next),
                onClick = {
                    otpViewModel.verifyPassword(currentPassWord.value)
                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White,
                enabled = currentPassWord.value.isNotBlank()
            )
        }
        when (verifyPasswordResponse.value?.status) {
            Status.ERROR -> {
                CKAlertDialog(
                    title = verifyPasswordResponse.value!!.data?.first
                        ?: stringResource(R.string.error),
                    text = verifyPasswordResponse.value!!.data?.second
                        ?: stringResource(R.string.otp_verify_password_incorrect),
                    onDismissButtonClick = {
                        otpViewModel.verifyPasswordResponse.value = null
                    },
                    dismissTitle = stringResource(R.string.close)
                )
            }
            Status.SUCCESS -> {
                otpViewModel.verifyPasswordResponse.value = null
                onClickNext()
            }
            else -> {

            }
        }
        Spacer(Modifier.height(58.sdp()))
    }
}