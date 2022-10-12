package com.tencent.qcloud.tuikit.tuichat.Encrypt;

import java.util.HashMap;

public class EncryptManager {
    private static EncryptManager encryptManager = null;
    private HashMap<String,String> fileMD5PathOriginPathMap = new HashMap<>();

    public static EncryptManager getInstance(){
        if (encryptManager==null){
            encryptManager = new EncryptManager();
        }
        return encryptManager;
    }

    public String getFileOriginPath(String fileMD5Path){
        if (!fileMD5PathOriginPathMap.containsKey(fileMD5Path)){
            return null;
        }
        return fileMD5PathOriginPathMap.get(fileMD5Path);
    }
    public void setFileMD5HitOriginPath(String fileMD5Path,String originPath){
        fileMD5PathOriginPathMap.put(fileMD5Path,originPath);
    }

}
