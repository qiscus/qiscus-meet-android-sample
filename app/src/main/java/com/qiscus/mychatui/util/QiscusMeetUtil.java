package com.qiscus.mychatui.util;

import android.content.Context;

import com.qiscus.meet.MeetJwtConfig;
import com.qiscus.meet.QiscusMeet;
import com.qiscus.mychatui.ui.QiscusChatCallActivity;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class QiscusMeetUtil {

    public static void handleReceivedMessageUtil(Context context, QiscusComment comment) {
        try {
            JSONObject extras = comment.getExtras();
            String callAction = extras.getString(CallType.CALL_ACTION);

            switch (callAction) {
                case CallType.CALLING:
                    launchCallingScreen(context, comment, callAction);
                    break;
                case QiscusMeetUtil.CallType.CALL_ACCEPTED:
                    QiscusMeetUtil.startCall(context, comment);
                    break;
                case QiscusMeetUtil.CallType.CALL_ENDED:
                    QiscusMeet.endCall();
                    break;
            }
        } catch (JSONException e) {
            Timber.e(e, "onReceiveComment: ");
        }
    }

    public static void launchCallingScreen(Context context, QiscusComment comment, String action) {
        context.startActivity(QiscusChatCallActivity.generateIntent(context, comment, action));
    }

    public static void startCall(Context context, QiscusComment comment) {
        try {
            JSONObject payload = new JSONObject(comment.getExtraPayload());
            String type = payload.getString("type");

            if (CallType.CALL.equals(type)) {
                MeetJwtConfig jwtConfig = new MeetJwtConfig();
                jwtConfig.setEmail(QiscusCore.getQiscusAccount().getEmail());
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

                QiscusMeet.call()
                        .setTypeCall(QiscusMeet.Type.VOICE)
                        .setRoomId(comment.getUniqueId())
                        .setMuted(false)
                        .setDisplayName(!comment.isMyComment() ? QiscusCore.getQiscusAccount().getUsername() : comment.getSender())
                        .setAvatar(!comment.isMyComment() ? QiscusCore.getQiscusAccount().getAvatar() : comment.getSenderAvatar())
                        .build(context);
            }
            else Timber.e( "startCall: type " + type + " is not "+ CallType.CALL);
        } catch (JSONException e) {
            Timber.e(e, "onReceiveComment: ");
        }
    }

    public static void answerCall(Context context, QiscusComment comment) {
        try {
            JSONObject payload = new JSONObject(comment.getExtraPayload());
            JSONObject content = payload.getJSONObject("content");
            String type = payload.getString("type");
            boolean isVideo = content.getBoolean("isVideo");

            if (CallType.CALL.equals(type)) {
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
            else Timber.e( "startCall: type " + type + " is not "+ CallType.CALL);
        } catch (JSONException e) {
            Timber.e(e, "onReceiveComment: ");
        }
    }

    public static class CallType {
        public static final String CALL_ACTION = "status", CALL_ACCEPTED = "answer", CALL_ENDED = "reject", CALLING = "calling", CALL = "call";
    }
}
