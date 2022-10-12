package com.tencent.qcloud.tuikit.tuichat.bean.message;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.qcloud.tuikit.tuichat.bean.message.reply.TUIReplyQuoteBean;
import com.tencent.qcloud.tuikit.tuichat.bean.message.reply.TextReplyQuoteBean;
import com.tencent.qcloud.tuikit.tuichat.Encrypt.EncryptUtil;

public class TextMessageBean extends TUIMessageBean {
    private String text;

    @Override
    public String onGetDisplayString() {
        return text;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onProcessMessage(V2TIMMessage v2TIMMessage) {
        if (v2TIMMessage.getTextElem() != null) {
            text = v2TIMMessage.getTextElem().getText();
        }
        setExtra(text);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Class<? extends TUIReplyQuoteBean> getReplyQuoteBeanClass() {
        return TextReplyQuoteBean.class;
    }
}
