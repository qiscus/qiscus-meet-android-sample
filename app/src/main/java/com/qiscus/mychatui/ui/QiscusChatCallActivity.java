package com.qiscus.mychatui.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.qiscus.meet.QiscusMeet;
import com.qiscus.mychatui.databinding.ActivityQiscusChatCallBinding;
import com.qiscus.mychatui.util.QiscusMeetUtil;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;

public class QiscusChatCallActivity extends AppCompatActivity {

    private static final String CHAT_COMMENT = "extra_chat_comment";
    private QiscusComment comment;

    public static Intent generateIntent(Context context, QiscusComment comment, String action) {
        Intent intent = new Intent(context, QiscusChatCallActivity.class);
        intent.setAction(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
                .load(comment.isMyComment() ? comment.getSenderAvatar() : comment.getRoomAvatar())
                .into(binding.ivOutgoingPicture);

        binding.tvIncomingMessage.setText(comment.getSender()+" is Calling");
        binding.tvOutgoingMessage.setText(QiscusCore.getQiscusAccount().getUsername() +" is Calling");
        if (comment.isMyComment()) binding.containerIncomingCall.setVisibility(View.GONE);
        if (!comment.isMyComment()) binding.tvOutgoingMessage.setVisibility(View.GONE);
        if (!comment.isMyComment()) binding.btnHangUp.setVisibility(View.GONE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    private void handleAction() {
        binding.btnAcceptCall.setOnClickListener(v -> {
            QiscusMeetUtil.accept(false, comment);
        });

        binding.btnRejectCall.setOnClickListener(v -> {
            // TODO: 04/11/2021 send reject call status
            QiscusMeetUtil.reject(false, comment);
            finish();
        });

        binding.btnHangUp.setOnClickListener(v->{
            // TODO: 04/11/2021 send cancel call status
            QiscusMeetUtil.endCall(false, comment);
            finish();
        });
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        comment = intent.getParcelableExtra(CHAT_COMMENT);
        switch (intent.getAction()){
            case QiscusMeetUtil.CallType.CALL_ACCEPTED:
                QiscusMeetUtil.startCall(this, comment);
                finish();
                break;
            case QiscusMeetUtil.CallType.CALL_ENDED:
                QiscusMeet.endCall();
                finish();
                break;
        }
    }
}