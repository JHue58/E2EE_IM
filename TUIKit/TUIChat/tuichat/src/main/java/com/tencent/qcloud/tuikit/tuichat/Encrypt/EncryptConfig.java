package com.tencent.qcloud.tuikit.tuichat.Encrypt;

import com.tencent.imsdk.v2.V2TIMMessage;

import javax.crypto.SecretKey;

public class EncryptConfig {
    private V2TIMMessage message;
    private String groupID="";
    private String userID="";
    private Boolean isSelfMessage = false;
    private Boolean isEncrypt = true;
    private SecretKey secretKey;

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public EncryptConfig(V2TIMMessage message) {
        this.message = message;
    }

    public void setMessage(V2TIMMessage message) {
        this.message = message;
    }

    public void SelfMessage(SecretKey secretKey){
        isSelfMessage = true;
        this.secretKey = secretKey;
    }
    public Boolean isSelfMessage(){
        return isSelfMessage;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getGroupID() {
        return groupID;
    }

    public String getUserID() {
        return userID;
    }

    public V2TIMMessage getMessage() {
        return message;
    }

    public Boolean isGroupMessage(){
        if (groupID.equals("")){
            return false;
        }
        return true;
    }
    public void encrypt(){
        isEncrypt = true;
    }
    public void deEncrypt(){
        isEncrypt = false;
    }
    public Boolean isEncrypt(){
        return isEncrypt;
    }

}
