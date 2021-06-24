package com.clearkeep.screen.auth.forgot

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscaleOffWhite

@Composable
fun ForgotScreen(
    onForgotPressed: (email: String) -> Unit,
    onBackPress: () -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val email = remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(80.dp))
        CKTopAppBarSample(title = stringResource(R.string.forgot_password), onBackPressed = { onBackPress() })
        Spacer(Modifier.height(26.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_email_reset_pass),
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            CKTextInputField(
                "Email",
                email, singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_mail),
                        contentDescription = null
                    )
                }
            )
            Spacer(Modifier.height(24.dp))
            CKButton(
                stringResource(R.string.btn_reset_password),
                onClick = {
                    if (validateInput(context, email.value))
                        onForgotPressed(email.value)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                buttonType = ButtonType.White
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