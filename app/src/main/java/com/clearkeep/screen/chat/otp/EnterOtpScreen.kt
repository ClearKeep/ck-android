package com.clearkeep.screen.chat.otp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.clearkeep.components.base.ButtonType
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTopAppBarSample
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status

@Composable
fun EnterOtpScreen(otpResponse: MutableLiveData<Resource<String>>, onDismissMessage: () -> Unit, onClickResend: () -> Unit, onClickSubmit: (String) -> Unit, onBackPress: () -> Unit, onClickSave: () -> Unit) {
    val input = remember { mutableStateListOf(" ", " ", " ", " ") }
    val verifyOtpResponse = otpResponse.observeAsState()

    BackHandler {
        onBackPress.invoke()
    }

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
            title = "Enter Your Code", onBackPressed = {
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
            OtpInput(input)
            Spacer(Modifier.height(32.dp))
            Text("Don't get the code?", Modifier.align(Alignment.CenterHorizontally), Color.White, 16.sp, fontWeight = FontWeight.W400)
            Text("Resend code", Modifier.align(Alignment.CenterHorizontally).clickable { onClickResend.invoke() }, Color.White, 16.sp)
            Spacer(Modifier.height(24.dp))
            CKButton(
                stringResource(R.string.verify),
                onClick = {
                    onClickSubmit.invoke(input.joinToString(""))
                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White,
                enabled = input.joinToString("").isNotBlank()
            )
        }

        when (verifyOtpResponse.value?.status) {
            Status.ERROR -> {
                CKAlertDialog(
                    title = stringResource(R.string.warning),
                    text = verifyOtpResponse.value!!.message ?: "",
                    onDismissButtonClick = {
                        onDismissMessage.invoke()
                    },
                    dismissTitle = stringResource(R.string.close)
                )
            }
            Status.SUCCESS -> {
                onClickSave()
            }
            else -> {

            }
        }
    }
}

@Composable
fun OtpInput(input: SnapshotStateList<String>) {
    val focusRequesters = remember { mutableStateListOf(FocusRequester(), FocusRequester(), FocusRequester(), FocusRequester()) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OtpInputSquare(input[0], focusRequesters[0]) {
            if (it.length <= 1) {
                input[0] = it
                if (it.isNotEmpty()) {
                    focusRequesters[1].requestFocus()
                } else {
                    input[0] = " "
                }
            } else if (it.length == 2) {
                //Handle delete and type case
                input[1] = it[1].toString()
                focusRequesters[1].requestFocus()
            } else if (isValidOtp(it)) {
                //Handle pasted OTP
                it.forEachIndexed { index: Int, c : Char ->
                    input[index] = it[index].toString()
                }
            }
        }
        OtpInputSquare(input[1], focusRequesters[1]) {
            if (it.length <= 1) {
                input[1] = it
                if (it.isNotEmpty()) {
                    focusRequesters[2].requestFocus()
                } else {
                    input[1] = " "
                    focusRequesters[0].requestFocus()
               }
            } else if (it.length == 2) {
                //Handle delete and type case
                input[2] = it[1].toString()
                focusRequesters[2].requestFocus()
            }
        }
        OtpInputSquare(input[2], focusRequesters[2]) {
            if (it.length <= 1) {
                input[2] = it
                if (it.isNotEmpty()) {
                    focusRequesters[3].requestFocus()
                } else {
                    input[2] = " "
                    focusRequesters[1].requestFocus()
                }
            } else if (it.length == 2) {
                //Handle delete and type case
                input[3] = it[1].toString()
                focusRequesters[3].requestFocus()
            }
        }
        OtpInputSquare(input[3], focusRequesters[3]) {
            if (it.length <= 1) {
                input[3] = it
                if (it.isEmpty()) {
                    input[3] = " "
                    focusRequesters[2].requestFocus()
                }
            }
        }
    }
}

@Composable
fun OtpInputSquare(value: String, focusRequester: FocusRequester, onValueChange: (String) -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        Box(Modifier.size(28.dp).align(Alignment.Center)) {
            BasicTextField(
                value,
                onValueChange = { onValueChange(it.trim()) },
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
        }
    }
}

private fun isValidOtp(otp: String) : Boolean {
    if (!(otp.isNotBlank() && otp.length <= 4))
        return false
    otp.forEach {
        if (!it.isDigit()) {
            return false
        }
    }
    return true
}