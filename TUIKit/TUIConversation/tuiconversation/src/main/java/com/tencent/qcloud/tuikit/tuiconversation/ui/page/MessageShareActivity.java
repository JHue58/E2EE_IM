package com.tencent.qcloud.tuikit.tuiconversation.ui.page;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.tencent.qcloud.tuikit.tuichat.TUIChatConstants;
import com.tencent.qcloud.tuikit.tuichat.bean.message.TUIMessageBean;
import com.tencent.qcloud.tuikit.tuichat.ui.view.ChatView;
import com.tencent.qcloud.tuikit.tuichat.util.ChatMessageBuilder;

import java.util.ArrayList;
import java.util.List;

public class MessageShareActivity extends BaseLightActivity {
    private List<TUIMessageBean> mForwardSelectMsgInfos = null;
    private int mForwardMode;
    private class ForwardSelectActivity implements ChatView.ForwardSelectActivityListener{

        @Override
        public void onStartForwardSelectActivity(int mode, List<TUIMessageBean> msgIds) {
            mForwardMode = mode;
            mForwardSelectMsgInfos = msgIds;
            Intent intent = new Intent();
            intent.putExtra("Message",msgIds.get(0));
            intent.setClass(MessageShareActivity.this,MessageShareSelectActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (!TUILogin.isUserLogined()){
            ToastUtil.toastLongMessage("请先登录E2EE·IM");
            finish();
            return;
        }

        if (Intent.ACTION_SEND.equals(action) && type!=null && "image/jpg".equals(type)){
            sendImage(intent);
        }
        else if (Intent.ACTION_SEND.equals(action) && type!=null && "image/bmp".equals(type)){
            sendImage(intent);
        }
        else if (Intent.ACTION_SEND.equals(action) && type!=null && "image/png".equals(type)){
            sendImage(intent);
        }
        else if (Intent.ACTION_SEND.equals(action) && type!=null && "text/plain".equals(type)){
            sendText(intent);
        }
        else{
            sendFile(intent);
        }

        finish();

    }
    private void sendImage(Intent intent){
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        TUIMessageBean msg = ChatMessageBuilder.buildImageMessage(uri);
        List<TUIMessageBean> msgList = new ArrayList<>();
        msgList.add(msg);
        if (msg!=null){
            ForwardSelectActivity forwardSelectActivity = new ForwardSelectActivity();
            forwardSelectActivity.onStartForwardSelectActivity(TUIChatConstants.FORWARD_MODE_ONE_BY_ONE,msgList);
        }

    }
    private void sendText(Intent intent){
        TUIMessageBean msg = ChatMessageBuilder.buildTextMessage(intent.getStringExtra(Intent.EXTRA_TEXT));
        List<TUIMessageBean> msgList = new ArrayList<>();
        msgList.add(msg);
        if (msg!=null){
            ForwardSelectActivity forwardSelectActivity = new ForwardSelectActivity();
            forwardSelectActivity.onStartForwardSelectActivity(TUIChatConstants.FORWARD_MODE_ONE_BY_ONE,msgList);
        }

    }
    private void sendFile(Intent intent){
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        TUIMessageBean msg = ChatMessageBuilder.buildFileMessage(uri);
        List<TUIMessageBean> msgList = new ArrayList<>();
        msgList.add(msg);
        if (msg!=null){
            ForwardSelectActivity forwardSelectActivity = new ForwardSelectActivity();
            forwardSelectActivity.onStartForwardSelectActivity(TUIChatConstants.FORWARD_MODE_ONE_BY_ONE,msgList);
        }
    }

}
