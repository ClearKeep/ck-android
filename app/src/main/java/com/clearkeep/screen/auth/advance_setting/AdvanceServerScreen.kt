package com.clearkeep.screen.auth.advance_setting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.screen.auth.login.LoginViewModel
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Status


@ExperimentalAnimationApi
@Composable
fun CustomServerScreen(
    loginViewModel: LoginViewModel,
    onBackPress: () -> Unit,
    isCustom: Boolean,
    url: String,
) {
    val useCustomServerChecked = rememberSaveable { mutableStateOf(isCustom) }
    val rememberServerUrl = rememberSaveable { mutableStateOf(url) }
    val (showDialog, setShowDialog) = rememberSaveable { mutableStateOf("") }
    val serverUrlValidateResponse = loginViewModel.serverUrlValidateResponse.observeAsState()
    val isLoading = loginViewModel.isLoading.observeAsState()

    BackHandler {
        if (!useCustomServerChecked.value) {
            loginViewModel.isCustomServer = false
            loginViewModel.customDomain = ""
        }
        loginViewModel.clearLoading()
        loginViewModel.cancelCheckValidServer()
        onBackPress()
    }

    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = LocalColorMapping.current.backgroundBrush
                )
            )
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(58.sdp()))
            CKTopAppBarSample(
                modifier = Modifier.padding(start = 6.sdp()),
                title = stringResource(R.string.advance_server_settings)
            ) {
                if (!useCustomServerChecked.value) {
                    loginViewModel.isCustomServer = false
                    loginViewModel.customDomain = ""
                } else {
                    loginViewModel.isCustomServer = isCustom
                    loginViewModel.customDomain = url
                }
                loginViewModel.clearLoading()
                loginViewModel.cancelCheckValidServer()
                onBackPress()
            }
            Spacer(Modifier.height(26.sdp()))
            Row(
                modifier = Modifier
                    .padding(16.sdp())
                    .clickable {
                        useCustomServerChecked.value = !useCustomServerChecked.value
                        if (!useCustomServerChecked.value) {
                            rememberServerUrl.value = ""
                        }
                    }, verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = if (useCustomServerChecked.value) R.drawable.ic_checkbox else R.drawable.ic_ellipse_20),
                    "",
                    Modifier.size(32.sdp()),
                    contentScale = ContentScale.FillBounds
                )
                Text(
                    text = stringResource(R.string.advance_server_settings_custom_server),
                    modifier = Modifier.padding(16.sdp()),
                    style = MaterialTheme.typography.body1.copy(
                        color = grayscaleOffWhite,
                        fontSize = defaultNonScalableTextSize(),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Column(Modifier.padding(start = 16.sdp(), end = 16.sdp())) {
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
                                fontSize = 16.sdp().toNonScalableTextSize(),
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Spacer(modifier = Modifier.height(20.sdp()))

                        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier
                                    .weight(0.66f)
                                    .padding(end = 16.sdp()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CKTextInputField(
                                    stringResource(R.string.server_url),
                                    rememberServerUrl,
                                    keyboardType = KeyboardType.Text,
                                    singleLine = true,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(50.sdp()))

                        CKButton(
                            stringResource(R.string.all_submit), {
                                loginViewModel.checkValidServerUrl(rememberServerUrl.value)
                            },
                            modifier = Modifier.padding(start = 60.sdp(), end = 66.sdp()),
                            buttonType = ButtonType.White,
                            enabled = rememberServerUrl.value.isNotBlank() && isLoading.value != true
                        )
                    }
                }
            }
            Spacer(Modifier.height(58.sdp()))
        }
        if (isLoading.value == true) {
            CKCircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        ErrorDialog(showDialog, setShowDialog)
    }

    if (serverUrlValidateResponse.value?.status == Status.SUCCESS) {
        loginViewModel.isCustomServer = useCustomServerChecked.value
        loginViewModel.customDomain = rememberServerUrl.value
        loginViewModel.serverUrlValidateResponse.value = null
        onBackPress()
    } else if (serverUrlValidateResponse.value?.status == Status.ERROR) {
        val (title, text, dismissText) = if (serverUrlValidateResponse.value!!.errorCode == ERROR_CODE_TIMEOUT) {
            Triple(
                stringResource(R.string.network_error_dialog_title),
                stringResource(R.string.network_error_dialog_text),
                stringResource(R.string.ok)
            )
        } else {
            Triple(
                stringResource(R.string.error),
                stringResource(R.string.wrong_server_url_error),
                stringResource(R.string.close)
            )
        }
        CKAlertDialog(
            title = title,
            text = text,
            dismissTitle = dismissText,
            onDismissButtonClick = {
                loginViewModel.serverUrlValidateResponse.value = null
            }
        )
    }
}

@Composable
fun ErrorDialog(showDialog: String, setShowDialog: (String) -> Unit) {
    if (showDialog.isNotEmpty()) {
        CKAlertDialog(
            title = stringResource(R.string.error),
            text = showDialog,
            onDismissButtonClick = {
                // Change the state to close the dialog
                setShowDialog("")
            },
        )
    }
}