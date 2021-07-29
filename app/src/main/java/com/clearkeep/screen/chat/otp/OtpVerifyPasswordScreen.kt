package com.clearkeep.screen.chat.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.clearkeep.components.base.ButtonType
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextInputField
import com.clearkeep.components.base.CKTopAppBarSample
import com.clearkeep.components.grayscaleOffWhite

@Composable
fun OtpVerifyPasswordScreen(
    onBackPress: () -> Unit,
    onClickNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        //todo disable dark mode
                        if (isSystemInDarkTheme()) backgroundGradientStart else backgroundGradientStart,
                        if (isSystemInDarkTheme()) backgroundGradientEnd else backgroundGradientEnd
                    )
                )
            )
    ) {
        val currentPassWord = remember { mutableStateOf("") }

        Spacer(Modifier.height(58.dp))
        CKTopAppBarSample(modifier = Modifier.padding(start = 6.dp),
            title = "Enter your OTP", onBackPressed = {
                onBackPress.invoke()
            })
        Spacer(Modifier.height(30.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Please enter your password to enable OTP",
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            CKTextInputField(
                placeholder = "Current Password",
                textValue = currentPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null
                    )
                },
            )
            Spacer(Modifier.height(24.dp))
            CKButton(
                stringResource(R.string.btn_next),
                onClick = {
                    onClickNext()
                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White
            )
        }
    }
}