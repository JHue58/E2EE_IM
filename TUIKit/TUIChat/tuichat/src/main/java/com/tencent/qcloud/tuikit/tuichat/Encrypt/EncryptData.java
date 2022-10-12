package com.tencent.qcloud.tuikit.tuichat.Encrypt;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class EncryptData implements Serializable {

    private byte[] data = null;
    public Boolean isEncrypt = false;
    public SecretKey secretKey =null;
    private HashMap<String, byte[]> EncryptSecretKeyMap = null;
    private String EncryptSecretKey = null;
    private String SenderEncryptSecretKey = null;
    private String groupID = null;
    private String userID = null;
    private String senderID = null;

    public String getOriginPath() {
        return originPath;
    }

    public void setOriginPath(String originPath) {
        this.originPath = originPath;
    }

    private String originPath = null;

    public String getDataMD5() {
        return dataMD5;
    }

    public String dataMD5;
    public Long encryptTime;

    private static final String ALGORITHM = "AES";
    private static final String HASH = "MD5";

    /**
     * 生成密钥
     *
     * @return
     * @throws
     */
    protected SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator secretGenerator = KeyGenerator.getInstance(ALGORITHM);
        SecureRandom secureRandom = new SecureRandom();
        secretGenerator.init(secureRandom);
        SecretKey secretKey = secretGenerator.generateKey();
        return secretKey;
    }

    static Charset charset = Charset.forName("UTF-8");

    /**
     * 加密
     *
     * @param content
     * @param secretKey
     * @return
     */
    protected byte[] encrypt(byte[] content, SecretKey secretKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException { // 加密
        return aes(content, Cipher.ENCRYPT_MODE, secretKey);
    }

    /**
     * 解密
     *
     * @param contentArray
     * @param secretKey
     * @return
     */
    protected byte[] decrypt(byte[] contentArray, SecretKey secretKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException { // 解密
        byte[] result = aes(contentArray, Cipher.DECRYPT_MODE, secretKey);
        return result;
    }

    private byte[] aes(byte[] contentArray, int mode, SecretKey secretKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, secretKey);
        byte[] result = cipher.doFinal(contentArray);
        return result;
    }

    public Boolean setEncryptData (byte[] data){
        MessageDigest digest =null;
        try {
            digest = MessageDigest.getInstance(HASH);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (digest==null){return false;}

        digest.reset();
        byte[] digest1 = digest.digest(data);
        if (digest1.length != 16){
            return false;
        }
        dataMD5 = EncryptUtil.bytesToString(digest1,16);
        this.data = data;
        return true;
    }

    public byte[] getEncryptData() {
        return data;
    }

    public void setConfig(EncryptConfig config){
        this.senderID = RSAKeyManager.getInstance().getMyUserID();
        if (!config.isGroupMessage()){
            this.userID = config.getUserID();
        }
        else{
            this.groupID = config.getGroupID();
        }
    }

    public Boolean Encrypt(EncryptConfig config) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        if (this.data == null){return isEncrypt;}
        setConfig(config);
        if (secretKey==null){
            secretKey = generateKey();
        }
        config.SelfMessage(secretKey);

        data = encrypt(data,secretKey);
        encryptTime = System.currentTimeMillis();
        isEncrypt = true;
        EncryptKey();
        return isEncrypt;

    }

    public Boolean deEnCrypt(EncryptConfig config) throws Exception {
        if (!isEncrypt){return false;}
        if (secretKey!=null){return false;}
        if (config.isSelfMessage()){
            secretKey = config.getSecretKey();
        }
        else {DecryptKey();}

        byte[] deCryptData = decrypt(data,secretKey);
        MessageDigest digest = MessageDigest.getInstance(HASH);
        digest.reset();
        byte[] digest1 = digest.digest(deCryptData);
        if (digest1.length != 16){
            return false;
        }
        String hash = EncryptUtil.bytesToString(digest1,16);
        if (!hash.equals(dataMD5)){return false;}
        data = deCryptData;
        isEncrypt = false;


        return !isEncrypt;

    }


    public Boolean isEncrypt(){
        if (data == null){
            return false;
        }
        return isEncrypt;
    }


    protected void EncryptKey(){
        if (secretKey == null){
            return;
        }

        byte[] keyBytes = EncryptUtil.ObjectToByteArray(secretKey);
        RSAKeyManager rsaKeyManager = RSAKeyManager.getInstance();
        if (groupID != null){
            EncryptSecretKeyMap = rsaKeyManager.getGroupRSAPublicKey(groupID).encrypt(keyBytes);
        }
        else{
            EncryptSecretKey = rsaKeyManager.encrypt(userID,keyBytes);
        }
        SenderEncryptSecretKey = rsaKeyManager.encrypt(rsaKeyManager.getMyPublicKey(), keyBytes);


        secretKey = null;

    }

    protected void DecryptKey() throws Exception {
        if (senderID==null){return;}
        if (senderID.equals(RSAKeyManager.getInstance().getMyUserID())){
            byte[] keyBytes = RSAUtils.decryptByPrivateKey(Base64.getDecoder().decode(SenderEncryptSecretKey),RSAKeyManager.getInstance().getMyPrivateKey());
            secretKey = (SecretKey) EncryptUtil.ByteArrayToObject(keyBytes);
            return;
        }


        if (EncryptSecretKey==null && EncryptSecretKeyMap==null){
            return;
        }
        // 私聊解密
        else if (EncryptSecretKey!=null && EncryptSecretKeyMap==null){

            byte[] keyBytes = RSAUtils.decryptByPrivateKey(Base64.getDecoder().decode(EncryptSecretKey),RSAKeyManager.getInstance().getMyPrivateKey());
            secretKey = (SecretKey) EncryptUtil.ByteArrayToObject(keyBytes);

        }
        // 群聊解密
        else {

            RSAKeyManager rsaKeyManager = RSAKeyManager.getInstance();
            byte[] keyBytes = RSAUtils.decryptByPrivateKey(EncryptSecretKeyMap.get(rsaKeyManager.getMyUserID()),rsaKeyManager.getMyPrivateKey());
            secretKey = (SecretKey) EncryptUtil.ByteArrayToObject(keyBytes);

        }
    }











}
