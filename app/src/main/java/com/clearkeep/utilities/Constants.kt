package com.clearkeep.utilities

import com.clearkeep.BuildConfig

const val FIREBASE_TOKEN = "ck_firebase_token"

//const val BASE_URL = "172.16.6.34"
//const val BASE_URL = "172.16.6.232"
//const val BASE_URL = "10.0.255.71"
const val BASE_URL = BuildConfig.BASE_URL
const val PORT = BuildConfig.PORT

// janus
const val JANUS_URI = "ws://${BuildConfig.BASE_JANUS_URL}:${BuildConfig.JANUS_PORT}/janus"
const val EXTRA_OUR_CLIENT_ID = "to"
const val EXTRA_GROUP_ID = "call_id"
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

const val ACTION_CALL_SERVICE_AVAILABLE_STATE_CHANGED = "ck.action.end.service.call"
const val EXTRA_SERVICE_IS_AVAILABLE = "call_service_is_available"

const val ACTION_CALL_CANCEL = "ck.action.end.call"
const val EXTRA_CALL_CANCEL_GROUP_ID = "call_cancel_group_id"
const val EXTRA_CALL_CANCEL_GROUP_TYPE = "call_cancel_group_type"

const val ACTION_CALL_SWITCH_VIDEO = "ck.action.switch.mode"
const val EXTRA_CALL_SWITCH_VIDEO = "call_switch_mode"

const val INCOMING_NOTIFICATION_ID = 123456
const val INCOMING_CHANNEL_ID = "incoming_channel_id"
const val INCOMING_CHANNEL_NAME = "incoming_channel_name"

const val CALL_TYPE_AUDIO = "audio"
const val CALL_TYPE_VIDEO = "video"