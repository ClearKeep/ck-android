package com.clearkeep.screen.auth.login

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextButton
import com.clearkeep.components.base.CKTextField

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel,
    onLoginPressed: (email: String, password: String) -> Unit,
    onRegisterPress: () -> Unit,
    onForgotPasswordPress: () -> Unit,
    isLoading: Boolean = false
) {
    val context = ContextAmbient.current
    val email = state { "" }
    val password = state { "" }
    val emailError = loginViewModel.emailError.observeAsState()
    val passError = loginViewModel.passError.observeAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.preferredHeight(24.dp))

            val image = imageResource(R.drawable.ic_logo)
            val imageModifier = Modifier
                .preferredSize(80.dp)
            Box(
                modifier = Modifier.fillMaxWidth(),
                alignment= Alignment.Center,
                children = {
                    Text(
                        text = stringResource(R.string.title_app),
                        style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    )
                })

            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxWidth(), alignment = Alignment.TopCenter) {
                Image(image, imageModifier)
            }

            Spacer(Modifier.preferredHeight(30.dp))

            Column (modifier = Modifier.padding(horizontal = 20.dp)) {
                CKTextField(
                    "Email",
                    "",
                    email,
                    keyboardType = KeyboardType.Email,
                    error = emailError.value
                )
                Spacer(Modifier.preferredHeight(10.dp))
                CKTextField(
                        "Password",
                        "",
                        password,
                        keyboardType = KeyboardType.Password,
                        passwordVisibility = true,
                    error = passError.value
                )
                Spacer(Modifier.preferredHeight(20.dp))
                CKButton(
                        stringResource(R.string.btn_login),
                        onClick = {
                            onLoginPressed(email.value, password.value)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        enabled = !isLoading
                )
                Spacer(Modifier.preferredHeight(30.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                ) {
                    CKTextButton(
                            stringResource(R.string.btn_register),
                            onClick = onRegisterPress,
                            enabled = !isLoading
                    )
                }
                /*Spacer(Modifier.preferredHeight(15.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                ) {
                    CKTextButton(
                            stringResource(R.string.btn_forgot_password),
                            onClick = onForgotPasswordPress,
                            enabled = !isLoading
                    )
                }*/
            }
        }
    }
}