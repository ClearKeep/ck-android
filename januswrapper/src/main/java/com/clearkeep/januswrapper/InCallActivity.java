package com.clearkeep.januswrapper;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.clearkeep.januswrapper.common.AvatarImageTask;
import com.clearkeep.januswrapper.services.InCallForegroundService;
import com.clearkeep.januswrapper.utils.Utils;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoTrack;
import java.math.BigInteger;
import de.hdodenhof.circleimageview.CircleImageView;
import static com.clearkeep.januswrapper.utils.Constants.*;

public class InCallActivity extends Activity implements View.OnClickListener, InCallForegroundService.CallListener {

    private static final int REQUEST_PERMISSIONS = 1;

    private boolean mBound = false;
    private boolean mFromComingCall;
    private InCallForegroundService mService;
    private String mUserNameInConversation;
    private String mAvatarInConversation;

    private ImageView imgAnswer;
    private ImageView imgEnd;
    private TextView tvEndButtonDescription;
    private CircleImageView imgThumb;
    private TextView tvUserName;
    private TextView tvCallState;
    private View answerButtonContainer;
    private Chronometer mChronometer;

    private GLSurfaceView surfaceView;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private boolean mIsSurfaceCreated = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            InCallForegroundService.LocalBinder binder = (InCallForegroundService.LocalBinder) service;
            mService = binder.getService();
            mService.registerCallListener(InCallActivity.this);
            mBound = true;

            updateMediaUI();
            executeCall();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_call);

        mFromComingCall = getIntent().getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false);
        mUserNameInConversation = getIntent().getStringExtra(EXTRA_USER_NAME);
        mAvatarInConversation = getIntent().getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION);

        initViews();
        requestCallPermissions();
    }

    void requestCallPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA,
                                Manifest.permission.MODIFY_AUDIO_SETTINGS
                        },
                        REQUEST_PERMISSIONS
                );
                return;
            }
        }
        onCallPermissionsAvailable();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS &&
                (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            onCallPermissionsAvailable();
        } else {
            finishAndRemoveFromTask();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChronometer.getVisibility() == View.VISIBLE) {
            mChronometer.stop();
        }
        unBindCallService();
    }

    void onCallPermissionsAvailable() {
        if (!Utils.isServiceRunning(this, InCallForegroundService.class.getName())) {
            startServiceAsForeground();
        }
        bindCallService();
    }

    private void initViews() {
        ImageView imgBack = findViewById(R.id.imgBack);
        imgAnswer = findViewById(R.id.imgAnswer);
        imgEnd = findViewById(R.id.imgEnd);
        tvEndButtonDescription = findViewById(R.id.tvEndButtonDescription);
        imgThumb = findViewById(R.id.imgThumb);
        tvCallState = findViewById(R.id.tvCallState);
        answerButtonContainer = findViewById(R.id.answerButtonContainer);
        tvUserName = findViewById(R.id.tvUserName);
        mChronometer = findViewById(R.id.chronometer);
        imgBack.setOnClickListener(this);
        imgAnswer.setOnClickListener(this);
        imgEnd.setOnClickListener(this);

        displayUiAsInConversation();
        updateConversationInformation();

        surfaceView = findViewById(R.id.glview);
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setKeepScreenOn(true);

        VideoRendererGui.setView(surfaceView, () -> {
            mIsSurfaceCreated = true;
            executeCall();
        });

        localRender = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        remoteRender = VideoRendererGui.create(27, 27, 70, 70, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
    }

    private void executeCall() {
        if (mIsSurfaceCreated && mBound) {
            EGLContext con = VideoRendererGui.getEGLContext();
            mService.executeMakeCall(con);
        }
    }

    private void updateConversationInformation() {
        if (!TextUtils.isEmpty(mUserNameInConversation)) {
            tvUserName.setText(mUserNameInConversation);
        }
        if (!TextUtils.isEmpty(mAvatarInConversation)) {
            new AvatarImageTask(imgThumb).execute(mAvatarInConversation);
        }
    }

    private void updateMediaUI() {
        if (mService != null) {
            InCallForegroundService.CallState currentState = mService.getCurrentState();
            if (InCallForegroundService.CallState.ANSWERED.equals(currentState)) {
                displayUiAsInConversation();
            }
        }
    }

    private void displayUiAsInConversation() {
        answerButtonContainer.setVisibility(View.GONE);
        tvEndButtonDescription.setVisibility(View.GONE);
    }

    private void displayCountUpClockOfConversation() {
        mChronometer.setVisibility(View.VISIBLE);
        tvCallState.setVisibility(View.GONE);
    }

    public void onCallStateChanged(String status, InCallForegroundService.CallState callState) {
        if (InCallForegroundService.CallState.CALLING.equals(callState)
                || InCallForegroundService.CallState.RINGING.equals(callState)) {
            runOnUiThread(() -> tvCallState.setText(status));
        } else if (InCallForegroundService.CallState.BUSY.equals(callState)
                || InCallForegroundService.CallState.ENDED.equals(callState)
                || InCallForegroundService.CallState.CALL_NOT_READY.equals(callState)) {
            runOnUiThread(() -> tvCallState.setText(status));
            finishAndRemoveFromTask();
        } else if (InCallForegroundService.CallState.ANSWERED.equals(callState)) {
            if (mService != null) {
                runOnUiThread(() -> {
                    /*if (mFromComingCall) {
                        displayUiAsInConversation();
                    }*/
                    displayCountUpClockOfConversation();
                    /*long lastTimeConnectedCall = mService.getLastTimeConnectedCall();
                    if (lastTimeConnectedCall > 0) {
                        mChronometer.setBase(lastTimeConnectedCall);
                        mChronometer.start();
                    }*/
                });
            }
        }
    }

    @Override
    public void onLocalStream(VideoTrack videoTrack) {
        videoTrack.addRenderer(new VideoRenderer(localRender));
    }

    @Override
    public void onRemoteStream(BigInteger remoteClientId, VideoTrack videoTrack) {
        videoTrack.addRenderer(new VideoRenderer(remoteRender));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imgEnd) {
            if (mService != null) {
                mService.hangup();
            }
            finishAndRemoveFromTask();
        } else if (id == R.id.imgBack) {
            finishAndRemoveFromTask();
        }
    }

    private void finishAndRemoveFromTask() {
        unBindCallService();
        if(Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    private void bindCallService() {
        Intent intent = new Intent(this, InCallForegroundService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void unBindCallService() {
        if (mBound) {
            mService.unregisterCallListener(InCallActivity.this);
            unbindService(connection);
            mBound = false;
        }
    }

    private void startServiceAsForeground() {
        Intent intent = new Intent(getApplicationContext(), InCallForegroundService.class);
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, mFromComingCall);
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, mAvatarInConversation);
        intent.putExtra(EXTRA_USER_NAME, mUserNameInConversation);

        String callId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        intent.putExtra(EXTRA_GROUP_ID, callId);
        String ourClientId = getIntent().getStringExtra(EXTRA_OUR_CLIENT_ID);
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(intent);
        } else {
            getApplicationContext().startService(intent);
        }
    }
}
