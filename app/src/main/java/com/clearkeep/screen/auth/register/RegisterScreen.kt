package com.clearkeep.screen.auth.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscaleBlack
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.toNonScalableTextSize

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
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
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
                CKText(
                    text = stringResource(R.string.sign_up_fill_information),
                    color = grayscaleBlack,
                    textAlign = TextAlign.Justify,
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
                    },
                    maxChars = 30
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
                    },
                    allowSpace = false
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
                    },
                    allowSpace = false
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
                                password.value.trim(),
                                confirmPassword.value.trim()
                            )
                        },
                        enabled = !isLoading && email.value.isNotBlank() && displayName.value.isNotBlank() && password.value.isNotBlank() && confirmPassword.value.isNotBlank(),
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
@Preview(fontScale = 1.00f, device = Devices.PIXEL_4_XL)
fun ScalableTextPreview() {
    Row(Modifier.fillMaxSize()) {
        Text(
            text = "em", //Default text size converted to em
            color = grayscaleBlack,
            textAlign = TextAlign.Justify,
            fontSize = defaultNonScalableTextSize()
        )
        Text(
            text = "em", //Default text size
            color = grayscaleBlack,
            textAlign = TextAlign.Justify,
        )
        Text(
            text = "em", //Default text size in sp
            color = grayscaleBlack,
            textAlign = TextAlign.Justify,
            fontSize = 14.sp
        )
        Text(
            text = "em", //14 dp converted to em
            color = grayscaleBlack,
            textAlign = TextAlign.Justify,
            fontSize = 14.dp.toNonScalableTextSize()
        )
    }
}