package com.clearkeep.screen.auth.forgot

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextField
import com.clearkeep.components.base.CKTopAppBar

@Composable
fun ForgotScreen(
        onForgotPressed: (email: String) -> Unit,
        onBackPress: () -> Unit,
        isLoading: Boolean = false
) {
    val context = LocalContext.current
    val email = remember {mutableStateOf("")}
    Column(modifier = Modifier.fillMaxSize()) {
        CKTopAppBar(
                title = {
                    Text(text = "Forgot password")
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
        Column (modifier = Modifier.padding(horizontal = 30.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(80.dp))
            CKTextField(
                    "Email",
                    "",
                    email
            )
            Spacer(Modifier.height(15.dp))
            Text(text = "Please enter email here. Then, click on active link to change your password",
                    style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(30.dp))
            CKButton(
                    stringResource(R.string.btn_reset_password),
                    onClick = {
                        if (validateInput(context, email.value))
                            onForgotPressed(email.value)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
            )
        }
    }
}

private fun validateInput(context: Context, email: String): Boolean {
    var error: String? = null
    when {
        TextUtils.isEmpty(email) -> {
            error = "Email must not be blank "
        }
    }
    if (error != null) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
    }
    return error == null
}