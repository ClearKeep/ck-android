package com.clearkeep.utilities

import com.clearkeep.BuildConfig

const val FIREBASE_TOKEN = "ck_firebase_token"

//const val BASE_URL = "172.16.6.34"
//const val BASE_URL = "172.16.6.232"
//const val BASE_URL = "10.0.255.71"
const val BASE_URL = BuildConfig.BASE_URL
const val PORT = BuildConfig.PORT
//54.235.68.160:15000
//54.235.68.160:25000

// janus
const val EXTRA_OWNER_CLIENT = "to"
const val EXTRA_GROUP_ID = "call_id"
const val EXTRA_OWNER_DOMAIN = "owner_domain"
const val EXTRA_GROUP_NAME = "group_name"
const val EXTRA_GROUP_TYPE = "group_type"
const val EXTRA_IS_AUDIO_MODE = "is_audio_mode"
const val EXTRA_USER_NAME = "user_name"
const val EXTRA_FROM_IN_COMING_CALL = "is_coming_call"
const val EXTRA_AVATAR_USER_IN_CONVERSATION = "avatar_user_in_conversation"
const val EXTRA_GROUP_TOKEN = "group_token"
const val EXTRA_TURN_USER_NAME = "turn_user_name"
const val EXTRA_TURN_PASS = "turn_pass"
const val EXTRA_TURN_URL = "turn_url"
const val EXTRA_STUN_URL = "stun_url"
const val EXTRA_WEB_RTC_GROUP_ID = "webrtc_group"
const val EXTRA_WEB_RTC_URL = "webrtc_url"
const val EXTRA_CURRENT_USERNAME = "current_username"
const val EXTRA_CURRENT_USER_AVATAR = "current_user_avatar"

const val ACTION_CALL_SERVICE_AVAILABLE_STATE_CHANGED = "ck.action.end.service.call"
const val EXTRA_SERVICE_IS_AVAILABLE = "call_service_is_available"

const val ACTION_CALL_CANCEL = "ck.action.end.call"
const val ACTION_CALL_BUSY = "ck.action.busy.call"
const val EXTRA_CALL_CANCEL_GROUP_ID = "call_cancel_group_id"
const val EXTRA_CALL_CANCEL_GROUP_TYPE = "call_cancel_group_type"

const val ACTION_CALL_SWITCH_VIDEO = "ck.action.switch.mode"
const val ACTION_REMOVE_MEMBER = "ck.action.remove.member"
const val ACTION_ADD_REMOVE_MEMBER = "ck.action.add.member"
const val EXTRA_CALL_SWITCH_VIDEO = "call_switch_mode"

const val INCOMING_NOTIFICATION_ID = 123456
const val MESSAGE_NOTIFICATION_ID = 123456789

const val INCOMING_CHANNEL_ID = "incoming_channel_id"
const val INCOMING_CHANNEL_NAME = "incoming_channel_name"

const val MESSAGE_HEADS_UP_CHANNEL_ID = "channel_heads_up_message"
const val MESSAGE_HEADS_UP_CHANNEL_NAME = "channel_name_heads_up"
const val MESSAGE_HEADS_UP_CANCEL_NOTIFICATION_ID = "message_heads_up_cancel_id"
const val MESSAGE_HEADS_UP_NOTIFICATION_ID = 100

const val MESSAGE_CHANNEL_ID = "message_channel_id_ck"
const val MESSAGE_CHANNEL_NAME = "message_channel_name_ck"

const val CALL_CHANNEL_ID = "channel_call"
const val CALL_CHANNEL_NAME = "Calls"
const val CALL_NOTIFICATION_ID = 1

const val ACTION_MESSAGE_CANCEL = "ck.action_message_cancel"
const val ACTION_MESSAGE_REPLY = "ck.action_message_reply"

const val CALL_TYPE_AUDIO = "audio"
const val CALL_TYPE_VIDEO = "video"
const val CALL_UPDATE_TYPE_CANCEL = "cancel_request_call"
const val CALL_UPDATE_TYPE_BUSY = "busy_request_call"

const val ERROR_CODE_TIMEOUT = 504
const val REQUEST_DEADLINE_SECONDS = 30L