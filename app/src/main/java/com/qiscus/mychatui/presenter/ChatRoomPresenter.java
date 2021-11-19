package com.qiscus.mychatui.presenter;

import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.util.QiscusImageUtil;
import com.qiscus.mychatui.util.QiscusMeetUtil;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager;
import com.qiscus.sdk.chat.core.data.model.QiscusAccount;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.model.QiscusRoomMember;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;
import com.qiscus.sdk.chat.core.data.remote.QiscusPusherApi;
import com.qiscus.sdk.chat.core.event.QiscusClearCommentsEvent;
import com.qiscus.sdk.chat.core.event.QiscusCommentDeletedEvent;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;
import com.qiscus.sdk.chat.core.event.QiscusCommentResendEvent;
import com.qiscus.sdk.chat.core.event.QiscusMqttStatusEvent;
import com.qiscus.sdk.chat.core.presenter.QiscusChatRoomEventHandler;
import com.qiscus.sdk.chat.core.util.QiscusAndroidUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.HttpException;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Jul, Fri 27 2018 12.52
 **/
public class ChatRoomPresenter extends QiscusPresenter<ChatRoomPresenter.View> implements QiscusChatRoomEventHandler.StateListener {

    private QiscusChatRoom room;
    private QiscusAccount qiscusAccount;
    private Func2<QiscusComment, QiscusComment, Integer> commentComparator = (lhs, rhs) -> rhs.getTime().compareTo(lhs.getTime());

    private Map<QiscusComment, Subscription> pendingTask;

    private QiscusChatRoomEventHandler roomEventHandler;

    public ChatRoomPresenter(View view, QiscusChatRoom room) {
        super(view);
        this.view = view;

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        this.room = room;
        if (this.room.getMember().isEmpty()) {
            this.room = QiscusCore.getDataStore().getChatRoom(room.getId());
        }
        qiscusAccount = QiscusCore.getQiscusAccount();
        pendingTask = new HashMap<>();

        roomEventHandler = new QiscusChatRoomEventHandler(this.room, this);
    }

    private void commentSuccess(QiscusComment qiscusComment) {
        pendingTask.remove(qiscusComment);
        qiscusComment.setState(QiscusComment.STATE_ON_QISCUS);
        QiscusComment savedQiscusComment = QiscusCore.getDataStore().getComment(qiscusComment.getUniqueId());
        if (savedQiscusComment != null && savedQiscusComment.getState() > qiscusComment.getState()) {
            qiscusComment.setState(savedQiscusComment.getState());
        }
        QiscusCore.getDataStore().addOrUpdate(qiscusComment);
    }

    private boolean mustFailed(Throwable throwable, QiscusComment qiscusComment) {
        //Error response from server
        //Means something wrong with server, e.g user is not member of these room anymore
        return ((throwable instanceof HttpException && ((HttpException) throwable).code() >= 400) ||
                //if throwable from JSONException, e.g response from server not json as expected
                (throwable instanceof JSONException) ||
                // if attachment type
                qiscusComment.isAttachment());
    }

    private void commentFail(Throwable throwable, QiscusComment qiscusComment) {
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

    private void sendComment(QiscusComment qiscusComment) {
        view.onSendingComment(qiscusComment);
        Subscription subscription = QiscusApi.getInstance().sendMessage(qiscusComment)
                .doOnSubscribe(() -> QiscusCore.getDataStore().addOrUpdate(qiscusComment))
                .doOnNext(this::commentSuccess)
                .doOnError(throwable -> commentFail(throwable, qiscusComment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    Log.e(ChatRoomPresenter.class.getName(), "sendComment: ", throwable);
                    if (qiscusComment.getRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }

    public void initiateCall(boolean isVideo) {
        Map<String, Object> map = new HashMap<>();
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(room.getId(), "Calling", QiscusMeetUtil.CallType.CALL, new JSONObject(map));
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(QiscusMeetUtil.CallType.CALL_ACTION, QiscusMeetUtil.CallType.CALLING);
        } catch (JSONException e) {
            Log.e("ChatRoomPresenter", "Json Object error", e);
            e.printStackTrace();
        }
        qiscusComment.setExtras(jsonObject);
        sendComment(qiscusComment);
    }

    public void answerCall(QiscusComment comment) {
        Map<String, Object> map = new HashMap<>();
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(comment.getRoomId(), "Call Accepted", QiscusMeetUtil.CallType.CALL, new JSONObject(map));
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(QiscusMeetUtil.CallType.CALL_ACTION, QiscusMeetUtil.CallType.CALL_ACCEPTED);
        } catch (JSONException e) {
            Log.e("ChatRoomPresenter", "Json Object error", e);
            e.printStackTrace();
        }
        qiscusComment.setExtras(jsonObject);
        sendComment(qiscusComment);
    }

    public void rejectCall(QiscusComment comment) {
        Map<String, Object> map = new HashMap<>();
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(comment.getRoomId(), "Call Rejected", QiscusMeetUtil.CallType.CALL, new JSONObject(map));
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(QiscusMeetUtil.CallType.CALL_ACTION, QiscusMeetUtil.CallType.CALL_ENDED);
        } catch (JSONException e) {
            Log.e("ChatRoomPresenter", "Json Object error", e);
            e.printStackTrace();
        }
        qiscusComment.setExtras(jsonObject);
        sendComment(qiscusComment);
    }

    public void endCall(QiscusComment comment) {
        Map<String, Object> map = new HashMap<>();
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(comment.getRoomId(), "Call Ended", QiscusMeetUtil.CallType.CALL, new JSONObject(map));
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(QiscusMeetUtil.CallType.CALL_ACTION, QiscusMeetUtil.CallType.CALL_ENDED);
        } catch (JSONException e) {
            Log.e("ChatRoomPresenter", "Json Object error", e);
            e.printStackTrace();
        }
        qiscusComment.setExtras(jsonObject);
        sendComment(qiscusComment);
    }

