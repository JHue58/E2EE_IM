package com.tencent.qcloud.tuikit.tuiconversation.ui.page;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity;
import com.tencent.qcloud.tuikit.tuiconversation.R;
import com.tencent.qcloud.tuikit.tuiconversation.ui.page.TUIForwardSelectActivity;
import com.tencent.qcloud.tuikit.tuiconversation.util.TUIConversationLog;

public class MessageShareSelectActivity extends BaseLightActivity {
    private static final String TAG = TUIForwardSelectActivity.class.getSimpleName();

    private com.tencent.qcloud.tim.demo.main.MessageShareSelectFragment mTUIForwardSelectFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forward_activity);

        init();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        TUIConversationLog.i(TAG, "onNewIntent");
        super.onNewIntent(intent);;
    }

    @Override
    protected void onResume() {
        TUIConversationLog.i(TAG, "onResume");
        super.onResume();
    }

    private void init() {
        mTUIForwardSelectFragment = new com.tencent.qcloud.tim.demo.main.MessageShareSelectFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.empty_view, mTUIForwardSelectFragment).commitAllowingStateLoss();
    }
}
