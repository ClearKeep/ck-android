package com.clearkeep.januswrapper.utils

object Constants {
    const val JANUS_URI = "ws://172.16.1.214:8188/janus"
    const val REQUEST = "request"
    const val MESSAGE = "message"
    const val PUBLISHERS = "publishers"
    const val EXTRA_OUR_CLIENT_ID = "to"
    const val EXTRA_GROUP_ID = "call_id"
    const val EXTRA_USER_NAME = "user_name"
    const val EXTRA_FROM_IN_COMING_CALL = "is_coming_call"
    const val EXTRA_AVATAR_USER_IN_CONVERSATION = "avatar_user_in_conversation"

    const val ACTION_CALL_SERVICE_AVAILABLE_STATE_CHANGED = "ck.action.end.service.call"
    const val EXTRA_SERVICE_IS_AVAILABLE = "call_service_is_available"
}