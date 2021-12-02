package com.qiscus.mychatui.presenter;

import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.HttpException;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Jul, Fri 27 2018 12.52
 **/
public class ChatRoomPresenter extends QiscusPresenter<ChatRoomPresenter.View> {

    private QiscusChatRoom room;

    private Map<QiscusComment, Subscription> pendingTask;

    public ChatRoomPresenter(View view, QiscusChatRoom room) {
        super(view);
        this.view = view;

        this.room = room;
        if (this.room.getMember().isEmpty()) {
            this.room = QiscusCore.getDataStore().getChatRoom(room.getId());
        }
        pendingTask = new HashMap<>();
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

    public void initiateCall() {
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
        endCall(comment, "Call Canceled");
    }

    public void endCall(QiscusComment comment, String message) {
        Map<String, Object> map = new HashMap<>();
        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(comment.getRoomId(), message, QiscusMeetUtil.CallType.CALL, new JSONObject(map));
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

    private void clearUnreadCount() {
        room.setUnreadCount(0);
        room.setLastComment(null);
        QiscusCore.getDataStore().addOrUpdate(room);
    }

    public void detachView() {
        clearUnreadCount();
        room = null;
    }

    public interface View extends QiscusPresenter.View {

        void onSendingComment(QiscusComment qiscusComment);

        void onSuccessSendComment(QiscusComment qiscusComment);

        void onFailedSendComment(QiscusComment qiscusComment);

    }
}