    @Subscribe
    public void onMqttEvent(QiscusMqttStatusEvent event) {
        view.onRealtimeStatusChanged(event == QiscusMqttStatusEvent.CONNECTED);
    }

    @Subscribe
    public void handleRetryCommentEvent(QiscusCommentResendEvent event) {
        if (event.getQiscusComment().getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.refreshComment(event.getQiscusComment());
                }
            });
        }
    }

    @Subscribe
    public void handleDeleteCommentEvent(QiscusCommentDeletedEvent event) {
        if (event.getQiscusComment().getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    if (event.isHardDelete()) {
                        view.onCommentDeleted(event.getQiscusComment());
                    } else {
                        view.refreshComment(event.getQiscusComment());
                    }
                }
            });
        }
    }

    @Subscribe
    public void onCommentReceivedEvent(QiscusCommentReceivedEvent event) {
        if (event.getQiscusComment().getRoomId() == room.getId()) {
            onGotNewComment(event.getQiscusComment());
        }
    }

    @Subscribe
    public void handleClearCommentsEvent(QiscusClearCommentsEvent event) {
        if (event.getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.clearCommentsBefore(event.getTimestamp());
                }
            });
        }
    }

    private void onGotNewComment(QiscusComment qiscusComment) {
        if (qiscusComment.getSenderEmail().equalsIgnoreCase(qiscusAccount.getEmail())) {
            QiscusAndroidUtil.runOnBackgroundThread(() -> commentSuccess(qiscusComment));
        } else {
            roomEventHandler.onGotComment(qiscusComment);
        }

        if (qiscusComment.getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnBackgroundThread(() -> {
                if (!qiscusComment.getSenderEmail().equalsIgnoreCase(qiscusAccount.getEmail())
                        && QiscusCacheManager.getInstance().getLastChatActivity().first) {
                    QiscusPusherApi.getInstance().markAsRead(room.getId(), qiscusComment.getId());
                }
            });
            view.onNewComment(qiscusComment);
        }
    }

    private void clearUnreadCount() {
        room.setUnreadCount(0);
        room.setLastComment(null);
        QiscusCore.getDataStore().addOrUpdate(room);
    }

    public void detachView() {
        roomEventHandler.detach();
        clearUnreadCount();
        room = null;
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onChatRoomNameChanged(String name) {
        room.setName(name);
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.onRoomChanged(room);
            }
        });
    }

    @Override
    public void onChatRoomMemberAdded(QiscusRoomMember member) {
        if (!room.getMember().contains(member)) {
            room.getMember().add(member);
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.onRoomChanged(room);
                }
            });
        }
    }

    @Override
    public void onChatRoomMemberRemoved(QiscusRoomMember member) {
        int x = room.getMember().indexOf(member);
        if (x >= 0) {
            room.getMember().remove(x);
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.onRoomChanged(room);
                }
            });
        }
    }

    @Override
    public void onUserTypng(String email, boolean typing) {
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.onUserTyping(email, typing);
            }
        });
    }

    @Override
    public void onChangeLastDelivered(long lastDeliveredCommentId) {
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.updateLastDeliveredComment(lastDeliveredCommentId);
            }
        });
    }

    @Override
    public void onChangeLastRead(long lastReadCommentId) {
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.updateLastReadComment(lastReadCommentId);
            }
        });
    }

    public interface View extends QiscusPresenter.View {

        void dismissLoading();

        void showError(String msg);

        void onRoomChanged(QiscusChatRoom qiscusChatRoom);

        void onSendingComment(QiscusComment qiscusComment);

        void onSuccessSendComment(QiscusComment qiscusComment);

        void onFailedSendComment(QiscusComment qiscusComment);

        void onNewComment(QiscusComment qiscusComment);

        void onCommentDeleted(QiscusComment qiscusComment);

        void refreshComment(QiscusComment qiscusComment);

        void updateLastDeliveredComment(long lastDeliveredCommentId);

        void updateLastReadComment(long lastReadCommentId);

        void onUserTyping(String user, boolean typing);

        void onRealtimeStatusChanged(boolean connected);

        void clearCommentsBefore(long timestamp);

    }
}

