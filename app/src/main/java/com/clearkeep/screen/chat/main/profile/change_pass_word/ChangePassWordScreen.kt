package com.clearkeep.screen.chat.main.profile.change_pass_word

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.ButtonType
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTextInputField
import com.clearkeep.components.base.CKToolbarBack
import com.clearkeep.screen.chat.main.MainViewModel
import com.clearkeep.screen.chat.main.profile.ProfileViewModel

@Composable
fun ChangePassWordScreen(
    profileViewModel: ProfileViewModel,
    homeViewModel: MainViewModel,
    onBackPress: (() -> Unit)
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isSystemInDarkTheme()) grayscaleBlack else backgroundGradientStart,
                        if (isSystemInDarkTheme()) grayscaleBlack else backgroundGradientEnd
                    )
                )
            )
    ) {
        val currentPassWord = remember { mutableStateOf("") }
        val newPassWord = remember { mutableStateOf("") }
        val confirmPassWord = remember { mutableStateOf("") }

        Spacer(Modifier.height(58.dp))
        CKToolbarBack(modifier = Modifier.padding(start = 6.dp),
            title = stringResource(R.string.enter_new_password), onClick = {
            onBackPress.invoke()
        })
        Spacer(Modifier.height(26.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_detail_change_pass),
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            CKTextInputField(
                placeholder = "Current Password",
                textValue = currentPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null
                    )
                }
            )
            Spacer(Modifier.height(24.dp))
            CKTextInputField(
                placeholder = "New Password",
                textValue = newPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null
                    )
                }
            )
            Spacer(Modifier.height(24.dp))
            CKTextInputField(
                placeholder = "Confirm Password",
                textValue = confirmPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null
                    )
                }
            )
            Spacer(Modifier.height(24.dp))
            CKButton(
                stringResource(R.string.save),
                onClick = {

                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White
            )
        }
    }

}