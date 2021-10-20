package com.clearkeep.screen.chat.change_pass_word

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.screen.chat.home.MainActivity
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.screen.splash.SplashActivity
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    onBackPress: (() -> Unit)
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
        val context = LocalContext.current

        val currentPassWord = remember { mutableStateOf("") }
        val newPassWord = remember { mutableStateOf("") }
        val confirmPassWord = remember { mutableStateOf("") }

        val currentPassWordError = viewModel.oldPasswordError.observeAsState()
        val newPassWordError = viewModel.newPasswordError.observeAsState()
        val confirmPassWordError = viewModel.newPasswordConfirmError.observeAsState()
        val changePasswordResponse = viewModel.changePasswordResponse.observeAsState()
        val isResetPassword = viewModel.isResetPassword.observeAsState()

        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(modifier = Modifier.padding(start = 6.sdp()),
            title = stringResource(R.string.enter_new_password), onBackPressed = {
                onBackPress.invoke()
            })
        Spacer(Modifier.height(26.sdp()))
        Column(
            modifier = Modifier
                .padding(horizontal = 16.sdp())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_detail_change_pass),
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(16.sdp()))
            if (isResetPassword.value != true) {
                CKTextInputField(
                    placeholder = "Current Password",
                    textValue = currentPassWord,
                    onValueChange = { viewModel.setOldPassword(it) },
                    keyboardType = KeyboardType.Password,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null,
                            Modifier.size(dimensionResource(R.dimen._24sdp)),
                            contentScale = ContentScale.FillBounds
                        )
                    },
                    error = currentPassWordError.value
                )
                Spacer(Modifier.height(24.sdp()))
            }
            CKTextInputField(
                placeholder = "New Password",
                textValue = newPassWord,
                keyboardType = KeyboardType.Password,
                onValueChange = { viewModel.setNewPassword(it) },
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        contentScale = ContentScale.FillBounds
                    )
                },
                error = newPassWordError.value
            )
            Spacer(Modifier.height(24.sdp()))
            CKTextInputField(
                placeholder = stringResource(R.string.confirm_password),
                textValue = confirmPassWord,
                onValueChange = { viewModel.setNewPasswordConfirm(it) },
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        contentScale = ContentScale.FillBounds
                    )
                },
                error = confirmPassWordError.value
            )
            Spacer(Modifier.height(24.sdp()))
            val currentPasswordValid = if (isResetPassword.value == true) true else currentPassWordError.value.isNullOrBlank() && currentPassWord.value.isNotBlank()
            val changePasswordButtonEnabled = currentPasswordValid
                    && newPassWordError.value.isNullOrBlank()
                    && confirmPassWordError.value.isNullOrBlank()
                    && newPassWord.value.isNotBlank()
                    && confirmPassWord.value.isNotBlank()
            CKButton(
                stringResource(R.string.save),
                onClick = {
                    viewModel.onClickConfirm()
                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White,
                enabled = changePasswordButtonEnabled
            )

            if (changePasswordResponse.value?.status == Status.SUCCESS) {
                CKAlertDialog(
                    title = "Password changed successfully",
                    text = "Please login again with new password!",
                    onDismissButtonClick = {
                        if (isResetPassword.value == true) {
                            val intent = Intent(context, SplashActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                            (context as AppCompatActivity).finish()
                        }

                        onBackPress()
                    }
                )
            } else if (changePasswordResponse.value?.status == Status.ERROR) {
                CKAlertDialog(
                    title = stringResource(R.string.error),
                    text = changePasswordResponse.value?.message ?: "",
                    onDismissButtonClick = {
                        viewModel.changePasswordResponse.value = null
                    }
                )
            }
        }
    }
}