package com.clearkeep.screen.chat.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.navigation.NavController
import com.clearkeep.BuildConfig
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.home.composes.SideBarLabel
import com.clearkeep.screen.chat.room.UploadPhotoDialog
import com.clearkeep.utilities.defaultNonScalableTextSize
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@ExperimentalMaterialApi
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    onCloseView: () -> Unit,
    onChangePassword: () -> Unit,
    onCopyToClipBoard: () -> Unit,
    onNavigateToOtp: () -> Unit
) {
    val versionName = BuildConfig.VERSION_NAME
    val env = BuildConfig.FLAVOR
    val profile = profileViewModel.profile.observeAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    BackHandler {
        onCloseView()
    }

    profile.value?.let { _ ->
        val userName = profileViewModel.username.observeAsState()
        val userNameErrorVisible = profileViewModel.usernameError.observeAsState()
        val email = profileViewModel.email.observeAsState()
        val phoneNumber = profileViewModel.phoneNumber.observeAsState()
        val phoneNumberErrorVisible = profileViewModel.phoneNumberError.observeAsState()
        val countryCode = profileViewModel.countryCode.observeAsState()
        val otpErrorDialogVisible = rememberSaveable { mutableStateOf(false) }
        val pickAvatarDialogVisible = rememberSaveable { mutableStateOf(false) }
        val unsavedChangesDialogVisible =
            profileViewModel.unsavedChangeDialogVisible.observeAsState()
        val uploadAvatarResponse = profileViewModel.uploadAvatarResponse.observeAsState()
        val updateMfaResponse = profileViewModel.updateMfaSettingResponse.observeAsState()
        val selectedAvatar = profileViewModel.imageUriSelected.observeAsState()
        val userPreference = profileViewModel.userPreference.observeAsState()

        Column(
            Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    profile.value?.let { user ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.sdp()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HeaderProfile(
                                onClickSave = {
                                    profileViewModel.updateProfileDetail(
                                        context,
                                        userName.value ?: "",
                                        phoneNumber.value ?: "",
                                        countryCode.value ?: ""
                                    )
                                },
                                onCloseView = {
                                    onCloseView()
                                }
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircleAvatar(
                                    when {
                                        selectedAvatar.value != null -> {
                                            listOf(selectedAvatar.value!!)
                                        }
                                        user.avatar != null -> {
                                            listOf(user.avatar)
                                        }
                                        else -> {
                                            emptyList()
                                        }
                                    } as List<String>,
                                    user.userName ?: "",
                                    size = 72.sdp(),
                                    modifier = Modifier.clickable {
                                        pickAvatarDialogVisible.value = true
                                    },
                                    cacheKey = user.updatedAt.toString()
                                )
                                Column(
                                    Modifier.padding(start = 16.sdp()),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SideBarLabel(
                                        text = stringResource(R.string.profile_avatar_change),
                                        color = primaryDefault,
                                        fontSize = defaultNonScalableTextSize(),
                                    )
                                    SideBarLabel(
                                        text = stringResource(R.string.profile_avatar_max_size),
                                        color = LocalColorMapping.current.bodyTextDisabled,
                                        modifier = Modifier,
                                        fontSize = 12.sdp().toNonScalableTextSize()
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.sdp()))
                            ItemInformationView(
                                stringResource(R.string.username),
                                errorText = stringResource(R.string.profile_username_error),
                                errorVisible = userNameErrorVisible.value ?: false
                            ) {
                                ItemInformationInput(
                                    textValue = userName.value ?: "",
                                    hasError = userNameErrorVisible.value ?: false
                                ) {
                                    profileViewModel.setUsername(it)
                                }
                            }
                            Spacer(Modifier.height(16.sdp()))
                            ItemInformationView(stringResource(R.string.tv_email)) {
                                ItemInformationInput(
                                    textValue = email.value ?: "",
                                    enable = false
                                ) {
                                    profileViewModel.setEmail(it)
                                }
                            }
                            Spacer(Modifier.height(16.sdp()))
                            ItemInformationView(
                                stringResource(R.string.profile_phone_number),
                                errorText = stringResource(R.string.profile_phone_number_error),
                                errorVisible = phoneNumberErrorVisible.value ?: false
                            ) {
                                Row(Modifier.height(IntrinsicSize.Max)) {
                                    Box(
                                        Modifier
                                            .weight(1.3f)
                                            .fillMaxHeight()
                                            .fillMaxWidth()
                                            .background(
                                                if (phoneNumberErrorVisible.value == true) {
                                                    LocalColorMapping.current.textFieldBackgroundAltError
                                                } else {
                                                    LocalColorMapping.current.textFieldBackgroundAlt
                                                },
                                                MaterialTheme.shapes.large
                                            )
                                            .clip(MaterialTheme.shapes.large)
                                            .clickable {
                                                focusManager.clearFocus()
                                                navController.navigate("country_code_picker")
                                            }
                                            .then(
                                                if (phoneNumberErrorVisible.value == true) Modifier.border(
                                                    1.sdp(),
                                                    LocalColorMapping.current.error,
                                                    MaterialTheme.shapes.large
                                                ) else Modifier
                                            )
                                    ) {
                                        val countryCode = countryCode.value ?: ""
                                        Text(
                                            countryCode,
                                            Modifier
                                                .padding(start = 12.sdp())
                                                .align(Alignment.CenterStart)
                                                .fillMaxWidth(),
                                            style = MaterialTheme.typography.body1.copy(
                                                color = LocalColorMapping.current.bodyTextAlt,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = defaultNonScalableTextSize()
                                            )
                                        )
                                        Image(
                                            painterResource(R.drawable.ic_chev_down),
                                            null,
                                            Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 11.sdp()),
                                            colorFilter = LocalColorMapping.current.closeIconFilter
                                        )
                                    }
                                    Spacer(Modifier.width(8.sdp()))
                                    ItemInformationInput(
                                        Modifier
                                            .weight(3f)
                                            .fillMaxSize(),
                                        hasError = phoneNumberErrorVisible.value ?: false,
                                        textValue = phoneNumber.value ?: "",
                                        keyboardType = KeyboardType.Number,
                                        placeholder = stringResource(R.string.profile_phone_number),
                                    ) {
                                        profileViewModel.setPhoneNumber(it)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.sdp()))
                            CopyLink(
                                onCopied = {
                                    onCopyToClipBoard()
                                }
                            )
                            Spacer(Modifier.height(8.sdp()))
                            if (userPreference.value?.isSocialAccount == false) {
                                ChangePassword(onChangePassword)
                                Spacer(Modifier.height(24.sdp()))
                            }
                            TwoFaceAuthView(userPreference.value?.mfa ?: false) {
                                focusManager.clearFocus()
                                if (it) {
                                    if (profileViewModel.getMfaErrorMessage().first.isEmpty()) {
                                        profileViewModel.updateMfaSettings(it)
                                    } else {
                                        otpErrorDialogVisible.value = true
                                    }
                                } else {
                                    profileViewModel.updateMfaSettings(it)
                                }
                            }
                            Spacer(Modifier.height(24.sdp()))
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 8.sdp(), bottom = 20.sdp()),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "version $versionName (${env.toUpperCase()})",
                                    style = MaterialTheme.typography.caption.copy(
                                        fontSize = defaultNonScalableTextSize()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        if (otpErrorDialogVisible.value) {
            val errorMessage = profileViewModel.getMfaErrorMessage()
            CKAlertDialog(
                title = errorMessage.first,
                text = errorMessage.second,
                dismissTitle = stringResource(R.string.close),
                onDismissButtonClick = {
                    otpErrorDialogVisible.value = false
                }
            )
        }
        if (uploadAvatarResponse.value != null && uploadAvatarResponse.value?.status == Status.ERROR) {
            CKAlertDialog(
                title = stringResource(R.string.warning),
                text = uploadAvatarResponse.value!!.message ?: "",
                dismissTitle = stringResource(R.string.confirm),
                onDismissButtonClick = {
                    profileViewModel.uploadAvatarResponse.value = null
                },
            )
        }
        if (unsavedChangesDialogVisible.value == true) {
            CKAlertDialog(
                title = stringResource(R.string.warning),
                text = stringResource(R.string.profile_unsaved_change_warning),
                dismissTitle = stringResource(R.string.cancel),
                confirmTitle = stringResource(R.string.leave),
                onDismissButtonClick = {
                    profileViewModel.unsavedChangeDialogVisible.value = false
                },
                onConfirmButtonClick = {
                    profileViewModel.unsavedChangeDialogVisible.value = false
                    profileViewModel.undoProfileChanges()
                    onCloseView()
                }
            )
        }
        if (updateMfaResponse.value?.status == Status.SUCCESS) {
            onNavigateToOtp()
            profileViewModel.updateMfaSettingResponse.value =
                null //Prevent response from being handled again
        } else if (updateMfaResponse.value?.status == Status.ERROR) {
            CKAlertDialog(
                title = updateMfaResponse.value?.data?.first ?: "",
                text = updateMfaResponse.value?.data?.second ?: "",
                onDismissButtonClick = {
                    profileViewModel.updateMfaSettingResponse.value = null
                },
                dismissTitle = stringResource(R.string.close)
            )
        }

        UploadPhotoDialog(
            isOpen = pickAvatarDialogVisible.value,
            getPhotoUri = { profileViewModel.getPhotoUri(context) },
            onDismiss = { pickAvatarDialogVisible.value = false },
            onNavigateToAlbums = {
                pickAvatarDialogVisible.value = false
                navController.navigate("pick_avatar")
            },
            onTakePhoto = {
                pickAvatarDialogVisible.value = false
                profileViewModel.setTakePhoto()
            }
        )
    }
}

@Composable
fun HeaderProfile(onClickSave: () -> Unit, onCloseView: () -> Unit) {
    val focusManager = LocalFocusManager.current

    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(32.sdp()))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cross),
                contentDescription = null, modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    },
                alignment = Alignment.CenterStart,
                colorFilter = LocalColorMapping.current.closeIconFilter
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CKTextButton(
                    title = stringResource(R.string.save),
                    onClick = {
                        focusManager.clearFocus()
                        onClickSave.invoke()
                    },
                    fontSize = 16.sdp().toNonScalableTextSize(),
                    textButtonType = TextButtonType.Blue
                )
            }
        }
        Spacer(modifier = Modifier.size(16.sdp()))
        CKHeaderText(
            stringResource(R.string.profile_settings),
            headerTextType = HeaderTextType.Medium
        )
        Spacer(modifier = Modifier.size(16.sdp()))
    }
}


