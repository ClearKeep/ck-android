package com.clearkeep.presentation.screen.auth.register

import androidx.compose.foundation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.clearkeep.R
import com.clearkeep.presentation.components.*
import com.clearkeep.presentation.components.base.*
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel,
    onRegisterPressed: (email: String, userName: String, password: String, confirmPassword: String) -> Unit,
    onBackPress: () -> Unit,
    isLoading: Boolean = false
) {
    val localFocusManager = LocalFocusManager.current

    val email = rememberSaveable { mutableStateOf("") }
    val displayName = rememberSaveable { mutableStateOf("") }
    val password = rememberSaveable { mutableStateOf("") }
    val confirmPassword = rememberSaveable { mutableStateOf("") }

    val emailError = registerViewModel.emailError.observeAsState()
    val passError = registerViewModel.passError.observeAsState()
    val confirmPassError = registerViewModel.confirmPassError.observeAsState()
    val displayNameError = registerViewModel.displayNameError.observeAsState()

    val image = painterResource(R.drawable.ic_logo)

    Column(
        modifier = Modifier
            .padding(horizontal = 20.sdp())
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.sdp()))
        Box(contentAlignment = Alignment.TopCenter) {
            Image(image, contentDescription = "")
        }
        Spacer(Modifier.height(24.sdp()))
        Card(shape = RoundedCornerShape(16.sdp())) {
            Column(
                modifier = Modifier
                    .background(if (isSystemInDarkTheme()) colorSurfaceDark else Color.White)
                    .padding(horizontal = 16.sdp(), vertical = 24.sdp())
            ) {
                CKText(
                    text = stringResource(R.string.sign_up_fill_information),
                    color = if (isSystemInDarkTheme()) grayscaleDarkModeGreyLight2 else grayscaleBlack,
                    fontSize = defaultNonScalableTextSize()
                )
                Spacer(Modifier.height(24.sdp()))
                CKTextInputField(
                    stringResource(R.string.tv_email),
                    email,
                    keyboardType = KeyboardType.Email,
                    error = emailError.value,
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    onNext = {
                        localFocusManager.moveFocus(FocusDirection.Down)
                    },
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_mail),
                            contentDescription = null,
                            Modifier.size(24.sdp()),
                            contentScale = ContentScale.FillBounds,
                            colorFilter = LocalColorMapping.current.textFieldIconFilter
                        )
                    }
                )
                Spacer(Modifier.height(10.sdp()))
                CKTextInputField(
                    stringResource(R.string.display_name),
                    displayName,
                    error = displayNameError.value,
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    onNext = {
                        localFocusManager.moveFocus(FocusDirection.Down)
                    },
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_user_check),
                            contentDescription = null,
                            Modifier.size(24.sdp()),
                            contentScale = ContentScale.FillBounds,
                            colorFilter = LocalColorMapping.current.textFieldIconFilter
                        )
                    },
                    maxChars = 30
                )
                Spacer(Modifier.height(10.sdp()))
                CKTextInputField(
                    stringResource(R.string.tv_password),
                    password,
                    keyboardType = KeyboardType.Password,
                    error = passError.value,
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    onNext = {
                        localFocusManager.moveFocus(FocusDirection.Down)
                    },
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null,
                            Modifier.size(24.sdp()),
                            contentScale = ContentScale.FillBounds,
                            colorFilter = LocalColorMapping.current.textFieldIconFilter
                        )
                    }
                )
                Spacer(Modifier.height(10.sdp()))
                CKTextInputField(
                    stringResource(R.string.confirm_password),
                    confirmPassword,
                    keyboardType = KeyboardType.Password,
                    error = confirmPassError.value,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null,
                            Modifier.size(24.sdp()),
                            contentScale = ContentScale.FillBounds,
                            colorFilter = LocalColorMapping.current.textFieldIconFilter
                        )
                    },
                )
                Spacer(Modifier.height(24.sdp()))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1.0f, true),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CKTextButton(
                            title = stringResource(R.string.register_sign_in_instead),
                            onClick = onBackPress,
                            textButtonType = TextButtonType.Blue,
                            fontSize = 12.sdp().toNonScalableTextSize()
                        )
                    }

                    CKButton(
                        stringResource(R.string.sign_up),
                        onClick = {
                            onRegisterPressed(
                                email.value,
                                displayName.value,
                                password.value,
                                confirmPassword.value
                            )
                        },
                        enabled = !isLoading && email.value.isNotBlank() && displayName.value.isNotBlank() && password.value.isNotBlank() && confirmPassword.value.isNotBlank(),
                        modifier = Modifier
                            .width(120.sdp())
                            .height(40.sdp())
                    )
                }
            }
        }
        Spacer(Modifier.height(28.sdp()))
    }
}