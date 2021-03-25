package com.clearkeep.screen.auth.register

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextField
import com.clearkeep.components.base.CKTopAppBar

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel,
    onRegisterPressed: (email: String, userName: String, password: String, confirmPassword: String) -> Unit,
    onBackPress: () -> Unit,
    isLoading: Boolean = false
) {
    val email = remember {mutableStateOf("")}
    val displayName = remember {mutableStateOf("")}
    val password = remember {mutableStateOf("")}
    val confirmPassword = remember {mutableStateOf("")}

    val emailError = registerViewModel.emailError.observeAsState()
    val passError = registerViewModel.passError.observeAsState()
    val confirmPassError = registerViewModel.confirmPassError.observeAsState()
    val displayNameError = registerViewModel.displayNameError.observeAsState()

    val image = painterResource(R.drawable.ic_logo)

    Column(modifier = Modifier.fillMaxSize()) {
        CKTopAppBar(
                title = {
                    Text(text = "Register")
                },
                navigationIcon = {
                    IconButton(
                            onClick = {
                                onBackPress()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = ""
                        )
                    }
                },
        )
        Column (modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Image(image, contentDescription = "", modifier = Modifier.size(80.dp))
            }

            Spacer(Modifier.height(30.dp))

            CKTextField(
                "Email",
                "",
                email,
                keyboardType = KeyboardType.Email,
                error = emailError.value,
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            CKTextField(
                "Display name",
                "",
                displayName,
                error = displayNameError.value,
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            CKTextField(
                "Password",
                "",
                password,
                keyboardType = KeyboardType.Password,
                error = passError.value,
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            CKTextField(
                "Confirm password",
                "",
                confirmPassword,
                keyboardType = KeyboardType.Password,
                error = confirmPassError.value,
                singleLine = true,
            )
            Spacer(Modifier.height(40.dp))
            CKButton(
                    stringResource(R.string.btn_create_account),
                    onClick = {
                        onRegisterPressed(email.value, displayName.value, password.value, confirmPassword.value)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
            )
        }
    }
}