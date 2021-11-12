package com.qiscus.mychatui.util;

import android.content.Context;
import android.util.Log;

import com.qiscus.meet.MeetJwtConfig;
import com.qiscus.meet.QiscusMeet;
import com.qiscus.mychatui.ui.ChatRoomActivity;
import com.qiscus.mychatui.ui.QiscusChatCallActivity;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;

import org.json.JSONException;
import org.json.JSONObject;

public class QiscusMeetUtil {

    private static final String TAG = "QiscusMeetUtil";

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

    public static void launchCallingScreen(Context context, QiscusComment comment, String action) {
        context.startActivity(QiscusChatCallActivity.generateIntent(context, comment, action));
    }

    public static void startCall(Context context, QiscusComment comment) {
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
        } catch (JSONException e) {
            Log.e(QiscusMeetUtil.class.getName(), "onReceiveComment: ", e);
        }
    }

    public static class CallType {
        public static final String CALL_ACTION = "status", CALL_ACCEPTED = "answer", CALL_ENDED = "reject", CALLING = "calling", CALL = "call";
    }
}
