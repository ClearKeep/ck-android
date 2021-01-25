package com.clearkeep.screen.auth.register

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextField
import com.clearkeep.components.base.CKTopAppBar

@Composable
fun RegisterScreen(
    onRegisterPressed: (userName: String, password: String, email: String) -> Unit,
    onBackPress: () -> Unit
) {
    val context = ContextAmbient.current
    val email = state { "" }
    val userName = state { "" }
    val password = state { "" }
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
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
        )
        Column (modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
            CKTextField(
                    "Email",
                    "",
                    email
            )
            Spacer(Modifier.preferredHeight(10.dp))
            CKTextField(
                    "Username",
                    "",
                    userName
            )
            Spacer(Modifier.preferredHeight(10.dp))
            CKTextField(
                    "Password",
                    "",
                    password,
                    keyboardType = KeyboardType.Password,
            )
            Spacer(Modifier.preferredHeight(20.dp))
            CKButton(
                    stringResource(R.string.btn_register),
                    onClick = {
                        if (validateInput(context, email.value, userName.value, password.value))
                            onRegisterPressed(userName.value, password.value, email.value)
                    },
                    modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun validateInput(context: Context, email: String, username: String, password: String): Boolean {
    var error: String? = null
    when {
        TextUtils.isEmpty(email) -> {
            error = "Email cannot be blank"
        }
        TextUtils.isEmpty(username) -> {
            error = "Username cannot be blank"
        }
        TextUtils.isEmpty(password) -> {
            error = "Password cannot be blank"
        }
    }
    if (error != null) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
    }
    return error == null
}