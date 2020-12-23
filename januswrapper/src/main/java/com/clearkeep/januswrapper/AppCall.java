package com.clearkeep.januswrapper;

import android.content.Context;
import android.content.Intent;
import static com.clearkeep.januswrapper.utils.Constants.*;

public class AppCall {

    public static void call(Context context, Long groupId, String ourClientId, String userName, String avatar, boolean isIncomingCall) {
        Intent intent = new Intent(context, InCallActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, isIncomingCall);
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void inComingCall(Context context, Long groupId, String ourClientId, String userName, String avatar) {
        Intent intent = new Intent(context, InComingCallActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
