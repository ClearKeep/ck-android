package com.clearkeep.screen.videojanus

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.clearkeep.R
import com.clearkeep.screen.videojanus.common.AvatarImageTask
import com.clearkeep.utilities.*
import de.hdodenhof.circleimageview.CircleImageView

class InComingCallActivity : Activity(), View.OnClickListener {
    private var mUserNameInConversation: String? = null
    private var mAvatarInConversation: String? = null
    private var mGroupId: String? = null
    private var mReceiverId: String? = null
    private lateinit var mToken: String
    private lateinit var imgAnswer: ImageView
    private lateinit var imgEnd: ImageView
    private lateinit var imgThumb: CircleImageView
    private lateinit var tvUserName: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_coming_call)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        mGroupId = intent.getStringExtra(EXTRA_GROUP_ID)
        mReceiverId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID)
        mToken = intent.getStringExtra(EXTRA_GROUP_TOKEN)!!
        initViews()
    }

    private fun initViews() {
        imgAnswer = findViewById(R.id.imgAnswer)
        imgEnd = findViewById(R.id.imgEnd)
        imgThumb = findViewById(R.id.imgThumb)
        tvUserName = findViewById(R.id.tvUserName)
        imgAnswer.setOnClickListener(this)
        imgEnd.setOnClickListener(this)
        updateConversationInformation()
    }

    private fun updateConversationInformation() {
        if (!TextUtils.isEmpty(mUserNameInConversation)) {
            tvUserName.text = mUserNameInConversation
        }
        if (!TextUtils.isEmpty(mAvatarInConversation)) {
            AvatarImageTask(imgThumb).execute(mAvatarInConversation)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgEnd -> {
                finishAndRemoveFromTask()
            }
            R.id.imgAnswer -> {
                val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
                finishAndRemoveFromTask()
                AppCall.call(this, mToken, mGroupId, mReceiverId, mUserNameInConversation, mAvatarInConversation, true,
                        turnUrl = turnUrl, turnUser =  turnUserName, turnPass = turnPassword,
                        stunUrl = stunUrl
                )
            }
        }
    }

    private fun finishAndRemoveFromTask() {
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }
}