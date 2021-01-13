package com.clearkeep.januswrapper

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.clearkeep.januswrapper.common.AvatarImageTask
import com.clearkeep.januswrapper.utils.Constants
import de.hdodenhof.circleimageview.CircleImageView

class InComingCallActivity : Activity(), View.OnClickListener {
    private var mUserNameInConversation: String? = null
    private var mAvatarInConversation: String? = null
    private var mGroupId: Long? = null
    private var mReceiverId: String? = null
    private lateinit var mToken: String
    private lateinit var imgAnswer: ImageView
    private lateinit var imgEnd: ImageView
    private lateinit var imgThumb: CircleImageView
    private lateinit var tvUserName: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_coming_call)
        mUserNameInConversation = intent.getStringExtra(Constants.EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION)
        mGroupId = intent.getLongExtra(Constants.EXTRA_GROUP_ID, 0)
        mReceiverId = intent.getStringExtra(Constants.EXTRA_OUR_CLIENT_ID)
        mToken = intent.getStringExtra(Constants.EXTRA_GROUP_TOKEN)!!
        initViews()
    }

    private fun initViews() {
        val imgBack = findViewById<ImageView>(R.id.imgBack)
        imgAnswer = findViewById(R.id.imgAnswer)
        imgEnd = findViewById(R.id.imgEnd)
        imgThumb = findViewById(R.id.imgThumb)
        tvUserName = findViewById(R.id.tvUserName)
        imgBack.setOnClickListener(this)
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
            R.id.imgBack -> {
                finishAndRemoveFromTask()
            }
            R.id.imgAnswer -> {
                finishAndRemoveFromTask()
                AppCall.call(this, mToken, mGroupId, mReceiverId, mUserNameInConversation, mAvatarInConversation, true)
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