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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import com.clearkeep.BuildConfig
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.screen.chat.composes.CircleAvatar
import com.clearkeep.screen.chat.home.composes.SideBarLabel
import com.clearkeep.screen.chat.room.UploadPhotoDialog
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    BackHandler {
        onCloseView()
    }

    profile?.value?.let { user ->
        val userName = profileViewModel.username.observeAsState()
        val userNameErrorVisible = profileViewModel.usernameError.observeAsState()
        val email = profileViewModel.email.observeAsState()
        val phoneNumber = profileViewModel.phoneNumber.observeAsState()
        val phoneNumberErrorVisible = profileViewModel.phoneNumberError.observeAsState()
        val countryCode = profileViewModel.countryCode.observeAsState()
        val otpErrorDialogVisible = remember { mutableStateOf(false) }
        val pickAvatarDialogVisible = remember { mutableStateOf(false) }
        val unsavedChangesDialogVisible =
            profileViewModel.unsavedChangeDialogVisible.observeAsState()
        val uploadAvatarResponse = profileViewModel.uploadAvatarResponse.observeAsState()
        val updateMfaResponse = profileViewModel.updateMfaSettingResponse.observeAsState()
        val selectedAvatar = profileViewModel.imageUriSelected.observeAsState()
        val userPreference = profileViewModel.userPreference.observeAsState()

        printlnCK("ProfileScreen userPreference ${userPreference.value?.mfa ?: "null"}")

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
                    profile?.value?.let { user ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
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
                                    },
                                    user.userName ?: "",
                                    size = 72.dp,
                                    modifier = Modifier.clickable {
                                        pickAvatarDialogVisible.value = true
                                    },
                                    cacheKey = user.updatedAt.toString()
                                )
                                Column(
                                    Modifier.padding(start = 16.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SideBarLabel(
                                        text = "Change profile picture",
                                        color = primaryDefault,
                                        fontSize = 14.sp,
                                    )
                                    SideBarLabel(
                                        text = " Maximum fize size 5MB",
                                        color = grayscale3,
                                        modifier = Modifier,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            ItemInformationView(
                                "Username",
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
                            Spacer(Modifier.height(16.dp))
                            ItemInformationView("Email") {
                                ItemInformationInput(
                                    textValue = email.value ?: "",
                                    enable = false
                                ) {
                                    profileViewModel.setEmail(it)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            ItemInformationView(
                                "Phone Number",
                                errorText = stringResource(R.string.profile_phone_number_error),
                                errorVisible = phoneNumberErrorVisible.value ?: false
                            ) {
                                Box(
                                    Modifier
                                        .weight(1.3f)
                                        .height(60.dp)
                                        .fillMaxWidth()
                                        .background(
                                            if (phoneNumberErrorVisible.value == true) grayscaleOffWhite else grayscale5,
                                            MaterialTheme.shapes.large
                                        )
                                        .clip(MaterialTheme.shapes.large)
                                        .clickable {
                                            keyboardController?.hide()
                                            navController.navigate("country_code_picker")
                                        }
                                        .then(
                                            if (phoneNumberErrorVisible.value == true) Modifier.border(
                                                1.dp,
                                                errorDefault,
                                                MaterialTheme.shapes.large
                                            ) else Modifier
                                        )
                                ) {
                                    val countryCode = countryCode.value ?: ""
                                    Text(
                                        countryCode,
                                        Modifier
                                            .padding(start = 12.dp)
                                            .align(Alignment.CenterStart)
                                            .fillMaxWidth(),
                                        style = MaterialTheme.typography.body1.copy(
                                            color = grayscaleBlack,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                    Image(
                                        painterResource(R.drawable.ic_chev_down),
                                        null,
                                        Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 11.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                ItemInformationInput(
                                    Modifier
                                        .weight(3f)
                                        .fillMaxSize(),
                                    hasError = phoneNumberErrorVisible.value ?: false,
                                    textValue = phoneNumber.value ?: "",
                                    keyboardType = KeyboardType.Number,
                                    placeholder = "Phone number",
                                ) {
                                    profileViewModel.setPhoneNumber(it)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            CopyLink(
                                onCopied = {
                                    onCopyToClipBoard()
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            ChangePassword(onChangePassword)
                            Spacer(Modifier.height(24.dp))
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
                            Spacer(Modifier.height(24.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 8.dp, bottom = 20.dp),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "version $versionName (${env.toUpperCase()})",
                                    style = MaterialTheme.typography.caption.copy(
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
                title = uploadAvatarResponse.value!!.message ?: "",
                onDismissButtonClick = {
                    profileViewModel.uploadAvatarResponse.value = null
                },
            )
        }
        if (unsavedChangesDialogVisible.value == true) {
            CKAlertDialog(
                title = stringResource(R.string.profile_unsaved_change_warning),
                dismissTitle = "Stay on this page",
                confirmTitle = "Leave this page",
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
            )
        }

        UploadPhotoDialog(
            isOpen = pickAvatarDialogVisible.value,
            getPhotoUri = { profileViewModel.getPhotoUri(context) },
            onDismiss = { pickAvatarDialogVisible.value = false },
            onNavigateToAlbums = { navController.navigate("pick_avatar") },
            onTakePhoto = {
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
        Spacer(Modifier.size(32.dp))
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
                alignment = Alignment.CenterStart
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CKTextButton(
                    title = "Save",
                    onClick = {
                        focusManager.clearFocus()
                        onClickSave.invoke()
                    },
                    fontSize = 16.sp,
                    textButtonType = TextButtonType.Blue
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        CKHeaderText("Profile Settings", headerTextType = HeaderTextType.Medium)
        Spacer(modifier = Modifier.size(16.dp))
    }
}


@ExperimentalComposeUiApi
@Composable
fun ItemInformationView(
    header: String, errorText: String = "",
    errorVisible: Boolean = false, content: @Composable RowScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = header, style = MaterialTheme.typography.body1.copy(
                color = grayscale1,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        )
        Row(Modifier.background(grayscaleOffWhite)) {
            content()
        }
        if (errorVisible) {
            Text(errorText, color = errorDefault, fontSize = 12.sp)
        }
    }
}

@ExperimentalComposeUiApi
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
        border = BorderStroke(1.dp, if (hasError) errorDefault else grayscale5),
        modifier = modifier
    ) {
        TextField(
            value = textValue,
            onValueChange = onValueChange,
            colors = TextFieldDefaults.textFieldColors(
                textColor = if (enable) grayscaleBlack else grayscale3,
                cursorColor = if (enable) grayscaleBlack else grayscale3,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                backgroundColor = if (hasError) grayscaleOffWhite else grayscale5
            ),
            textStyle = MaterialTheme.typography.body1.copy(
                color = if (enable) grayscaleBlack else grayscale3,
                fontWeight = FontWeight.Normal
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
                    Text(
                        placeholder, style = MaterialTheme.typography.body1.copy(
                            color = MaterialTheme.colors.onSecondary,
                            fontWeight = FontWeight.Normal
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
            text = "Copy profile link", modifier = Modifier
                .weight(0.66f), fontSize = 14.sp, color = MaterialTheme.colors.primary
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
            text = "Change Password", modifier = Modifier
                .weight(0.66f), fontSize = 14.sp, color = MaterialTheme.colors.primary
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
                    Text(text = if (enabled) "Disable" else "Enable")
                }
            }
        }
        Text(
            text = stringResource(R.string.two_factors_auth_description),
            style = MaterialTheme.typography.body1.copy(
                color = grayscale2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

class PrefixTransformation(val prefix: String) : VisualTransformation {
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