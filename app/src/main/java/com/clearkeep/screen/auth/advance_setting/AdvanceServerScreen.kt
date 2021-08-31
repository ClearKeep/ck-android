package com.clearkeep.screen.auth.advance_setting

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
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
import com.clearkeep.components.base.*
import com.clearkeep.screen.auth.login.LoginViewModel
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK

@ExperimentalAnimationApi
@Composable
fun CustomServerScreen(
    loginViewModel: LoginViewModel,
    onBackPress: () -> Unit,
    isCustom: Boolean,
    url: String,
) {
    val useCustomServerChecked = remember { mutableStateOf(isCustom) }
    val rememberServerUrl = remember { mutableStateOf(url) }
    val (showDialog, setShowDialog) = remember { mutableStateOf("") }
    val serverUrlValidateResponse = loginViewModel.serverUrlValidateResponse.observeAsState()
    val isLoading = loginViewModel.isLoading.observeAsState()

    BackHandler {
        printlnCK("Advanced server settings back press")
        loginViewModel.isCustomServer = false
        loginViewModel.customDomain = ""
        loginViewModel.clearLoading()
        loginViewModel.cancelCheckValidServer()
        onBackPress()
    }

    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        //todo disable dark mode
                        if (isSystemInDarkTheme()) backgroundGradientStart else backgroundGradientStart,
                        if (isSystemInDarkTheme()) backgroundGradientEnd else backgroundGradientEnd,
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(58.dp))
            CKTopAppBarSample(
                modifier = Modifier.padding(start = 6.dp),
                title = stringResource(R.string.advance_server_settings),
                onBackPressed = {
                    loginViewModel.isCustomServer = isCustom
                    loginViewModel.customDomain = url
                    loginViewModel.clearLoading()
                    loginViewModel.cancelCheckValidServer()
                    onBackPress()
                }
            )
            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        useCustomServerChecked.value = !useCustomServerChecked.value
                        if (!useCustomServerChecked.value) {
                            rememberServerUrl.value = ""
                        }
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
            Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
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
                            text = stringResource(R.string.use_custom_server),
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
                                    .weight(0.66f)
                                    .padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                CKTextInputField(
                                    "Server URL",
                                    rememberServerUrl,
                                    keyboardType = KeyboardType.Text,
                                    singleLine = true,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(58.dp))

                        CKButton(
                            "Submit", {
                                loginViewModel.checkValidServerUrl(rememberServerUrl.value)
                            },
                            modifier = Modifier.padding(start = 60.dp, end = 66.dp),
                            buttonType = ButtonType.White,
                            enabled = rememberServerUrl.value.isNotBlank() && isLoading.value != true
                        )
                    }
                }
            }
        }
        if (isLoading.value == true) {
            CKCircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        ErrorDialog(showDialog, setShowDialog)
    }

    if (!serverUrlValidateResponse.value.isNullOrBlank()) {
        loginViewModel.isCustomServer = useCustomServerChecked.value
        loginViewModel.customDomain = rememberServerUrl.value
        loginViewModel.serverUrlValidateResponse.value = null
        onBackPress()
    } else if (serverUrlValidateResponse.value?.isBlank() == true) {
        CKAlertDialog(
            title = stringResource(R.string.warning),
            text = stringResource(R.string.wrong_server_url_error),
            dismissTitle = stringResource(R.string.close),
            onDismissButtonClick = {
                loginViewModel.serverUrlValidateResponse.value = null
            })
    }
}

@Composable
fun ErrorDialog(showDialog: String, setShowDialog: (String) -> Unit) {
    if (showDialog.isNotEmpty()) {
        CKAlertDialog(
            title = "Error",
            text = showDialog,
            onDismissButtonClick = {
                // Change the state to close the dialog
                setShowDialog("")
            },
        )
    }
}