package com.clearkeep.features.chat.presentation.room

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.*
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.common.presentation.components.CKInsetTheme
import com.clearkeep.common.presentation.components.base.CKAlertDialog
import com.clearkeep.common.utilities.*
import com.clearkeep.features.chat.presentation.groupcreate.CreateGroupViewModel
import com.clearkeep.features.chat.presentation.groupcreate.EnterGroupNameScreen
import com.clearkeep.features.chat.presentation.groupinvite.AddMemberUIType
import com.clearkeep.features.chat.presentation.groupinvite.InviteGroupScreen
import com.clearkeep.features.chat.presentation.groupinvite.InviteGroupViewModel
import com.clearkeep.features.chat.presentation.groupremove.RemoveMemberScreen
import com.clearkeep.features.chat.presentation.room.imagepicker.ImagePickerScreen
import com.clearkeep.features.chat.presentation.room.photodetail.PhotoDetailScreen
import com.clearkeep.features.chat.presentation.room.roomdetail.GroupMemberScreen
import com.clearkeep.features.chat.presentation.room.roomdetail.RoomInfoScreen
import com.clearkeep.navigation.NavigationUtils
import com.clearkeep.navigation.NavigationUtils.restartToRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoomActivity : AppCompatActivity(), LifecycleObserver {
    private val roomViewModel: RoomViewModel by viewModels()
    private val createGroupViewModel: CreateGroupViewModel by viewModels()
    private val inviteGroupViewModel: InviteGroupViewModel by viewModels()

    lateinit var messageSharedPreferences: SharedPreferences
    private var addMemberReceiver: BroadcastReceiver? = null
    private var roomId: Long = 0
    private lateinit var domain: String
    private lateinit var clientId: String
    private var isNote: Boolean = false
    private var chatServiceIsStartInRoom = false
    lateinit var navController: NavHostController

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        messageSharedPreferences =
            getSharedPreferences("CK_SharePreference_Message", Context.MODE_PRIVATE)

        roomViewModel.isLogout.observe(this) {
            printlnCK("RoomActivity signOut $it")
            if (it) {
                roomViewModel.signOut()
            }
        }

        roomViewModel.shouldReLogin.observe(this) {
            if (it) {
                restartToRoot(this)
            }
        }

        val rootView = findViewById<View>(android.R.id.content).rootView
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val navigationWidthLeft = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).left
            val navigationWidthRight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).right

            val bottomPadding = if (imeHeight == 0) navigation else imeHeight
            rootView.setPadding(navigationWidthLeft, 0, navigationWidthRight, bottomPadding)
            insets
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerAddMemberReceiver()
        roomId = intent.getLongExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_GROUP_ID, 0)
        domain = intent.getStringExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_DOMAIN) ?: ""
        clientId = intent.getStringExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_CLIENT_ID) ?: ""
        isNote = intent.getBooleanExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_IS_NOTE, false)
        val clearTempMessage = intent.getBooleanExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_CLEAR_TEMP_MESSAGE, false)
        if (clearTempMessage) {
            roomViewModel.clearTempMessage()
        }
        val friendId = intent.getStringExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_FRIEND_ID) ?: ""
        val friendDomain = intent.getStringExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_FRIEND_DOMAIN) ?: ""

        if (roomId > 0) {
            val notificationManagerCompat =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerCompat.cancel(roomId.toInt())
        }
        roomViewModel.joinRoom(domain, clientId, roomId, friendId, friendDomain)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CKInsetTheme {
                navController = rememberNavController()
                val showing = roomViewModel.isShowDialogRemoved.observeAsState()
                val isMemberChangeKey = roomViewModel.isMemberChangeKey.observeAsState()

                if (showing.value == true) {
                    CKAlertDialog(
                        title = "",
                        text = "You have been removed from the conversation",
                        onDismissButtonClick = {
                            restartToRoot(context = this@RoomActivity)

                        }
                    )
                }
                if (isMemberChangeKey.value == true) {
                    val mIntent = intent
                    finish()
                    startActivity(mIntent)
                }

                val selectedItem = remember { mutableStateListOf<com.clearkeep.domain.model.User>() }
                NavHost(navController, startDestination = "room_screen") {
                    composable("room_screen") {
                        LaunchedEffect(true) {
                            roomViewModel.setMessage(
                                messageSharedPreferences.getString(
                                    "$roomId+$clientId+$domain",
                                    ""
                                ).orEmpty()
                            )
                        }

                        RoomScreen(
                            roomViewModel,
                            navController,
                            onFinishActivity = {
                                roomViewModel.message.value?.let { it -> saveDrafMessage(it) }
                                finish()
                            },
                            onCallingClick = { isPeer ->
                                NavigationUtils.navigateToAvailableCall(this@RoomActivity, isPeer)
                            }
                        )
                    }
                    composable("room_info_screen") {
                        RoomInfoScreen(
                            roomViewModel,
                            navController
                        )
                    }
                    composable("invite_group_screen") {
                        selectedItem.clear()
                        inviteGroupViewModel.updateContactList()
                        InviteGroupScreen(
                            AddMemberUIType,
                            inviteGroupViewModel,
                            selectedItem = selectedItem,
                            chatGroup = roomViewModel.group,
                            onFriendSelected = { friends ->
                                roomViewModel.inviteToGroup(friends, groupId = roomId)
                                navController.navigate("room_screen")
                            },
                            onDirectFriendSelected = { },
                            onBackPressed = {
                                navController.popBackStack()
                            },
                            isCreateDirectGroup = false
                        )
                    }
                    composable("member_group_screen") {
                        GroupMemberScreen(
                            roomViewModel,
                            navController
                        )
                    }
                    composable("enter_group_name") {
                        EnterGroupNameScreen(
                            navController,
                            createGroupViewModel,
                        )
                    }
                    composable("remove_member") {
                        RemoveMemberScreen(
                            roomViewModel,
                            navController
                        )
                    }
                    composable("image_picker") {
                        ImagePickerScreen(
                            roomViewModel.imageUriSelected,
                            navController,
                            onSetSelectedImages = {
                                roomViewModel.setSelectedImages(it)
                            }
                        )
                    }
                    composable("photo_detail") {
                        PhotoDetailScreen(
                            roomViewModel,
                        ) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
        subscriber()
    }

    private fun saveDrafMessage(message: String) {
        messageSharedPreferences.edit().putString("$roomId+$clientId+$domain", message).apply()
    }

    private fun registerAddMemberReceiver() {
        addMemberReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1)
                when (intent.action) {
                    ACTION_MEMBER_CHANGE_KEY -> {
                        if (roomViewModel.group.value?.isGroup() == false) {
                            val refClientId = intent.getStringExtra(EXTRA_ID_MEMBER_CHANGE_KEY)
                            val friend = roomViewModel.group.value?.clientList?.firstOrNull { client ->
                                client.userId != roomViewModel.clientId
                            }
                            if (friend?.userId == refClientId) {
                                roomViewModel.isMemberChangeKey.postValue(true)
                            }
                        }
                    }
                    else -> {
                        if (roomId == groupId) {
                            roomViewModel.refreshRoom()
                        }
                    }
                }
            }
        }
        registerReceiver(
            addMemberReceiver,
            IntentFilter(ACTION_ADD_REMOVE_MEMBER)
        )
        registerReceiver(
            addMemberReceiver,
            IntentFilter(ACTION_MEMBER_CHANGE_KEY)
        )
    }


    private fun unRegisterAddMemberReceiver() {
        if (addMemberReceiver != null) {
            unregisterReceiver(addMemberReceiver)
            addMemberReceiver = null
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val newRoomId = intent.getLongExtra(NavigationUtils.NAVIGATE_ROOM_ACTIVITY_GROUP_ID, 0)
        printlnCK("onNewIntent, $newRoomId")
        if (newRoomId > 0 && newRoomId != roomId) {
            finish()
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        roomViewModel.leaveRoom()
        unRegisterAddMemberReceiver()
        super.onDestroy()
    }

    private fun subscriber() {
        roomViewModel.requestCallState.observe(this, Observer {
            if (it != null && it.status == com.clearkeep.common.utilities.network.Status.SUCCESS) {
                it.data?.let { requestInfo ->
                    NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)
                    navigateToInComingCallActivity(
                        requestInfo.chatGroup,
                        requestInfo.isAudioMode
                    )
                }
                roomViewModel.requestCallState.value = null
            }
        })

        if (roomId > 0)
            roomViewModel.groups.observe(this, Observer {
                val group =
                    it.find { group -> group.groupId == roomId && group.ownerDomain == domain }
                if (group == null) {
                    roomViewModel.isShowDialogRemoved.postValue(true)
                }
            })
    }

    private fun navigateToInComingCallActivity(group: com.clearkeep.domain.model.ChatGroup, isAudioMode: Boolean) {
        val roomName = if (group.isGroup()) group.groupName else {
            group.clientList.firstOrNull { client ->
                client.userId != clientId
            }?.userName ?: ""
        }
        NavigationUtils.navigateToCall(
            this,
            isAudioMode,
            null,
            group.groupId.toString(),
            group.groupType,
            roomName,
            domain,
            clientId,
            roomName,
            roomViewModel.getUserAvatarUrl(),
            false,
            currentUserName = roomViewModel.getUserName(),
            currentUserAvatar = roomViewModel.getSelfAvatarUrl()
        )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        if (chatServiceIsStartInRoom) {
            printlnCK("CK room go into background")
            stopChatService()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        printlnCK("CK room go into foregrounded")
        if (!checkServiceRunning(NavigationUtils.getChatServiceRef())) {
            chatServiceIsStartInRoom = true
            startChatService()
        }
    }

    private fun startChatService() {
        NavigationUtils.startChatService(this)
    }

    private fun stopChatService() {
        NavigationUtils.stopChatService(this)
    }


    /**
     * Check if the service is Running
     * @param serviceClass the class of the Service
     *
     * @return true if the service is running otherwise false
     */
    private fun checkServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}