package com.clearkeep.screen.auth.login

import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import com.clearkeep.components.colorWarningLight
import com.clearkeep.components.grayscaleOffWhite

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
            if (isJoinServer) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp)) {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "",
                            tint = grayscaleOffWhite,
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Image(image, contentDescription = "")
            }

            ViewUsedCustomServer(loginViewModel.customDomain.isNotEmpty() && loginViewModel.isCustomServer)
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
                    },
                    allowSpace = false
                )

                Spacer(Modifier.height(24.dp))
                CKButton(
                    stringResource(R.string.btn_login),
                    onClick = {
                        onLoginPressed(email.value, password.value.trim())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    enabled = !isLoading,
                    buttonType = ButtonType.White
                )

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
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
                    if (isShowAdvanceSetting) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CKTextButton(
                                modifier = Modifier.padding(0.dp),
                                stringResource(R.string.advance_server_settings),
                                onClick ={advanceSetting?.invoke()} ,
                                enabled = !isLoading,
                                textButtonType = TextButtonType.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider(color = colorResource(R.color.line), thickness = 1.dp)
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Social Sign-in",
                        color = Color.White,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(Modifier.height(16.dp))

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
                            .padding(vertical = 5.dp),
                        enabled = !isLoading,
                        buttonType = LoginType.Google
                    )
                    Spacer(Modifier.width(40.dp))
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
                    Spacer(Modifier.width(40.dp))
                    CKButtonSignIn(
                        stringResource(R.string.btn_login_facebook),
                        onClick = {
                            onLoginFacebook?.invoke()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        enabled = !isLoading,
                        buttonType = LoginType.Facebook
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
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
                    stringResource(R.string.sign_up),
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

@Composable
fun ViewUsedCustomServer(shouldShow: Boolean){
    if (shouldShow) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
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
                    tint = colorWarningLight,
                    modifier = Modifier.padding(10.dp)
                )
                Text(
                    text = "You are using custom server",
                    color = colorWarningLight,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }else{
        Spacer(Modifier.height(48.dp))
    }
}