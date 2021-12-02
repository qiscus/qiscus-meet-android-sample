package com.qiscus.mychatui;

import android.content.Intent;

import androidx.multidex.MultiDexApplication;

import com.qiscus.jupuk.Jupuk;
import com.qiscus.meet.MeetJwtConfig;
import com.qiscus.meet.MeetParticipantJoinedEvent;
import com.qiscus.meet.MeetParticipantLeftEvent;
import com.qiscus.meet.MeetTerminatedConfEvent;
import com.qiscus.meet.QiscusMeet;
import com.qiscus.mychatui.ui.QiscusChatCallActivity;
import com.qiscus.mychatui.util.PushNotificationUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class MyApplication extends MultiDexApplication {
    private static MyApplication instance;

    private AppComponent component;

    public static MyApplication getInstance() {
        return instance;
    }

    private static void initEmoji() {
        EmojiManager.install(new EmojiOneProvider());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        component = new AppComponent(this);

        Nirmana.init(this);
        QiscusCore.setup(this, BuildConfig.QISCUS_SDK_APP_ID);

        // Setup QiscusMeet
        QiscusMeet.setup(this, "qiscus-lMNhoA0fw7CDAY8d", "https://meet.qiscus.com");

        // Register EventBus
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        // Setup QiscusMeet config
        MeetJwtConfig jwtConfig = new MeetJwtConfig()
                .setEmail(QiscusCore.getQiscusAccount().getEmail());
        jwtConfig.build();

        QiscusMeet.config().setJwtConfig(jwtConfig)
                .setAutoRecording(false)
                .setScreenSharing(false)
                .setOverflowMenu(true)
                .setChat(false)
                .setRecording(true)
                .setParticipantMenu(false)
                .setTileView(false)
                .setEnableRoomName(false);

        // Setup QiscusChat config
        QiscusCore.getChatConfig()
                .enableDebugMode(true)
                .setNotificationListener(PushNotificationUtil::showNotification)
                .setEnableFcmPushNotification(true);
        initEmoji();
        Jupuk.init(this);
    }

    public AppComponent getComponent() {
        return component;
    }

    @Subscribe
    public void onReceiveComment(QiscusCommentReceivedEvent event) {
        try {
            QiscusComment comment = event.getQiscusComment();
            JSONObject extras = comment.getExtras();
            String callAction = extras.getString("status");

            if ("calling".equals(callAction)) {
                Intent intent = QiscusChatCallActivity.generateIntent(this, comment, callAction);
                startActivity(intent);
            }
        } catch (JSONException e) {
            Timber.e(e, "onReceiveComment: ");
        }
    }

    @Subscribe
    public void onTerminatedConf(MeetTerminatedConfEvent event) {
        Timber.e( "onTerminatedConf: %s", event.getData());
    }

    @Subscribe
    public void onParticipantLeft(MeetParticipantLeftEvent event) {
        // end call on Participant Left Conference
        QiscusMeet.endCall();
    }

    @Subscribe
    public void onParticipantJoined(MeetParticipantJoinedEvent event) {
        Timber.e( "onParticipantJoined: %s", event.getData());
    }
}
