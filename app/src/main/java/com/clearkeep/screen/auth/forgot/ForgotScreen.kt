package com.clearkeep.screen.auth.forgot

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextField
import com.clearkeep.components.base.CKTopAppBar

@Composable
fun ForgotScreen(
        onForgotPressed: (email: String) -> Unit,
        onBackPress: () -> Unit
) {
    val context = ContextAmbient.current
    val email = state { "" }
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
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
        )
        Column (modifier = Modifier.padding(horizontal = 30.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.preferredHeight(80.dp))
            CKTextField(
                    "Email",
                    "",
                    email
            )
            Spacer(Modifier.preferredHeight(15.dp))
            Text(text = "Please enter email here. Then, click on active link to change your password",
                    style = MaterialTheme.typography.caption)
            Spacer(Modifier.preferredHeight(30.dp))
            CKButton(
                    stringResource(R.string.btn_send),
                    onClick = {
                        if (validateInput(context, email.value))
                            onForgotPressed(email.value)
                    },
                    modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun validateInput(context: Context, email: String): Boolean {
    var error: String? = null
    when {
        TextUtils.isEmpty(email) -> {
            error = "Email cannot be blank"
        }
    }
    if (error != null) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
    }
    return error == null
}