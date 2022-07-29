package com.andre.videoapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

import android.os.Bundle;
import android.widget.TextView;

import com.andre.videoapp.model.StatisticsInfo;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
            ,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    // Fill the App ID of your project generated on Agora Console.
    public static final String appId = "APP_ID";
    // Fill the channel name.
    public static final String channelName = "AndreVideoAppChanel01";
    // Fill the temp token generated on Agora Console.
    public static final String token = "TOKEN";
    private RtcEngine mRtcEngine;
    private StatisticsInfo statisticsInfo;

//    private AppCompatTextView localStats, remoteStats;
    private TextView localStats, remoteStats;
    private FrameLayout fl_local, fl_remote;
    private AppCompatButton screenShareButton;
    private AppCompatButton startButton;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // Listen for the remote user joining the channel to get the uid of the user.
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Call setupRemoteVideo to set the remote video view after getting uid from the onUserJoined callback.
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onRtcStats(RtcStats stats) {
            statisticsInfo.setRtcStats(stats);
        }

        @Override
        public void onLocalVideoStats(LocalVideoStats localVideoStats) {
            statisticsInfo.setLocalVideoStats(localVideoStats);
            updateLocalStats();
        }

        @Override
        public void onLocalAudioStats(LocalAudioStats localAudioStats) {
            statisticsInfo.setLocalAudioStats(localAudioStats);
            updateLocalStats();
        }

        @Override
        public void onRemoteVideoStats(RemoteVideoStats remoteVideoStats) {
            statisticsInfo.setRemoteVideoStats(remoteVideoStats);
            updateRemoteStats();
        }

        @Override
        public void onRemoteAudioStats(RemoteAudioStats remoteAudioStats) {
            statisticsInfo.setRemoteAudioStats(remoteAudioStats);
            updateRemoteStats();
        }
    };

    private void updateLocalStats(){
        localStats.setText(statisticsInfo.getLocalVideoStats());
        localStats.bringToFront();
    }

    private void updateRemoteStats(){
        remoteStats.setText(statisticsInfo.getRemoteVideoStats());
        remoteStats.bringToFront();
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private void initializeAndJoinChannel() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), appId, mRtcEventHandler);
        } catch (Exception e) {
            throw new RuntimeException("Check the error.");
        }

        // By default, video is disabled, and you need to call enableVideo to start a video stream.
        mRtcEngine.enableVideo();

        FrameLayout container = findViewById(R.id.local_video_view_container);
        // Call CreateRendererView to create a SurfaceView object and add it as a child to the FrameLayout.
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        // Pass the SurfaceView object to Agora so that it renders the local video.
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0));

        // Join the channel with a token.
        mRtcEngine.joinChannel(token, channelName, "", 0);
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)
                &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)&&
                checkSelfPermission(REQUESTED_PERMISSIONS[3], PERMISSION_REQ_ID)
                )
        {
//            initializeAndJoinChannel();
            initComponents();
        }
    }

    private void initComponents() {
        statisticsInfo = new StatisticsInfo();

        localStats = findViewById(R.id.local_stats);
        localStats.bringToFront();

        remoteStats = findViewById(R.id.remote_stats);
        remoteStats.bringToFront();

        screenShareButton = findViewById(R.id.screen_share_button);
//        screenShareButton.bringToFront();
        screenShareButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, ScreenShareActivity.class);
            startActivity(intent);
        });

        startButton = findViewById(R.id.start_call_button);

        startButton.setOnClickListener(view -> initializeAndJoinChannel());
    }
}