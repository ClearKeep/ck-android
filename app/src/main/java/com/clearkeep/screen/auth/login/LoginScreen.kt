package com.clearkeep.screen.auth.login

import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearkeep.BuildConfig
import com.clearkeep.R
import com.clearkeep.presentation.components.LocalColorMapping
import com.clearkeep.presentation.components.base.*
import com.clearkeep.presentation.components.grayscaleOffWhite
import com.clearkeep.utilities.*


@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel,
    onLoginPressed: (email: String, password: String) -> Unit,
    onRegisterPress: () -> Unit,
    onForgotPasswordPress: () -> Unit,
    isLoading: Boolean = false,
    onLoginGoogle: (() -> Unit)? = null,
    onLoginMicrosoft: (() -> Unit)? = null,
    onLoginFacebook: (() -> Unit)? = null,
    advanceSetting: (() -> Unit)? = null,
    isShowAdvanceSetting: Boolean = true,
    isJoinServer: Boolean = false,
    onNavigateBack: (() -> Unit) = {}
) {
    val emailField = remember { FocusRequester() }
    val passwordField = remember { FocusRequester() }

    val email = rememberSaveable { mutableStateOf("") }
    val password = rememberSaveable { mutableStateOf("") }
    val confirmForgotPasswordVisible = rememberSaveable { mutableStateOf(false) }

    email.value = loginViewModel.email.observeAsState().value ?: ""

    val image = painterResource(R.drawable.ic_logo)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isJoinServer) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = dimensionResource(R.dimen._8sdp))
            ) {
                IconButton(onClick = { onNavigateBack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "",
                        tint = grayscaleOffWhite,
                    )
                }
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen._32sdp)))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Image(image, contentDescription = "")
        }

        ViewUsedCustomServer(loginViewModel.customDomain.isNotEmpty() && loginViewModel.isCustomServer)
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen._20sdp))) {
            CKTextInputField(
                stringResource(R.string.tv_email),
                email,
                modifier = Modifier.focusRequester(emailField),
                keyboardType = KeyboardType.Email,
                singleLine = true,
                imeAction = ImeAction.Next,
                onNext = {
                    passwordField.requestFocus()
                },
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_mail),
                        contentDescription = null,
                        colorFilter = LocalColorMapping.current.textFieldIconFilter
                    )
                },
                onValueChange = {
                    loginViewModel.setEmail(it)
                }
            )
            Spacer(Modifier.height(dimensionResource(R.dimen._24sdp)))
            CKTextInputField(
                stringResource(R.string.tv_password),
                password,
                modifier = Modifier.focusRequester(passwordField),
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

            Spacer(Modifier.height(dimensionResource(R.dimen._24sdp)))
            CKButton(
                stringResource(R.string.btn_login),
                onClick = {
                    onLoginPressed(email.value, password.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen._5sdp)),
                enabled = !isLoading,
                buttonType = ButtonType.White
            )

            Spacer(Modifier.height(dimensionResource(R.dimen._16sdp)))

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CKTextButton(
                        modifier = Modifier.padding(0.sdp()),
                        stringResource(R.string.btn_forgot_password),
                        onClick = { confirmForgotPasswordVisible.value = true },
                        enabled = !isLoading,
                        textButtonType = TextButtonType.White
                    )
                }
                if (isShowAdvanceSetting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CKTextButton(
                            modifier = Modifier.padding(0.sdp()),
                            stringResource(R.string.advance_server_settings),
                            onClick = { advanceSetting?.invoke() },
                            enabled = !isLoading,
                            textButtonType = TextButtonType.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen._24sdp)))
            Divider(
                color = colorResource(R.color.line),
                thickness = dimensionResource(R.dimen._1sdp)
            )
            Spacer(Modifier.height(dimensionResource(R.dimen._24sdp)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.social_sign_in),
                    color = Color.White,
                    style = TextStyle(
                        fontSize = defaultNonScalableTextSize(),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen._16sdp)))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CKButtonSignIn(
                    stringResource(R.string.btn_login_google),
                    onClick = {
                        onLoginGoogle?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen._5sdp)),
                    enabled = !isLoading,
                    buttonType = LoginType.Google
                )
                Spacer(Modifier.width(dimensionResource(R.dimen._40sdp)))
                CKButtonSignIn(
                    stringResource(R.string.btn_login_microsoft),
                    onClick = {
                        onLoginMicrosoft?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen._5sdp)),
                    enabled = !isLoading,
                    buttonType = LoginType.Microsoft
                )
                Spacer(Modifier.width(dimensionResource(R.dimen._40sdp)))
                CKButtonSignIn(
                    stringResource(R.string.btn_login_facebook),
                    onClick = {
                        onLoginFacebook?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen._5sdp)),
                    enabled = !isLoading,
                    buttonType = LoginType.Facebook
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen._80sdp)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.tv_not_account),
                    color = Color.White,
                    style = TextStyle(
                        fontSize = defaultNonScalableTextSize(),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen._16sdp)))
            CKButton(
                stringResource(R.string.sign_up),
                onClick = onRegisterPress,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen._32sdp)),
                buttonType = ButtonType.BorderWhite

            )
            Spacer(Modifier.height(dimensionResource(R.dimen._30sdp)))
            Text(
                "App version: ${BuildConfig.VERSION_NAME}",
                Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.caption.copy(
                    fontSize = 10.dp.toNonScalableTextSize()
                ),
                color = Color.White
            )
            Spacer(Modifier.height(dimensionResource(R.dimen._14sdp)))
        }
    }

    if (confirmForgotPasswordVisible.value) {
        CKAlertDialog(
            title = stringResource(R.string.warning),
            text = stringResource(R.string.reset_password_dialog_text),
            confirmTitle = stringResource(R.string.forgot),
            dismissTitle = stringResource(R.string.cancel),
            onConfirmButtonClick = {
                confirmForgotPasswordVisible.value = false
                onForgotPasswordPress()
            },
            onDismissButtonClick = {
                confirmForgotPasswordVisible.value = false
            }
        )
    }
}

@Composable
fun ViewUsedCustomServer(shouldShow: Boolean) {
    if (shouldShow) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen._8sdp)))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen._24sdp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alert),
                    contentDescription = "",
                    tint = LocalColorMapping.current.warning,
                    modifier = Modifier.padding(dimensionResource(R.dimen._10sdp))
                )
                Text(
                    text = stringResource(R.string.login_custom_server),
                    color = LocalColorMapping.current.warning,
                    style = TextStyle(
                        fontSize = defaultNonScalableTextSize(),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    } else {
        Spacer(Modifier.height(dimensionResource(R.dimen._48sdp)))
    }
}