package com.clearkeep.screen.chat.change_pass_word

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
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
import com.clearkeep.components.base.CKTopAppBarSample
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun ChangePassWordScreen(
    onBackPress: (() -> Unit)
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        //todo disable dark mode
                        if (isSystemInDarkTheme()) backgroundGradientStart else backgroundGradientStart,
                        if (isSystemInDarkTheme()) backgroundGradientEnd else backgroundGradientEnd
                    )
                )
            )
    ) {
        val currentPassWord = remember { mutableStateOf("") }
        val newPassWord = remember { mutableStateOf("") }
        val confirmPassWord = remember { mutableStateOf("") }

        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(modifier = Modifier.padding(start = 6.sdp()),
            title = stringResource(R.string.enter_new_password), onBackPressed = {
            onBackPress.invoke()
        })
        Spacer(Modifier.height(26.sdp()))
        Column(
            modifier = Modifier
                .padding(horizontal = 16.sdp())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_detail_change_pass),
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(16.sdp()))
            CKTextInputField(
                placeholder = "Current Password",
                textValue = currentPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        contentScale = ContentScale.FillBounds
                    )
                }
            )
            Spacer(Modifier.height(24.sdp()))
            CKTextInputField(
                placeholder = "New Password",
                textValue = newPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        contentScale = ContentScale.FillBounds
                    )
                }
            )
            Spacer(Modifier.height(24.sdp()))
            CKTextInputField(
                placeholder = stringResource(R.string.confirm_password),
                textValue = confirmPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null,
                        Modifier.size(dimensionResource(R.dimen._24sdp)),
                        contentScale = ContentScale.FillBounds
                    )
                }
            )
            Spacer(Modifier.height(24.sdp()))
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