package com.clearkeep.screen.auth.advance_setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKTextInputField
import com.clearkeep.components.base.CKToolbarBack
import com.clearkeep.screen.auth.login.LoginViewModel


@ExperimentalAnimationApi
@Composable
fun CustomServerScreen(onBackPress: () -> Unit, loginViewModel: LoginViewModel) {
    val useCustomServerChecked = remember { mutableStateOf(false) }
    val rememberServerUrl = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isSystemInDarkTheme()) grayscaleBlack else backgroundGradientStart,
                        if (isSystemInDarkTheme()) grayscaleBlack else backgroundGradientEnd,
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(80.dp))
            CKToolbarBack(
                title = stringResource(R.string.advance_settings),
                onClick = { onBackPress() })
            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier
                    .padding( 16.dp)
                    .clickable {
                        useCustomServerChecked.value = !useCustomServerChecked.value
                    }, verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = if (useCustomServerChecked.value) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
                    ""
                )
                Text(
                    text = "Use Custom Server",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.body1.copy(
                        color = grayscaleOffWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Column(Modifier.padding(start = 16.dp,end = 16.dp)) {
                AnimatedVisibility(
                    visible = useCustomServerChecked.value,
                    enter = expandIn(
                        expandFrom = Alignment.BottomStart,
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    ),
                    exit = shrinkOut(
                        shrinkTowards = Alignment.CenterStart,
                        targetSize = { fullSize ->
                            IntSize(
                                fullSize.width / 10,
                                fullSize.height / 10
                            )
                        },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )

                ) {
                    Column {
                        Text(
                            text = "Please enter your server URL and Port to enter custom server",
                            style = MaterialTheme.typography.body1.copy(
                                color = grayscaleOffWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier
                                    .weight(0.66f).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                CKTextInputField(
                                    "Server URL",
                                    rememberServerUrl,
                                    keyboardType = KeyboardType.Text,
                                    singleLine = true,
                                )
                            }
                            Box(modifier = Modifier) {
                                OutlinedButton(
                                    onClick = {
                                        if (rememberServerUrl.value.isNotEmpty()) {
                                            loginViewModel.urlCustomServer.postValue(
                                                rememberServerUrl.value
                                            )
                                            onBackPress.invoke()
                                        }
                                    },
                                    modifier = Modifier.width(76.dp).height(52.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = grayscale5,
                                        contentColor = grayscale3,
                                    ),shape  = RoundedCornerShape(16.dp)
                                ) {
                                    Text(text = "Port",style = MaterialTheme.typography.body1.copy(
                                        color = grayscale3,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal))
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}