package com.clearkeep.presentation.screen.chat.changepassword

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.common.presentation.components.*
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.features.splash.presentation.SplashActivity
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    onBackPress: () -> Unit
) {
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
        val context = LocalContext.current

        val (currentPasswordField, newPasswordField, confirmPasswordField) = remember {
            FocusRequester.createRefs()
        }

        val currentPassWord = rememberSaveable { mutableStateOf("") }
        val newPassWord = rememberSaveable { mutableStateOf("") }
        val confirmPassWord = rememberSaveable { mutableStateOf("") }

        val currentPassWordError = viewModel.oldPasswordError.observeAsState()
        val newPassWordError = viewModel.newPasswordError.observeAsState()
        val confirmPassWordError = viewModel.newPasswordConfirmError.observeAsState()
        val changePasswordResponse = viewModel.changePasswordResponse.observeAsState()
        val isResetPassword = viewModel.isResetPassword.observeAsState()

        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(
            modifier = Modifier.padding(start = 6.sdp()),
            title = stringResource(R.string.enter_new_password)
        ) {
            onBackPress.invoke()
        }
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
                color = LocalColorMapping.current.topAppBarContent,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(16.sdp()))
            if (isResetPassword.value != true) {
                CKTextInputField(
                    placeholder = stringResource(R.string.current_password),
                    textValue = currentPassWord,
                    modifier = Modifier.focusRequester(currentPasswordField),
                    keyboardType = KeyboardType.Password,
                    error = currentPassWordError.value,
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    onNext = {
                        newPasswordField.requestFocus()
                    },
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null,
                            Modifier.size(dimensionResource(R.dimen._24sdp)),
                            contentScale = ContentScale.FillBounds,
                            colorFilter = LocalColorMapping.current.textFieldIconFilter
                        )
                    }
                ) { viewModel.setOldPassword(it) }
                Spacer(Modifier.height(24.sdp()))
            }
            CKTextInputField(
                placeholder = stringResource(R.string.new_password),
                textValue = newPassWord,
                modifier = Modifier.focusRequester(newPasswordField),
                keyboardType = KeyboardType.Password,
                error = newPassWordError.value,
                singleLine = true,
                imeAction = ImeAction.Next,
                onNext = {
                    confirmPasswordField.requestFocus()
                },
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = LocalColorMapping.current.textFieldIconFilter
                    )
                }
            ) { viewModel.setNewPassword(it) }
            Spacer(Modifier.height(24.sdp()))
            CKTextInputField(
                placeholder = stringResource(R.string.confirm_password),
                textValue = confirmPassWord,
                modifier = Modifier.focusRequester(confirmPasswordField),
                keyboardType = KeyboardType.Password,
                error = confirmPassWordError.value,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = LocalColorMapping.current.textFieldIconFilter
                    )
                },
                onValueChange = { viewModel.setNewPasswordConfirm(it) },
            )
            Spacer(Modifier.height(24.sdp()))
            val currentPasswordValid =
                if (isResetPassword.value == true) true else currentPassWordError.value.isNullOrBlank() && currentPassWord.value.isNotBlank()
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

            if (changePasswordResponse.value?.status == com.clearkeep.common.utilities.network.Status.SUCCESS) {
                CKAlertDialog(
                    title = stringResource(R.string.password_change_success_title),
                    text = stringResource(R.string.password_change_success_text),
                    onDismissButtonClick = {
                        if (isResetPassword.value == true) {
                            val intent = Intent(context, com.clearkeep.features.splash.presentation.SplashActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                            (context as AppCompatActivity).finish()
                        }

                        onBackPress()
                    }
                )
            } else if (changePasswordResponse.value?.status == com.clearkeep.common.utilities.network.Status.ERROR) {
                CKAlertDialog(
                    title = stringResource(R.string.error),
                    text = changePasswordResponse.value?.message ?: "",
                    onDismissButtonClick = {
                        viewModel.changePasswordResponse.value = null
                    }
                )
            }
            Spacer(Modifier.height(58.sdp()))
        }
    }
}