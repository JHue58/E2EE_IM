package com.tencent.qcloud.tuikit.tuichat.Encrypt;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.tencent.imsdk.common.IMCallback;
import com.tencent.imsdk.group.GroupInfo;
import com.tencent.imsdk.group.GroupManager;
import com.tencent.imsdk.group.GroupMemberInfo;
import com.tencent.imsdk.group.GroupMemberInfoResult;
import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMCompleteCallback;
import com.tencent.imsdk.v2.V2TIMGroupManager;
import com.tencent.imsdk.v2.V2TIMGroupMemberFullInfo;
import com.tencent.imsdk.v2.V2TIMGroupMemberInfo;
import com.tencent.imsdk.v2.V2TIMGroupMemberInfoResult;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMUserFullInfo;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.qcloud.tuicore.util.ToastUtil;

import java.nio.charset.Charset;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
@RequiresApi(api = Build.VERSION_CODES.O)
public class GroupRSAPublicKey {
    private class GetGroupMemberListCallback implements V2TIMValueCallback<V2TIMGroupMemberInfoResult> {
        @Override
        public void onSuccess(V2TIMGroupMemberInfoResult v2TIMGroupMemberInfoResult) {
            List<String> userIDList = new ArrayList<>();
            for (V2TIMGroupMemberInfo memberInfo :
                    v2TIMGroupMemberInfoResult.getMemberInfoList()) {
                String userID = memberInfo.getUserID();
                userIDList.add(userID);
            }

            V2TIMManager v2TIMManager = V2TIMManager.getInstance();
            v2TIMManager.getUsersInfo(userIDList, new V2TIMValueCallback<List<V2TIMUserFullInfo>>() {

                @Override
                public void onSuccess(List<V2TIMUserFullInfo> v2TIMUserFullInfos) {
                    for (V2TIMUserFullInfo info :
                            v2TIMUserFullInfos) {
                        HashMap<String,byte[]> customInfo = info.getCustomInfo();
                        String publicKey = new String(customInfo.get("RSAKEY"), Charset.defaultCharset());
                        try {
                            rsaPublicKeyHashMap.put(info.getUserID(),RSAUtils.getPublicKey(publicKey));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(int i, String s) {
                    ToastUtil.toastLongMessage(s);
                }
            });


            if (v2TIMGroupMemberInfoResult.getNextSeq()!=0){
                V2TIMManager.getGroupManager().getGroupMemberList(groupID, V2TIMGroupMemberFullInfo.V2TIM_GROUP_MEMBER_FILTER_ALL,
                        v2TIMGroupMemberInfoResult.getNextSeq(), this);
            }
        }

        @Override
        public void onError(int i, String s) {
            ToastUtil.toastLongMessage(s);
        }
    }


    private HashMap<String, RSAPublicKey> rsaPublicKeyHashMap = new HashMap<>();
    private String groupID;
    private Activity activity;

    public GroupRSAPublicKey(String groupID,Activity activity) {
        this.groupID = groupID;
        this.activity = activity;
        initRSAPublicKey(groupID);
    }

    public GroupRSAPublicKey(String groupID){
        this.groupID = groupID;
        initRSAPublicKey(groupID);
    }

    private void initRSAPublicKey(String groupID){
        V2TIMGroupManager groupManager = V2TIMManager.getGroupManager();
        groupManager.getGroupMemberList(groupID, V2TIMGroupMemberFullInfo.V2TIM_GROUP_MEMBER_FILTER_ALL,
                0, new GetGroupMemberListCallback());
    }

    public HashMap<String,byte[]> encrypt(byte[] data){
        HashMap<String,byte[]> encryptMap = new HashMap<>();

        for (String userID:
                rsaPublicKeyHashMap.keySet()) {
            try {
                byte[] encryptData = RSAUtils.encryptByPublicKey(data,rsaPublicKeyHashMap.get(userID));
                encryptMap.put(userID,encryptData);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return encryptMap;
    }

    public void addMember(String userID){
        List<String> userIDList = new ArrayList<>();
        userIDList.add(userID);
        V2TIMManager v2TIMManager = V2TIMManager.getInstance();
        v2TIMManager.getUsersInfo(userIDList, new V2TIMValueCallback<List<V2TIMUserFullInfo>>() {

            @Override
            public void onSuccess(List<V2TIMUserFullInfo> v2TIMUserFullInfos) {
                for (V2TIMUserFullInfo info :
                        v2TIMUserFullInfos) {
                    HashMap<String,byte[]> customInfo = info.getCustomInfo();
                    String publicKey = new String(customInfo.get("RSAKEY"), Charset.defaultCharset());
                    try {
                        rsaPublicKeyHashMap.put(info.getUserID(),RSAUtils.getPublicKey(publicKey));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(int i, String s) {

            }
        });

    }

    public void removeMember(String userID){
        rsaPublicKeyHashMap.remove(userID);
    }
}
