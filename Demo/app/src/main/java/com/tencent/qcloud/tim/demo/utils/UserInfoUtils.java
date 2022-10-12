package com.tencent.qcloud.tim.demo.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMUserFullInfo;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.qcloud.tim.demo.DemoApplication;
import com.tencent.qcloud.tim.demo.bean.SyncCallback;
import com.tencent.qcloud.tim.demo.bean.UserInfo;
import com.tencent.qcloud.tim.demo.dbUtil.MyDBHelper;
import com.tencent.qcloud.tim.demo.login.LoginForDevActivity;
import com.tencent.qcloud.tim.demo.main.MainActivity;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.tencent.qcloud.tuikit.tuichat.Encrypt.RSAKeyManager;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserInfoUtils {

    public static void checkPassword(String account, String psw, Activity activity){
        V2TIMManager v2TIMManager = V2TIMManager.getInstance();
        List<String> l = new ArrayList<>();
        l.add(account);
        v2TIMManager.getUsersInfo(l, new V2TIMValueCallback<List<V2TIMUserFullInfo>>() {
            @Override
            public void onSuccess(List<V2TIMUserFullInfo> v2TIMUserFullInfos) {
                HashMap<String,byte[]> customInfo = null;
                for (V2TIMUserFullInfo info :
                        v2TIMUserFullInfos) {
                    customInfo = info.getCustomInfo();
                }
                byte[] serverPSWMD5;
                try {
                    serverPSWMD5 = customInfo.get("PSW");
                    if (serverPSWMD5==null){throw new java.lang.NullPointerException();}
                }catch (java.lang.NullPointerException e){
                    e.printStackTrace();
                    pswLoginFail(activity);
                    return;
                }

                String pswMD5 = new String(customInfo.get("PSW"), Charset.defaultCharset());
                if (pswMD5.equals(MD5.md5Password(psw))){
                    pswLoginSuccess(activity);
                }
                else {
                    pswLoginFail(activity);
                }
            }

            @Override
            public void onError(int i, String s) {
                pswLoginFail(activity);
            }
        });

    }

    private static void pswLoginFail(Activity activity){
        TUILogin.logout(new TUICallback() {
            @Override
            public void onSuccess() {
                UserInfo.getInstance().cleanUserInfo();
            }

            @Override
            public void onError(int code, String desc) {
                ToastUtil.toastLongMessage("error");
                activity.finish();
            }
        });
        ToastUtil.toastLongMessage("用户名或密码错误");
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void pswLoginSuccess(Activity activity){
        UserInfo.getInstance().setAutoLogin(true);
        UserInfo.getInstance().setDebugLogin(true);
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);

        DemoApplication.instance().registerPushManually();


        activity.finish();
    }
    public static void setPassword(String account,String psw){
        V2TIMManager v2TIMManager = V2TIMManager.getInstance();
        List<String> l = new ArrayList<>();
        l.add(account);

        v2TIMManager.getUsersInfo(l, new V2TIMValueCallback<List<V2TIMUserFullInfo>>() {
            @Override
            public void onSuccess(List<V2TIMUserFullInfo> v2TIMUserFullInfos) {
                HashMap<String,byte[]> customInfo = null;
                V2TIMUserFullInfo myInfo = null;
                for (V2TIMUserFullInfo info :
                        v2TIMUserFullInfos) {
                    customInfo = info.getCustomInfo();

                    customInfo.put("PSW",MD5.md5Password(psw).getBytes(Charset.defaultCharset()));
                    info.setCustomInfo(customInfo);
                    myInfo = info;
                }

                v2TIMManager.setSelfInfo(myInfo, new V2TIMCallback() {
                    @Override
                    public void onSuccess() {
                        ToastUtil.toastShortMessage("注册成功");
                    }

                    @Override
                    public void onError(int i, String s) {
                        ToastUtil.toastShortMessage(s);
                    }
                });

            }

            @Override
            public void onError(int i, String s) {

            }
        });
    }
}
