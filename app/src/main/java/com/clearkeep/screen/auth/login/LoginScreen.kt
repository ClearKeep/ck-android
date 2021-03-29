package com.clearkeep.screen.auth.login

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    val email = remember {mutableStateOf("")}
    val password = remember {mutableStateOf("")}
    val emailError = loginViewModel.emailError.observeAsState()
    val passError = loginViewModel.passError.observeAsState()

    val image = painterResource(R.drawable.ic_logo)
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment= Alignment.Center,) {
                Text(
                    text = stringResource(R.string.title_app),
                    style = MaterialTheme.typography.body2.copy(
                        fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onPrimary
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Image(image, contentDescription = "", modifier = Modifier.size(80.dp))
            }

            Spacer(Modifier.height(30.dp))

            Column (modifier = Modifier.padding(horizontal = 20.dp)) {
                CKTextField(
                    "Email",
                    "Email",
                    email,
                    keyboardType = KeyboardType.Email,
                    error = emailError.value,
                    singleLine = true,
                )
                Spacer(Modifier.height(20.dp))
                CKTextField(
                    "Password",
                    "Password",
                    password,
                    keyboardType = KeyboardType.Password,
                    error = passError.value,
                    singleLine = true,
                )
                Spacer(Modifier.height(40.dp))
                CKButton(
                        stringResource(R.string.btn_login),
                        onClick = {
                            onLoginPressed(email.value, password.value)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        enabled = !isLoading
                )
                Spacer(Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                ) {
                    CKTextButton(
                            title = stringResource(R.string.btn_register),
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