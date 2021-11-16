package com.clearkeep.screen.chat.otp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.MutableLiveData
import com.clearkeep.R
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.base.ButtonType
import com.clearkeep.presentation.components.base.CKAlertDialog
import com.clearkeep.presentation.components.base.CKButton
import com.clearkeep.presentation.components.base.CKTopAppBarSample
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun EnterOtpScreen(
    otpResponse: MutableLiveData<Resource<String>>,
    onDismissMessage: () -> Unit,
    onClickResend: () -> Unit,
    onClickSubmit: (String) -> Unit,
    onBackPress: () -> Unit,
    onClickSave: () -> Unit,
) {
    printlnCK("EnterOtpScreen recompose")
    val input = rememberSaveable { mutableStateListOf(" ", " ", " ", " ") }
    val verifyOtpResponse = otpResponse.observeAsState()

    BackHandler {
        onBackPress.invoke()
    }

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
        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(
            modifier = Modifier.padding(start = 6.sdp()),
            title = stringResource(R.string.enter_otp_screen_title)
        ) {
            onBackPress.invoke()
        }
        Spacer(Modifier.height(30.sdp()))
        Column(Modifier.padding(horizontal = 16.sdp())) {
            Text(
                text = stringResource(R.string.otp_hint),
                style = MaterialTheme.typography.caption,
                color = LocalColorMapping.current.topAppBarContent,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(16.sdp()))
            OtpInput(input)
            Spacer(Modifier.height(32.sdp()))
            Text(
                stringResource(R.string.enter_otp_no_code),
                Modifier.align(Alignment.CenterHorizontally),
                LocalColorMapping.current.bodyText,
                16.sdp().toNonScalableTextSize(),
                fontWeight = FontWeight.W400
            )
            Text(
                stringResource(R.string.enter_otp_resend),
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onClickResend.invoke() },
                LocalColorMapping.current.clickableBodyText,
                16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(24.sdp()))
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
                input[0] = " "
                input[1] = " "
                input[2] = " "
                input[3] = " "

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
        Spacer(Modifier.height(58.sdp()))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpInput(input: SnapshotStateList<String>) {
    val (otpInput0, otpInput1, otpInput2, otpInput3) = FocusRequester.createRefs()

    LaunchedEffect(true) {
        otpInput0.requestFocus()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.sdp()),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OtpInputSquare(input[0], otpInput0) {
            if (it.length <= 1) {
                input[0] = it
                if (it.isNotEmpty()) {
                    otpInput1.requestFocus()
                } else {
                    input[0] = " "
                }
            } else if (it.length == 2) {
                //Handle delete and type case
                input[1] = it[1].toString()
                otpInput1.requestFocus()
            } else if (isValidOtp(it)) {
                //Handle pasted OTP
                it.forEachIndexed { index: Int, _: Char ->
                    input[index] = it[index].toString()
                }
            }
        }
        OtpInputSquare(input[1], otpInput1) {
            if (it.length <= 1) {
                input[1] = it
                if (it.isNotEmpty()) {
                    otpInput2.requestFocus()
                } else {
                    input[1] = " "
                    otpInput0.requestFocus()
                }
            } else if (it.length == 2) {
                //Handle delete and type case
                input[2] = it[1].toString()
                otpInput2.requestFocus()
            }
        }
        OtpInputSquare(input[2], otpInput2) {
            if (it.length <= 1) {
                input[2] = it
                if (it.isNotEmpty()) {
                    otpInput3.requestFocus()
                } else {
                    input[2] = " "
                    otpInput1.requestFocus()
                }
            } else if (it.length == 2) {
                //Handle delete and type case
                input[3] = it[1].toString()
                otpInput3.requestFocus()
            }
        }
        OtpInputSquare(input[3], otpInput3, imeAction = ImeAction.Done) {
            if (it.length <= 1) {
                input[3] = it
                if (it.isEmpty()) {
                    input[3] = " "
                    otpInput2.requestFocus()
                }
            }
        }
    }
}

@Composable
fun OtpInputSquare(
    value: String,
    focusRequester: FocusRequester,
    imeAction: ImeAction = ImeAction.Next,
    onValueChange: (String) -> Unit
) {
    val localFocusManager = LocalFocusManager.current

    Box(
        Modifier
            .size(56.sdp())
            .background(Color.White, RoundedCornerShape(8.sdp()))
    ) {
        Box(
            Modifier
                .size(28.sdp())
                .align(Alignment.Center)
        ) {
            Row {
                Spacer(Modifier.width(8.sdp()))
                BasicTextField(
                    value,
                    onValueChange = { onValueChange(it.trim()) },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 20.sdp().toNonScalableTextSize()),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            localFocusManager.moveFocus(FocusDirection.Right)
                        },
                        onDone = {
                            localFocusManager.clearFocus()
                        }
                    )
                )
            }
        }
    }
}

private fun isValidOtp(otp: String): Boolean {
    if (!(otp.isNotBlank() && otp.length <= 4))
        return false
    otp.forEach {
        if (!it.isDigit()) {
            return false
        }
    }
    return true
}
