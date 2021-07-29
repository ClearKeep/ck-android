package com.clearkeep.screen.chat.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.clearkeep.components.base.ButtonType
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTopAppBarSample
import com.clearkeep.components.grayscaleOffWhite

@Composable
fun EnterOtpScreen(onBackPress: () -> Unit, onClickSave: () -> Unit) {
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
                text = "Please input a code that has been sent to your phone",
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            OtpInput()
            Spacer(Modifier.height(40.dp))
            Text("Don't get the code?", Modifier.align(Alignment.CenterHorizontally), Color.White, 16.sp)
            Text("Resend code", Modifier.align(Alignment.CenterHorizontally), Color.White, 16.sp, fontWeight = FontWeight.W700)
            Spacer(Modifier.height(24.dp))
            CKButton(
                stringResource(R.string.save),
                onClick = {

                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White
            )
        }
    }
}

@Composable
fun OtpInput() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OtpInputSquare()
        OtpInputSquare()
        OtpInputSquare()
        OtpInputSquare()
    }
}

@Composable
fun OtpInputSquare() {
    BasicTextField(
        "",
        onValueChange = { },
        modifier = Modifier
            .size(56.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
    )
}