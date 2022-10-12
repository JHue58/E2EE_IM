package com.tencent.qcloud.tim.demo.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tim.demo.DemoApplication;
import com.tencent.qcloud.tim.demo.R;
import com.tencent.qcloud.tim.demo.bean.UserInfo;
import com.tencent.qcloud.tim.demo.signature.GenerateTestUserSig;
import com.tencent.qcloud.tim.demo.utils.TUIKitConstants;
import com.tencent.qcloud.tim.demo.utils.TUIUtils;
import com.tencent.qcloud.tim.demo.utils.UserInfoUtils;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.tencent.qcloud.tuicore.util.ToastUtil;

public class RegisterForDevActivity extends BaseLightActivity {

    private TextView mRegisterView;
    private EditText mUserAccount;
    private EditText mUserPassword;
    private EditText mUserPasswordAgain;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_for_dev_activity);
        initActivity();



    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK ){
            Intent intent = new Intent(RegisterForDevActivity.this,LoginForDevActivity.class);
            startActivity(intent);
            this.finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initActivity(){
        mRegisterView = findViewById(R.id.first_register_btn);
        mUserAccount = findViewById(R.id.user_register);
        mUserPassword = findViewById(R.id.password_register);
        mUserPasswordAgain = findViewById(R.id.register_password_again);

        mRegisterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DemoApplication.instance().init(0);
                String account = mUserAccount.getText().toString();
                String password = mUserPassword.getText().toString();
                String passwordAgain = mUserPasswordAgain.getText().toString();
                if (password.equals("")){
                    ToastUtil.toastLongMessage("密码不能为空");
                    return;
                }
                if (password.indexOf(" ")!=-1){
                    ToastUtil.toastLongMessage("密码不能有空格");
                    return;
                }
                if (!password.equals(passwordAgain)){
                    ToastUtil.toastLongMessage("密码不一致");
                    return;
                }
                DemoApplication.instance().init(0);
                UserInfo.getInstance().setUserId(account);
                String userSig = GenerateTestUserSig.genTestUserSig(account);
                UserInfo.getInstance().setUserSig(userSig);
                TUILogin.login(DemoApplication.instance(), DemoApplication.instance().getSdkAppId(), account, userSig, new TUICallback() {
                    @Override
                    public void onSuccess() {
                        UserInfoUtils.setPassword(account,password);

                        UserInfo.getInstance().cleanUserInfo();
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(TUIKitConstants.LOGOUT, true);
                        TUIUtils.startActivity("LoginForDevActivity", bundle);
                        finish();




                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) {
                        ToastUtil.toastLongMessage("error");
                    }
                });
            }
        });


    }
}
