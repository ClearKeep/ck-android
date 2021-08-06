package com.clearkeep.screen.auth.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextButton
import com.clearkeep.components.base.CKTextInputField
import com.clearkeep.components.base.TextButtonType
import com.clearkeep.components.grayscaleBlack

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel,
    onRegisterPressed: (email: String, userName: String, password: String, confirmPassword: String) -> Unit,
    onBackPress: () -> Unit,
    isLoading: Boolean = false
) {
    val email = remember { mutableStateOf("") }
    val displayName = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }

    val emailError = registerViewModel.emailError.observeAsState()
    val passError = registerViewModel.passError.observeAsState()
    val confirmPassError = registerViewModel.confirmPassError.observeAsState()
    val displayNameError = registerViewModel.displayNameError.observeAsState()

    val image = painterResource(R.drawable.ic_logo)

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            Image(image, contentDescription = "")
        }
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.sign_up_fill_information),
                    color = grayscaleBlack,
                    textAlign = TextAlign.Justify
                )
                Spacer(Modifier.height(24.dp))
                CKTextInputField(
                    "Email",
                    email,
                    keyboardType = KeyboardType.Email,
                    error = emailError.value,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_mail),
                            contentDescription = null
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                CKTextInputField(
                    stringResource(R.string.display_name),
                    displayName,
                    error = displayNameError.value,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_user_check),
                            contentDescription = null
                        )
                    }

                )
                Spacer(Modifier.height(10.dp))
                CKTextInputField(
                    "Password",
                    password,
                    keyboardType = KeyboardType.Password,
                    error = passError.value,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                CKTextInputField(
                    stringResource(R.string.confirm_password),
                    confirmPassword,
                    keyboardType = KeyboardType.Password,
                    error = confirmPassError.value,
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painterResource(R.drawable.ic_icon_lock),
                            contentDescription = null
                        )
                    }
                )
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1.0f, true),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CKTextButton(
                            title = "Sign in instead",
                            onClick = onBackPress,
                            textButtonType = TextButtonType.Blue
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
                        enabled = !isLoading,
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                    )
                }
            }
        }
    }
}