@Composable
fun ItemInformationView(
    header: String, errorText: String = "",
    errorVisible: Boolean = false, content: @Composable RowScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = header, style = MaterialTheme.typography.body1.copy(
                color = LocalColorMapping.current.inputLabel,
                fontSize = defaultNonScalableTextSize(),
                fontWeight = FontWeight.Normal
            )
        )
        Row {
            content()
        }
        if (errorVisible) {
            Text(
                errorText,
                color = LocalColorMapping.current.error,
                fontSize = 12.sdp().toNonScalableTextSize()
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ItemInformationInput(
    modifier: Modifier = Modifier.fillMaxWidth(),
    textValue: String,
    enable: Boolean = true,
    hasError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    prefix: String = "",
    onValueChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.sdp(),
            if (hasError) LocalColorMapping.current.error else LocalColorMapping.current.textFieldBackgroundAlt
        ),
        modifier = modifier,
    ) {
        TextField(
            value = textValue,
            onValueChange = onValueChange,
            colors = TextFieldDefaults.textFieldColors(
                textColor = if (enable) LocalColorMapping.current.bodyTextAlt else LocalColorMapping.current.bodyTextDisabled,
                cursorColor = if (enable) LocalColorMapping.current.bodyTextAlt else LocalColorMapping.current.bodyTextDisabled,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                backgroundColor = if (hasError) LocalColorMapping.current.textFieldBackgroundAltError else LocalColorMapping.current.textFieldBackgroundAlt
            ),
            textStyle = MaterialTheme.typography.body1.copy(
                color = if (enable) LocalColorMapping.current.bodyTextAlt else LocalColorMapping.current.bodyTextDisabled,
                fontWeight = FontWeight.Normal,
                fontSize = defaultNonScalableTextSize()
            ),
            enabled = enable,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            ),
            visualTransformation = if (prefix.isNotBlank()) PrefixTransformation(prefix) else VisualTransformation.None,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    CKText(
                        placeholder, style = MaterialTheme.typography.body1.copy(
                            color = MaterialTheme.colors.onSecondary,
                            fontWeight = FontWeight.Normal,
                            fontSize = defaultNonScalableTextSize()
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun CopyLink(onCopied: () -> Unit) {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        SideBarLabel(
            text = stringResource(R.string.copy_profile_link),
            modifier = Modifier
                .weight(0.66f),
            fontSize = defaultNonScalableTextSize(),
            color = primaryDefault
        )
        Column(
            modifier = Modifier.clickable { },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { onCopied.invoke() }
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "",
                    tint = primaryDefault
                )
            }
        }
    }
}

@Composable
fun ChangePassword(onChangePassword: () -> Unit) {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        SideBarLabel(
            text = stringResource(R.string.change_password),
            modifier = Modifier
                .weight(0.66f),
            fontSize = defaultNonScalableTextSize(),
            color = primaryDefault
        )
        Column(
            modifier = Modifier.clickable { },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { onChangePassword.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "",
                    tint = primaryDefault
                )
            }
        }
    }
}

@Composable
fun TwoFaceAuthView(
    enabled: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(
                stringResource(R.string.two_factors_auth),
                modifier = Modifier.weight(0.66f)
            )
            Column(
                modifier = Modifier.clickable { },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextButton(onClick = { onCheckChange.invoke(!enabled) }) {
                    CKText(
                        text = if (enabled) {
                            stringResource(R.string.disable)
                        } else stringResource(
                            R.string.enable
                        ),
                        color = primaryDefault
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.two_factors_auth_description),
            style = MaterialTheme.typography.body1.copy(
                color = LocalColorMapping.current.descriptionText,
                fontSize = defaultNonScalableTextSize(),
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

class PrefixTransformation(private val prefix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val out = prefix + text.text
        val prefixOffset = prefix.length

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + prefixOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= prefixOffset - 1) return prefixOffset
                return offset - prefixOffset
            }
        }

        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}

private const val KEYBOARD_HIDE_DELAY_MILLIS = 500L