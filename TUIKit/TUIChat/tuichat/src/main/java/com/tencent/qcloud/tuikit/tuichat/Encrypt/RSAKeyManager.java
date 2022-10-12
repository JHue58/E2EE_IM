package com.tencent.qcloud.tuikit.tuichat.Encrypt;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.tencent.imsdk.common.IMCallback;
import com.tencent.imsdk.group.GroupInfo;
import com.tencent.imsdk.group.GroupManager;
import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMCompleteCallback;
import com.tencent.imsdk.v2.V2TIMFriendInfo;
import com.tencent.imsdk.v2.V2TIMFriendshipManager;
import com.tencent.imsdk.v2.V2TIMGroupInfo;
import com.tencent.imsdk.v2.V2TIMGroupListener;
import com.tencent.imsdk.v2.V2TIMGroupManager;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMUserFullInfo;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.qcloud.tuicore.util.ToastUtil;

import java.nio.charset.Charset;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
@RequiresApi(api = Build.VERSION_CODES.O)
public class RSAKeyManager {

    private class GetJoinedGroupListCallback implements V2TIMValueCallback<List<V2TIMGroupInfo>> {


        @Override
        public void onSuccess(List<V2TIMGroupInfo> groupInfos) {
            for (V2TIMGroupInfo groupInfo :
                    groupInfos) {
                String groupID = groupInfo.getGroupID();
                GroupRSAPublicKey groupRSAPublicKey = new GroupRSAPublicKey(groupID,activity);
                groupRSAPublicKeyHashMap.put(groupID,groupRSAPublicKey);
            }
        }

        @Override
        public void onError(int i, String s) {

        }
    }
    private class GetFriendListCallback implements V2TIMValueCallback<List<V2TIMFriendInfo>>{

        @Override
        public void onSuccess(List<V2TIMFriendInfo> v2TIMFriendInfos) {
            List<String> userIDList = new ArrayList<>();
            for (V2TIMFriendInfo friendInfo :
                    v2TIMFriendInfos) {
                userIDList.add(friendInfo.getUserID());
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
                            userPublicKeyHashMap.put(info.getUserID(),RSAUtils.getPublicKey(publicKey));
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
        }

        @Override
        public void onError(int i, String s) {
            ToastUtil.toastLongMessage(s);
        }
    }


    private static RSAKeyManager rsaKeyManager = null;

    private RSAPrivateKey myPrivateKey = null;
    private RSAPublicKey myPublicKey = null;
    private String myUserID;

    private HashMap<String,GroupRSAPublicKey> groupRSAPublicKeyHashMap = new HashMap<>();
    private HashMap<String, RSAPublicKey> userPublicKeyHashMap = new HashMap<>();

    private Activity activity;

    public RSAKeyManager(Activity activity,String myUserID) {
        this.activity = activity;
        this.myUserID = myUserID;
        initRsaKey();
    }

    // 获取单例对象
    public static RSAKeyManager getInstance(){
        return rsaKeyManager;
    }

    public static void setInstance(Activity activity,String myUserID){
        rsaKeyManager = new RSAKeyManager(activity,myUserID);
    }


    public void setMyPrivateKey(String prkString){
        try {
            myPrivateKey = RSAUtils.getPrivateKey(prkString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMyPublicKey(String pruString){
        try {
            myPublicKey = RSAUtils.getPublicKey(pruString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RSAPrivateKey getMyPrivateKey(){
        return myPrivateKey;
    }

    public RSAPublicKey getMyPublicKey() {
        return myPublicKey;
    }

    public String getMyUserID(){
        return myUserID;
    }

    private void initRsaKey(){
        V2TIMGroupManager groupManager = V2TIMManager.getGroupManager();
        V2TIMFriendshipManager friendshipManager = V2TIMManager.getFriendshipManager();
        groupManager.getJoinedGroupList(new GetJoinedGroupListCallback());
        friendshipManager.getFriendList(new GetFriendListCallback());

    }

    public GroupRSAPublicKey getGroupRSAPublicKey(String groupID){
        return groupRSAPublicKeyHashMap.get(groupID);
    }




    public String encrypt(String userID,byte[] data){
        byte[] temp;
        try {
            temp =  RSAUtils.encryptByPublicKey(data,userPublicKeyHashMap.get(userID));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String base64 = Base64.getEncoder().encodeToString(temp);
        return base64;

    }
    public String encrypt(RSAPublicKey publicKey,byte[] data){
        byte[] temp;
        try {
            temp = RSAUtils.encryptByPublicKey(data,publicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String base64 = Base64.getEncoder().encodeToString(temp);
        return base64;
    }

    public void addGroupRSAPublicKey(String groupID){
        if (groupRSAPublicKeyHashMap.containsKey(groupID)){
            return;
        }
        GroupRSAPublicKey groupRSAPublicKey = new GroupRSAPublicKey(groupID);
        groupRSAPublicKeyHashMap.put(groupID,groupRSAPublicKey);
    }

    public void removeGroupRSAPublicKey(String groupID){
        if (groupRSAPublicKeyHashMap.containsKey(groupID)){
            groupRSAPublicKeyHashMap.remove(groupID);
        }
    }

    public void addFriend(String userID){
        if (userPublicKeyHashMap.containsKey(userID)){
            return;
        }
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
                        userPublicKeyHashMap.put(info.getUserID(),RSAUtils.getPublicKey(publicKey));
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
    public void removeFriend(String userID){
        if (userPublicKeyHashMap.containsKey(userID)) {
            userPublicKeyHashMap.remove(userID);
        }
    }
}
