package com.qiscus.mychatui.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.qiscus.mychatui.databinding.ActivityQiscusChatCallBinding;
import com.qiscus.mychatui.presenter.ChatRoomPresenter;
import com.qiscus.mychatui.util.QiscusMeetUtil;
import com.qiscus.mychatui.util.UnitCountDownTimer;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class QiscusChatCallActivity extends AppCompatActivity implements ChatRoomPresenter.View {

    private static final String CHAT_COMMENT = "extra_chat_comment";
    private QiscusComment comment;
    private QiscusChatRoom chatRoom;
    private ChatRoomPresenter chatRoomPresenter;
    private UnitCountDownTimer timer;

    public static Intent generateIntent(Context context, QiscusComment comment, String action) {
        Intent intent = new Intent(context, QiscusChatCallActivity.class);
        intent.setAction(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(CHAT_COMMENT, comment);
        return intent;
    }

    private ActivityQiscusChatCallBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQiscusChatCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        handleIntent(getIntent());
        handleAction();

        Glide.with(getApplicationContext())
                .load(!comment.isMyComment() ? comment.getSenderAvatar() : chatRoom.getAvatarUrl())
                .into(binding.ivOutgoingPicture);

        String callMessage = "Calling "+chatRoom.getName();
        if (!comment.isMyComment()) {
            callMessage = comment.getSender()+" is Calling";
        }
        binding.tvIncomingMessage.setText(callMessage);
        setIncomingCall(comment.isMyComment());

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void setIncomingCall(boolean isMyComment){
        binding.btnHangUp.setVisibility(isMyComment ? View.VISIBLE : View.INVISIBLE);
        binding.btnAcceptCall.setVisibility(isMyComment ? View.GONE : View.VISIBLE);
        binding.btnRejectCall.setVisibility(isMyComment ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    private void handleAction() {
        binding.btnAcceptCall.setOnClickListener(v -> {
            chatRoomPresenter.answerCall(comment);
        });

        binding.btnRejectCall.setOnClickListener(v -> {
            chatRoomPresenter.rejectCall(comment);
        });

        binding.btnHangUp.setOnClickListener(v->{
            chatRoomPresenter.endCall(comment);
        });
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        comment = intent.getParcelableExtra(CHAT_COMMENT);
        chatRoom = QiscusCore.getDataStore().getChatRoom(comment.getRoomId());

        chatRoomPresenter = new ChatRoomPresenter(this, chatRoom);

        timer = new UnitCountDownTimer(10L, TimeUnit.SECONDS, 1) {
            @Override
            public void onUnitTick(long secondsUntilFinished, @NonNull TimeUnit unit) {
                Log.e(getClass().getName(), "onUnitTick() called with: secondsUntilFinished = [" + secondsUntilFinished + "], unit = [" + unit + "]");
            }

            @Override
            public void onFinish() {
                super.onFinish();
                if (comment.isMyComment()) chatRoomPresenter.endCall(comment, "Outgoing Call");
            }
        };

        if (QiscusMeetUtil.CallType.CALLING.equals(intent.getAction())){
            if (comment.isMyComment()) timer.start();
        }
    }

    @Subscribe
    public void onReceiveComment(QiscusCommentReceivedEvent event) {
        try {
            QiscusComment comment = event.getQiscusComment();
            JSONObject extras = comment.getExtras();
            String callAction = extras.getString(QiscusMeetUtil.CallType.CALL_ACTION);

            switch (callAction){
                case QiscusMeetUtil.CallType.CALL_ACCEPTED:
                case QiscusMeetUtil.CallType.CALL_ENDED:
                    timer.cancel();
                    finish();
                    break;
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), "onReceiveComment: ", e);
        } finally {
            finish();
        }
    }

    @Override
    public void dismissLoading() {
        Log.e(getClass().getName(), "dismissLoading() called");
    }

    @Override
    public void showLoading() {
        Log.e(getClass().getName(), "showLoading() called");
    }

    @Override
    public void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRoomChanged(QiscusChatRoom qiscusChatRoom) {
        Log.e(getClass().getName(), "onRoomChanged() called with: qiscusChatRoom = [" + qiscusChatRoom + "]");
    }

    @Override
    public void onSendingComment(QiscusComment qiscusComment) {
        Log.e(getClass().getName(), "onSendingComment() called with: qiscusComment = [" + qiscusComment + "]");
    }

    @Override
    public void onSuccessSendComment(QiscusComment qiscusComment) {
        Log.e(getClass().getName(), "onSuccessSendComment() called with: qiscusComment = [" + qiscusComment + "]");
    }

    @Override
    public void onFailedSendComment(QiscusComment qiscusComment) {
        Log.e(getClass().getName(), "onFailedSendComment() called with: qiscusComment = [" + qiscusComment + "]");
    }

    @Override
    public void onNewComment(QiscusComment qiscusComment) {
        Log.e(getClass().getName(), "onNewComment() called with: qiscusComment = [" + qiscusComment + "]");
    }

    @Override
    public void onCommentDeleted(QiscusComment qiscusComment) {
        Log.e(getClass().getName(), "onCommentDeleted() called with: qiscusComment = [" + qiscusComment + "]");
    }

    @Override
    public void refreshComment(QiscusComment qiscusComment) {
        Log.e(getClass().getName(), "refreshComment() called with: qiscusComment = [" + qiscusComment + "]");
    }

    @Override
    public void updateLastDeliveredComment(long lastDeliveredCommentId) {
        Log.e(getClass().getName(), "updateLastDeliveredComment() called with: lastDeliveredCommentId = [" + lastDeliveredCommentId + "]");
    }

    @Override
    public void updateLastReadComment(long lastReadCommentId) {
        Log.e(getClass().getName(), "updateLastReadComment() called with: lastReadCommentId = [" + lastReadCommentId + "]");
    }

    @Override
    public void onRealtimeStatusChanged(boolean connected) {
        Log.e(getClass().getName(), "onRealtimeStatusChanged() called with: connected = [" + connected + "]");
    }

    @Override
    public void clearCommentsBefore(long timestamp) {
        Log.e(getClass().getName(), "clearCommentsBefore() called with: timestamp = [" + timestamp + "]");
    }

    @Override
    public void onUserTyping(String user, boolean typing) {
        Log.e(getClass().getName(), "onUserTyping() called with: user = [" + user + "], typing = [" + typing + "]");
    }
}