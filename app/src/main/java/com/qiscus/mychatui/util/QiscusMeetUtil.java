package com.qiscus.mychatui.util;

import android.content.Context;
import android.util.Log;

import com.qiscus.meet.MeetJwtConfig;
import com.qiscus.meet.QiscusMeet;
import com.qiscus.mychatui.presenter.ChatRoomPresenter;
import com.qiscus.mychatui.ui.ChatRoomActivity;
import com.qiscus.mychatui.ui.QiscusChatCallActivity;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.HttpException;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class QiscusMeetUtil {

    private static final String TAG = "QiscusMeetUtil";

    private static Map<QiscusComment, Subscription> pendingTask = new HashMap<>();

    public static void handleReceivedMessageUtil(Context context, QiscusCommentReceivedEvent event) {
        try {
            JSONObject payload = new JSONObject(event.getQiscusComment().getExtraPayload());
            JSONObject content = payload.getJSONObject("content");
            String callAction = content.getString(CallType.CALL_ACTION);

            launchCallingScreen(context, event.getQiscusComment(), callAction);
        } catch (JSONException e) {
            Log.e(ChatRoomActivity.class.getName(), "onReceiveComment: ", e);
        }
    }

    // TODO: 03/11/2021 Replace with activty with Incoming call UI
    public static void launchCallingScreen(Context context, QiscusComment comment, String action) {
        context.startActivity(QiscusChatCallActivity.generateIntent(context, comment, action));
    }

    public static void startCall(Context context, QiscusComment comment) {
        try {
            JSONObject payload = new JSONObject(comment.getExtraPayload());
            JSONObject content = payload.getJSONObject("content");
            String type = payload.getString("type");
            boolean isVideo = content.getBoolean("isVideo");

            if (QiscusComment.Type.SYSTEM_EVENT.name().equals(type)) {
                MeetJwtConfig jwtConfig = new MeetJwtConfig();
                jwtConfig.setEmail(QiscusCore.getQiscusAccount().getEmail());
                jwtConfig.build();

                QiscusMeet.config().setJwtConfig(jwtConfig)
                        .setAutoRecording(false)
                        .setScreenSharing(true)
                        .setOverflowMenu(true)
                        .setChat(true)
                        .setEnableRoomName(true);

                QiscusMeet.call()
                        .setTypeCall(isVideo ? QiscusMeet.Type.VIDEO : QiscusMeet.Type.VOICE)
                        .setRoomId(comment.getUniqueId())
                        .setMuted(true)
                        .setDisplayName(!comment.isMyComment() ? QiscusCore.getQiscusAccount().getUsername() : comment.getSender())
                        .setAvatar(!comment.isMyComment() ? QiscusCore.getQiscusAccount().getAvatar() : comment.getSenderAvatar())
                        .build(context);
            }
        } catch (JSONException e) {
            Log.e(QiscusMeetUtil.class.getName(), "onReceiveComment: ", e);
        }
    }

    public static void answerCall(Context context, QiscusComment comment) {
        try {
            JSONObject payload = new JSONObject(comment.getExtraPayload());
            JSONObject content = payload.getJSONObject("content");
            String type = payload.getString("type");
            boolean isVideo = content.getBoolean("isVideo");

            if (QiscusComment.Type.SYSTEM_EVENT.name().equals(type)) {
                MeetJwtConfig jwtConfig = new MeetJwtConfig();
                jwtConfig.setEmail(QiscusCore.getQiscusAccount().getEmail());
                jwtConfig.build();

                QiscusMeet.config().setJwtConfig(jwtConfig)
                        .setAutoRecording(false)
                        .setScreenSharing(true)
                        .setOverflowMenu(true)
                        .setChat(true)
                        .setEnableRoomName(true);

                QiscusMeet.answer()
                        .setTypeCall(isVideo ? QiscusMeet.Type.VIDEO : QiscusMeet.Type.VOICE)
                        .setRoomId(comment.getUniqueId())
                        .setMuted(true)
                        .setDisplayName(!comment.isMyComment() ? QiscusCore.getQiscusAccount().getUsername() : comment.getSender())
                        .setAvatar(!comment.isMyComment() ? QiscusCore.getQiscusAccount().getAvatar() : comment.getSenderAvatar())
                        .build(context);
            }
        } catch (JSONException e) {
            Log.e(QiscusMeetUtil.class.getName(), "onReceiveComment: ", e);
        }
    }

    public static void accept(boolean isVideo, QiscusComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("isVideo", isVideo);
        map.put(CallType.CALL_ACTION, CallType.CALL_ACCEPTED);
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(comment.getRoomId(), "Call Accepted", QiscusComment.Type.SYSTEM_EVENT.name(), new JSONObject(map));
        sendComment(qiscusComment);
    }

    public static void reject(boolean isVideo, QiscusComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("isVideo", isVideo);
        map.put(CallType.CALL_ACTION, CallType.CALL_ENDED);
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(comment.getRoomId(), "Call Rejected", QiscusComment.Type.SYSTEM_EVENT.name(), new JSONObject(map));
        sendComment(qiscusComment);
    }

    public static void endCall(boolean isVideo, QiscusComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("isVideo", isVideo);
        map.put(CallType.CALL_ACTION, CallType.CALL_ENDED);
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(comment.getRoomId(), "Call Canceled", QiscusComment.Type.SYSTEM_EVENT.name(), new JSONObject(map));
        sendComment(qiscusComment);
    }

    private static void sendComment(QiscusComment qiscusComment) {
        Subscription subscription = QiscusApi.getInstance().sendMessage(qiscusComment)
                .doOnSubscribe(() -> QiscusCore.getDataStore().addOrUpdate(qiscusComment))
                .doOnNext(QiscusMeetUtil::commentSuccess)
                .doOnError(throwable -> commentFail(throwable, qiscusComment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(commentSend -> {
                    Log.e(TAG, "sendComment() commentSend");
                }, throwable -> {
                    Log.e(ChatRoomPresenter.class.getName(), "sendComment: ", throwable);
                });

        pendingTask.put(qiscusComment, subscription);
    }

    private static void commentSuccess(QiscusComment qiscusComment) {
        pendingTask.remove(qiscusComment);
        qiscusComment.setState(QiscusComment.STATE_ON_QISCUS);
        QiscusComment savedQiscusComment = QiscusCore.getDataStore().getComment(qiscusComment.getUniqueId());
        if (savedQiscusComment != null && savedQiscusComment.getState() > qiscusComment.getState()) {
            qiscusComment.setState(savedQiscusComment.getState());
        }
        QiscusCore.getDataStore().addOrUpdate(qiscusComment);
    }

    private static void commentFail(Throwable throwable, QiscusComment qiscusComment) {
        pendingTask.remove(qiscusComment);
        if (!QiscusCore.getDataStore().isContains(qiscusComment)) { //Have been deleted
            return;
        }

        int state = QiscusComment.STATE_PENDING;
        if (mustFailed(throwable, qiscusComment)) {
            qiscusComment.setDownloading(false);
            state = QiscusComment.STATE_FAILED;
        }

        //Kalo ternyata comment nya udah sukses dikirim sebelumnya, maka ga usah di update
        QiscusComment savedQiscusComment = QiscusCore.getDataStore().getComment(qiscusComment.getUniqueId());
        if (savedQiscusComment != null && savedQiscusComment.getState() > QiscusComment.STATE_SENDING) {
            return;
        }

        //Simpen statenya
        qiscusComment.setState(state);
        QiscusCore.getDataStore().addOrUpdate(qiscusComment);
    }

    private static boolean mustFailed(Throwable throwable, QiscusComment qiscusComment) {
        //Error response from server
        //Means something wrong with server, e.g user is not member of these room anymore
        return ((throwable instanceof HttpException && ((HttpException) throwable).code() >= 400) ||
                //if throwable from JSONException, e.g response from server not json as expected
                (throwable instanceof JSONException) ||
                // if attachment type
                qiscusComment.isAttachment());
    }

    public static class CallType {
        public static final String CALL_ACTION = "extra_action", CALL_ACCEPTED = "start_call", CALL_ENDED = "end_call", CALLING = "calling";
    }
}
