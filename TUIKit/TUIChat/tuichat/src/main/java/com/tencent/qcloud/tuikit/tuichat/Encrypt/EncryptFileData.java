package com.tencent.qcloud.tuikit.tuichat.Encrypt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class EncryptFileData extends EncryptData{
    enum FileLengthStatus{
        MORE_THAN_1M,
        EQUAL_TO_1M,
        LESS_THAN_1M;
    }

    private final int MAX_FILE_BYTE_LENGTH = 1048576;  // 1M
    private int lastFileLength = 0;
    private int firstFileLength = 0;
    private File file = null;
    private FileLengthStatus fileLengthStatus;
    public int encryptFileCount = 0; // 分卷加密后,加密文件的数量

    private int[] encryptDataBytesLength;

    public void setFile(File file) {
        this.file = file;
    }

    public void setEncryptData(File file) {
        this.file = file;
        dataMD5 = getMD5(file);
        if (file.length()==MAX_FILE_BYTE_LENGTH){
            fileLengthStatus = FileLengthStatus.EQUAL_TO_1M;
            encryptFileCount = 1;
        }
        else if(file.length()>MAX_FILE_BYTE_LENGTH){
            fileLengthStatus = FileLengthStatus.MORE_THAN_1M;
            lastFileLength = Math.toIntExact(file.length() % MAX_FILE_BYTE_LENGTH);
            encryptFileCount = (int)Math.ceil((double) file.length()/MAX_FILE_BYTE_LENGTH);
        }
        else{
            fileLengthStatus = FileLengthStatus.LESS_THAN_1M;
            firstFileLength = Math.toIntExact(file.length());
            encryptFileCount = 1;
        }
        encryptDataBytesLength = new int[encryptFileCount];
    }

    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return EncryptUtil.bytesToString(MD5.digest(),16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileInputStream != null){
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Boolean Encrypt(EncryptConfig config) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        if (file == null){return isEncrypt;}
        setConfig(config);
        if (secretKey==null){
            secretKey = generateKey();
        }
        config.SelfMessage(secretKey);
        isEncrypt = EncryptOneByOne();
        encryptTime = System.currentTimeMillis();
        EncryptKey();
        return isEncrypt;
    }

    @Override
    public Boolean deEnCrypt(EncryptConfig config) throws Exception {
        if (!isEncrypt){return false;}
        if (secretKey!=null){return false;}
        if (config.isSelfMessage()){
            secretKey = config.getSecretKey();
        }
        else {DecryptKey();}

        isEncrypt = !DeEncryptOneByOne();

        return !isEncrypt;
    }

    // TODO 文件分批加解密存在问题
    private Boolean EncryptOneByOne() throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Boolean flag = true;
        String filename = file.getName();
        String suffix = filename.substring(filename.lastIndexOf('.'));
        String newPath = file.getParentFile() + File.separator + dataMD5 + "_";
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        byte[] dataBytes;
        byte[] encryptDataBytes;
        switch (fileLengthStatus){
            case LESS_THAN_1M:
                dataBytes = new byte[Math.toIntExact(file.length())];
                bufferedInputStream.read(dataBytes);
                int tex = bufferedInputStream.read();
                System.out.println(tex);
                encryptDataBytes = encrypt(dataBytes,secretKey);
                saveBytesToFile(newPath+"0"+suffix,encryptDataBytes);
                encryptDataBytesLength[0] = encryptDataBytes.length;
                break;
            case EQUAL_TO_1M:
                dataBytes = new byte[MAX_FILE_BYTE_LENGTH];
                bufferedInputStream.read(dataBytes);
                encryptDataBytes = encrypt(dataBytes,secretKey);
                saveBytesToFile(newPath+"0"+suffix,encryptDataBytes);
                encryptDataBytesLength[0] = encryptDataBytes.length;
                break;
            case MORE_THAN_1M:
                for (int i = 0; i < encryptDataBytesLength.length; i++) {
                    if (i==encryptDataBytesLength.length-1){
                        dataBytes = new byte[lastFileLength];
                    }
                    else{dataBytes = new byte[MAX_FILE_BYTE_LENGTH];}
                    bufferedInputStream.read(dataBytes);
                    encryptDataBytes = encrypt(dataBytes,secretKey);
                    saveBytesToFile(newPath+i+suffix,encryptDataBytes);
                    encryptDataBytesLength[i] = encryptDataBytes.length;
                }
                int x = bufferedInputStream.read();
                break;
        }
        if (bufferedInputStream.read()!=-1){flag = false;}
        fileInputStream.close();bufferedInputStream.close();
        //saveBytesToFile(newPath+"config"+suffix,EncryptUtil.ObjectToByteArray(this));

        return flag;

    }

    private Boolean DeEncryptOneByOne() throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Boolean flag = true;
        String filename = file.getName();
        String suffix = filename.substring(filename.lastIndexOf('.'));
        String newPath = file.getParentFile() + File.separator + dataMD5 + "_";


        file.delete();//清空未解压的文件
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        byte[] dataBytes;
        byte[] encryptDataBytes;
        switch (fileLengthStatus){
            case LESS_THAN_1M:
            case EQUAL_TO_1M:
                encryptDataBytes = new byte[encryptDataBytesLength[0]];
                readFileToBytes(newPath+"0"+suffix,encryptDataBytes);
                dataBytes = decrypt(encryptDataBytes,secretKey);
                bufferedOutputStream.write(dataBytes);
                break;
            case MORE_THAN_1M:
                for (int i = 0; i < encryptDataBytesLength.length; i++) {
                    encryptDataBytes = new byte[encryptDataBytesLength[i]];
                    readFileToBytes(newPath+i+suffix,encryptDataBytes);
                    dataBytes = decrypt(encryptDataBytes,secretKey);
                    bufferedOutputStream.write(dataBytes);
                }
                break;
        }
        fileOutputStream.close();bufferedOutputStream.close();
        if (!getMD5(file).equals(dataMD5)){flag = false;}
        new File(newPath+"config"+suffix).delete();


        return flag;

    }

    public static void saveBytesToFile(String path,byte[] bytes) throws IOException {
        File file = new File(path);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        bufferedOutputStream.write(bytes);
        bufferedOutputStream.close();fileOutputStream.close();
    }
    public static void readFileToBytes(String path,byte[] bytes) throws IOException {
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(bytes);
        bufferedInputStream.close();fileInputStream.close();
        file.delete();
    }
}
