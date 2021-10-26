package com.clearkeep.screen.chat.room

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.*
import androidx.core.view.WindowCompat
import com.clearkeep.components.CKInsetTheme
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.group_create.CreateGroupViewModel
import com.clearkeep.screen.chat.group_create.EnterGroupNameScreen
import com.clearkeep.screen.chat.group_invite.AddMemberUIType
import com.clearkeep.screen.chat.group_invite.InviteGroupScreen
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.chat.group_invite.InviteGroupViewModel
import com.clearkeep.screen.chat.group_remove.RemoveMemberScreen
import com.clearkeep.screen.chat.room.image_picker.ImagePickerScreen
import com.clearkeep.screen.chat.room.room_detail.GroupMemberScreen
import com.clearkeep.screen.chat.room.room_detail.RoomInfoScreen
import com.clearkeep.services.ChatService
import com.clearkeep.utilities.network.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.app.ActivityManager
import androidx.compose.material.ExperimentalMaterialApi
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.compose.*
import com.clearkeep.screen.chat.room.photo_detail.PhotoDetailScreen
import com.clearkeep.utilities.*


@AndroidEntryPoint
class RoomActivity : AppCompatActivity(), LifecycleObserver {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val roomViewModel: RoomViewModel by viewModels {
        viewModelFactory
    }
    private val createGroupViewModel: CreateGroupViewModel by viewModels {
        viewModelFactory
    }

    private val inviteGroupViewModel: InviteGroupViewModel by viewModels {
        viewModelFactory
    }

    private var addMemberReceiver: BroadcastReceiver? = null

    private var roomId: Long = 0
    private lateinit var domain: String
    private lateinit var clientId: String
    private var isNote: Boolean = false
    private var chatServiceIsStartInRoom = false

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        roomViewModel.isLogout.observe(this) {
            printlnCK("RoomActivity signOut $it")
            if (it) {
                roomViewModel.signOut()
            }
        }

        roomViewModel.isLogOutCompleted.observe(this) {
            printlnCK("RoomActivity isLogoutCompleted $it")
            if (it) {
                restartToRoot(this)
            }
        }

        val rootView = findViewById<View>(android.R.id.content).rootView
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomPadding = if (imeHeight == 0) navigation else imeHeight
            rootView.setPadding(0, 0, 0, bottomPadding)
            insets
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerAddMemberReceiver()
        roomId = intent.getLongExtra(GROUP_ID, 0)
        domain = intent.getStringExtra(DOMAIN) ?: ""
        clientId = intent.getStringExtra(CLIENT_ID) ?: ""
        isNote = intent.getBooleanExtra(IS_NOTE, false)
        val clearTempMessage = intent.getBooleanExtra(CLEAR_TEMP_MESSAGE, false)
        if (clearTempMessage) {
            roomViewModel.clearTempMessage()
        }
        val friendId = intent.getStringExtra(FRIEND_ID) ?: ""
        val friendDomain = intent.getStringExtra(FRIEND_DOMAIN) ?: ""

        if (roomId > 0) {
            val notificationManagerCompat =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerCompat.cancel(roomId.toInt())
        }
        if (isNote) {
            roomViewModel.initNotes(domain, clientId)
        } else {
            roomViewModel.joinRoom(domain, clientId, roomId, friendId, friendDomain)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CKInsetTheme {
                val navController = rememberNavController()
                val selectedItem = remember { mutableStateListOf<User>() }
                NavHost(navController, startDestination = "room_screen") {
                    composable("room_screen") {
                        RoomScreen(
                            roomViewModel,
                            navController,
                            onFinishActivity = {
                                finish()
                            },
                            onCallingClick = { isPeer ->
                                AppCall.openCallAvailable(this@RoomActivity, isPeer)
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

    private fun registerAddMemberReceiver() {
        addMemberReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getLongExtra(EXTRA_GROUP_ID,-1)
                if (roomId == groupId) {
                    roomViewModel.refreshRoom()
                }
            }
        }
        registerReceiver(
            addMemberReceiver,
            IntentFilter(ACTION_ADD_REMOVE_MEMBER)
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
        val newRoomId = intent.getLongExtra(GROUP_ID, 0)
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
            if (it != null && it.status == Status.SUCCESS) {
                it.data?.let { requestInfo ->
                    NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)
                    navigateToInComingCallActivity(
                        requestInfo.chatGroup,
                        requestInfo.isAudioMode
                    )
                }
            }
        })

        if (roomId > 0)
            roomViewModel.groups.observe(this, Observer {
                val group = it.find { group -> group.groupId == roomId && group.ownerDomain == domain }
                if (group == null) finish()
            })
    }

    private fun navigateToInComingCallActivity(group: ChatGroup, isAudioMode: Boolean) {
        val roomName = if (group.isGroup()) group.groupName else {
            group.clientList.firstOrNull { client ->
                client.userId != clientId
            }?.userName ?: ""
        }
        AppCall.call(
            this, isAudioMode, null,
            group.groupId.toString(), group.groupType, roomName,
            domain, clientId, roomName, "", false, currentUserName = roomViewModel.getUserName(), currentUserAvatar = roomViewModel.getUserAvatarUrl()
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
        if (!checkServiceRunning(ChatService::class.java)) {
            chatServiceIsStartInRoom = true
            startChatService()
        }
    }

    private fun startChatService() {
        Intent(this, ChatService::class.java).also { intent ->
            startService(intent)
        }
    }

    private fun stopChatService() {
        Intent(this, ChatService::class.java).also { intent ->
            stopService(intent)
        }
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


    companion object {
        const val GROUP_ID = "room_id"
        const val DOMAIN = "domain"
        const val CLIENT_ID = "client_id"
        const val FRIEND_ID = "remote_id"
        const val FRIEND_DOMAIN = "remote_domain"
        const val IS_NOTE = "is_note"
        const val CLEAR_TEMP_MESSAGE = "clear_temp_message"
    }
}