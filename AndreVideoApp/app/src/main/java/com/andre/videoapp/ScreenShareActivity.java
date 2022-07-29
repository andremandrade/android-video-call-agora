package com.andre.videoapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.ScreenCaptureParameters;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.VideoCanvas;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import static io.agora.rtc.video.VideoCanvas.RENDER_MODE_HIDDEN;

public class ScreenShareActivity extends AppCompatActivity {

    private static final String TAG = ScreenShareActivity.class.getSimpleName();
    private RtcEngine engine;
    private int myUid;
    private boolean joined = false;

    private FrameLayout fl_remote;
    private FrameLayout fl_remote_video;
    private TextView description;
    private AppCompatButton join_button;
    private Handler handler;

    public static final String channelName = "AndreVideoAppShare";
    public static final String token = "TOKEN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_screen_share);
        description = findViewById(R.id.screen_share_logs);
        fl_remote = findViewById(R.id.fl_remote);
        fl_remote_video = findViewById(R.id.fl_remote_video);
        join_button = findViewById(R.id.join_button);

        try {
            engine = RtcEngine.create(getApplicationContext(), MainActivity.appId, getIRtcEngineEventHandler());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Check the error.", e);
        }
        join_button.setOnClickListener(view -> joinChanelAction());
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(engine != null)
        {
            engine.leaveChannel();
        }
        handler.post(RtcEngine::destroy);
        engine = null;
    }

    void joinChanelAction(){
        String channelId = channelName;
        // Check permission
        if (AndPermission.hasPermissions(this, Permission.Group.STORAGE, Permission.Group.MICROPHONE, Permission.Group.CAMERA))
        {
            joinChannel(channelId);
            return;
        }
        // Request permission
        AndPermission.with(this).runtime().permission(
                Permission.Group.STORAGE,
                Permission.Group.MICROPHONE,
                Permission.Group.CAMERA
        ).onGranted(permissions ->
        {
            // Permissions Granted
            joinChannel(channelId);
        }).start();
    }

    private void joinChannel(String channelId)
    {
        // Check if the context is valid
        Context context = getApplicationContext();
        if (context == null)
        {
            return;
        }

        engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        /**In the demo, the default is to enter as the anchor.*/
        engine.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER);
        // Enable video module
        engine.enableVideo();

        ScreenCaptureParameters screenCaptureParameters = new ScreenCaptureParameters();
        screenCaptureParameters.captureAudio = true;
        screenCaptureParameters.captureVideo = true;
        ScreenCaptureParameters.VideoCaptureParameters videoCaptureParameters = new ScreenCaptureParameters.VideoCaptureParameters();
        screenCaptureParameters.videoCaptureParameters = videoCaptureParameters;
        engine.startScreenCapture(screenCaptureParameters);
        description.setText("Screen Sharing Starting");

        String accessToken = token;
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(accessToken, "<#YOUR ACCESS TOKEN#>"))
        {
            accessToken = null;
        }

        ChannelMediaOptions option = new ChannelMediaOptions();
        option.autoSubscribeAudio = true;
        option.autoSubscribeVideo = true;
        int res = engine.joinChannel(accessToken, channelId, "Extra Optional Data", 0, option);
        if (res != 0)
        {
            showAlert(RtcEngine.getErrorDescription(Math.abs(res)));
            return;
        }
        join_button.setEnabled(false);
    }

    protected void showAlert(String message)
    {
        handler.post(()->{
            Context context = getApplicationContext();
            if (context == null) {
                return;
            }

            new AlertDialog.Builder(context).setTitle("Tips").setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    protected final void showLongToast(final String msg)
    {
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (this == null || getApplicationContext() == null)
                {return;}
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private IRtcEngineEventHandler getIRtcEngineEventHandler() {
        return new IRtcEngineEventHandler() {

            @Override
            public void onWarning(int warn) {
                Log.w(TAG, String.format("onWarning code %d message %s", warn, RtcEngine.getErrorDescription(warn)));
            }

            @Override
            public void onError(int err) {
                Log.e(TAG, String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)));
                showAlert(String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)));
            }

            @Override
            public void onLeaveChannel(RtcStats stats) {
                super.onLeaveChannel(stats);
                Log.i(TAG, String.format("local user %d leaveChannel!", myUid));
                showLongToast(String.format("local user %d leaveChannel!", myUid));
            }

            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                Log.i(TAG, String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
                showLongToast(String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
                myUid = uid;
                joined = true;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        join_button.setEnabled(true);
                        join_button.setText("Leave");
                    }
                });
            }

            @Override
            public void onFirstLocalVideoFramePublished(int elapsed) {
                description.setText("Screen Sharing started");
            }

            @Override
            public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
                super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
                Log.i(TAG, "onRemoteAudioStateChanged->" + uid + ", state->" + state + ", reason->" + reason);
            }

            @Override
            public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
                super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
                Log.i(TAG, "onRemoteVideoStateChanged->" + uid + ", state->" + state + ", reason->" + reason);
            }

            @Override
            public void onUserJoined(int uid, int elapsed) {
                super.onUserJoined(uid, elapsed);
                Log.i(TAG, "onUserJoined->" + uid);
                showLongToast(String.format("user %d joined!", uid));
                /**Check if the context is correct*/
                Context context = getApplicationContext();
                if (context == null) {
                    return;
                }
                handler.post(() ->
                {
                    /**Display remote video stream*/
                    SurfaceView surfaceView = null;
                    if (fl_remote.getChildCount() > 0) {
                        fl_remote.removeAllViews();
                    }
                    // Create render view by RtcEngine
                    surfaceView = RtcEngine.CreateRendererView(context);
                    surfaceView.setZOrderMediaOverlay(true);
                    // Add to the remote container
                    fl_remote.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    // Setup remote video to render
                    engine.setupRemoteVideo(new VideoCanvas(surfaceView, RENDER_MODE_HIDDEN, uid));
                });
            }

            @Override
            public void onUserOffline(int uid, int reason) {
                Log.i(TAG, String.format("user %d offline! reason:%d", uid, reason));
                showLongToast(String.format("user %d offline! reason:%d", uid, reason));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        /**Clear render view
                         Note: The video will stay at its last frame, to completely remove it you will need to
                         remove the SurfaceView from its parent*/
                        engine.setupRemoteVideo(new VideoCanvas(null, RENDER_MODE_HIDDEN, uid));
                    }
                });
            }
        };
    }
}