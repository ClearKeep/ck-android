package com.clearkeep.login

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.ui.ButtonGeneral
import com.clearkeep.ui.FilledTextInputComponent

@Composable
fun LoginMainView(
    onRegisterPressed: (String) -> Unit
) {
    val context = ContextAmbient.current
    val userName = state { "" }
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.preferredHeight(24.dp))

            val image = imageResource(R.drawable.phone)
            val imageModifier = Modifier
                .preferredSize(100.dp)
            Box(
                modifier = Modifier.fillMaxWidth(),
                alignment= Alignment.Center,
                children = {
                    Text(
                        text = stringResource(R.string.title_app),
                        style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    )
                })

            Box(Modifier.fillMaxWidth(), alignment = Alignment.TopCenter) {
                Image(image, imageModifier)
            }

            FilledTextInputComponent(
                "Username",
                "",
                userName
            )
            Row() {
                ButtonGeneral(
                    stringResource(R.string.btn_register),
                    onClick = {
                        if (validateInput(context, userName.value))
                            onRegisterPressed(userName.value)
                    })
            }

        }
    }
}

private fun validateInput(context: Context, username: String): Boolean {
    if (TextUtils.isEmpty(username)) {
        Toast.makeText(
            context,
            "Username cannot be blank",
            Toast.LENGTH_LONG
        ).show()
        return false
    }
    return true
}

sealed class LoginViewState
object LoginSuccess : LoginViewState()
object LoginError : LoginViewState()
object LoginProcessing : LoginViewState()