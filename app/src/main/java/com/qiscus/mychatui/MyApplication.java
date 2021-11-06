package com.qiscus.mychatui;


import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.qiscus.jupuk.Jupuk;
import com.qiscus.meet.MeetParticipantJoinedEvent;
import com.qiscus.meet.MeetParticipantLeftEvent;
import com.qiscus.meet.MeetTerminatedConfEvent;
import com.qiscus.meet.QiscusMeet;
import com.qiscus.mychatui.util.PushNotificationUtil;
import com.qiscus.mychatui.util.QiscusMeetUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
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
        QiscusMeet.setup(this, "qiscus-lMNhoA0fw7CDAY8d", "https://meet.qiscus.com");

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
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
        Log.e(getPackageName(), "onReceiveComment() called with: event = [" + event.getQiscusComment() + "]");
        QiscusMeetUtil.handleReceivedMessageUtil(this, event);
    }

    @Subscribe
    public void onTerminatedConf(MeetTerminatedConfEvent event) {
        Log.e(MyApplication.class.getName(), event.getData().toString());
    }

    @Subscribe
    public void onParticipantLeft(MeetParticipantLeftEvent event) {
        Log.e(MyApplication.class.getName(), event.getData().toString());
    }

    @Subscribe
    public void onParticipantJoined(MeetParticipantJoinedEvent event) {
        Log.e(MyApplication.class.getName(), event.getData().toString());
    }
}
