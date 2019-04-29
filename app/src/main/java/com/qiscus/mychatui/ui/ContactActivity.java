package com.qiscus.mychatui.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.presenter.ContactPresenter;
import com.qiscus.mychatui.ui.adapter.ContactAdapter;
import com.qiscus.mychatui.ui.adapter.OnItemClickListener;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;

import java.util.List;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ContactActivity extends AppCompatActivity implements ContactPresenter.View, OnItemClickListener {
    private RecyclerView recyclerView;
    private ContactAdapter contactAdapter;

    private ContactPresenter contactPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        contactAdapter = new ContactAdapter(this);
        contactAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(contactAdapter);

        contactPresenter = new ContactPresenter(this,
                MyApplication.getInstance().getComponent().getUserRepository(),
                MyApplication.getInstance().getComponent().getChatRoomRepository());
        contactPresenter.loadContacts(1,100, "");
    }

    @Override
    public void showContacts(List<User> contacts) {
        contactAdapter.addOrUpdate(contacts);
    }

    @Override
    public void showChatRoomPage(QiscusChatRoom chatRoom) {
        startActivity(ChatRoomActivity.generateIntent(this, chatRoom));
    }


    @Override
    public void showErrorMessage(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(int position) {
        contactPresenter.createRoom(contactAdapter.getData().get(position));
    }
}
