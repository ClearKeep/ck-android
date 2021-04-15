package com.clearkeep.screen.auth.login

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.*

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel,
    onLoginPressed: (email: String, password: String) -> Unit,
    onRegisterPress: () -> Unit,
    onForgotPasswordPress: () -> Unit,
    isLoading: Boolean = false,
    onLoginGoogle: (() -> Unit)? = null,
    onLoginMicrosoft: (() -> Unit)? = null
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val emailError = loginViewModel.emailError.observeAsState()
    val passError = loginViewModel.passError.observeAsState()

    val image = painterResource(R.drawable.ic_logo)

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Image(image, contentDescription = "")
            }

            Spacer(Modifier.height(48.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                CKTextInputField(
                    stringResource(R.string.tv_email),
                    email,
                    keyboardType = KeyboardType.Email,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_mail),
                            contentDescription = null
                        )
                    }
                )
                Spacer(Modifier.height(24.dp))
                CKTextInputField(
                    stringResource(R.string.tv_password),
                    password,
                    keyboardType = KeyboardType.Password,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null
                        )
                    }
                )

                Spacer(Modifier.height(24.dp))
                CKButton(
                    stringResource(R.string.btn_login),
                    onClick = {
                        onLoginPressed(email.value, password.value)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    enabled = !isLoading,
                    buttonType = ButtonType.White
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CKTextButton(
                        modifier = Modifier.padding(0.dp),
                        stringResource(R.string.btn_forgot_password),
                        onClick = onForgotPasswordPress,
                        enabled = !isLoading,
                        textButtonType = TextButtonType.White
                    )
                }
                Spacer(Modifier.height(24.dp))

                Divider(color = colorResource(R.color.line), thickness = 1.dp)
                Spacer(Modifier.height(24.dp))
                CKButtonSignIn(
                    stringResource(R.string.btn_login_google),
                    onClick = {
                        onLoginGoogle?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    enabled = !isLoading,
                    buttonType = LoginType.Google
                )
                Spacer(Modifier.height(16.dp))
                CKButtonSignIn(
                    stringResource(R.string.btn_login_microsoft),
                    onClick = {
                        onLoginMicrosoft?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    enabled = !isLoading,
                    buttonType = LoginType.Microsoft
                )
                Spacer(Modifier.height(42.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.tv_not_account),
                        color = Color.White,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(16.dp))
                CKButton(
                    stringResource(R.string.btn_register),
                    onClick = onRegisterPress,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    buttonType = ButtonType.BorderWhite

                )

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}