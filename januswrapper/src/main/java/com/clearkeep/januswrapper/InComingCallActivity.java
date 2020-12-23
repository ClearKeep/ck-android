package com.clearkeep.januswrapper;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.clearkeep.januswrapper.common.AvatarImageTask;
import de.hdodenhof.circleimageview.CircleImageView;
import static com.clearkeep.januswrapper.utils.Constants.*;

public class InComingCallActivity extends Activity implements View.OnClickListener {
    private String mUserNameInConversation;
    private String mAvatarInConversation;
    private Long mGroupId;
    private String mReceiverId;

    private ImageView imgAnswer;
    private ImageView imgEnd;
    private CircleImageView imgThumb;
    private TextView tvUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_coming_call);

        mUserNameInConversation = getIntent().getStringExtra(EXTRA_USER_NAME);
        mAvatarInConversation = getIntent().getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION);
        mGroupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 0);
        mReceiverId = getIntent().getStringExtra(EXTRA_OUR_CLIENT_ID);

        initViews();
    }

    private void initViews() {
        ImageView imgBack = findViewById(R.id.imgBack);
        imgAnswer = findViewById(R.id.imgAnswer);
        imgEnd = findViewById(R.id.imgEnd);
        imgThumb = findViewById(R.id.imgThumb);
        tvUserName = findViewById(R.id.tvUserName);
        imgBack.setOnClickListener(this);
        imgAnswer.setOnClickListener(this);
        imgEnd.setOnClickListener(this);

        updateConversationInformation();
    }

    private void updateConversationInformation() {
        if (!TextUtils.isEmpty(mUserNameInConversation)) {
            tvUserName.setText(mUserNameInConversation);
        }
        if (!TextUtils.isEmpty(mAvatarInConversation)) {
            new AvatarImageTask(imgThumb).execute(mAvatarInConversation);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imgEnd) {
            finishAndRemoveFromTask();
        } else if (id == R.id.imgBack) {
            finishAndRemoveFromTask();
        } else if (id == R.id.imgAnswer) {
            AppCall.call(this, mGroupId, mReceiverId, "Dai", "", true);
        }
    }

    private void finishAndRemoveFromTask() {
        if(Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
