package com.qiscus.mychatui.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.qiscus.meet.QiscusMeet;
import com.qiscus.mychatui.databinding.ActivityQiscusChatCallBinding;
import com.qiscus.mychatui.presenter.ChatRoomPresenter;
import com.qiscus.mychatui.util.QiscusMeetUtil;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;

public class QiscusChatCallActivity extends AppCompatActivity implements ChatRoomPresenter.View {

    private static final String CHAT_COMMENT = "extra_chat_comment";
    private QiscusComment comment;
    private QiscusChatRoom chatRoom;
    private ChatRoomPresenter chatRoomPresenter;

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
            chatRoomPresenter.answerCall(false, comment);
        });

        binding.btnRejectCall.setOnClickListener(v -> {
            chatRoomPresenter.rejectCall(false, comment);
            finish();
        });

        binding.btnHangUp.setOnClickListener(v->{
            chatRoomPresenter.endCall(false, comment);
            finish();
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