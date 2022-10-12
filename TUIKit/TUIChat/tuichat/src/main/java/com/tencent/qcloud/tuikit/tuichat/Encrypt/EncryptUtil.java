package com.tencent.qcloud.tuikit.tuichat.Encrypt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentManager;

import com.tencent.imsdk.message.ImageElement;
import com.tencent.imsdk.message.VideoElement;
import com.tencent.imsdk.v2.V2TIMFileElem;
import com.tencent.imsdk.v2.V2TIMImageElem;
import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.imsdk.v2.V2TIMSoundElem;
import com.tencent.imsdk.v2.V2TIMTextElem;
import com.tencent.imsdk.v2.V2TIMVideoElem;
import com.tencent.qcloud.tuicore.util.TUIUtil;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.tencent.qcloud.tuikit.tuichat.Encrypt.EncryptData;
import com.tencent.qcloud.tuikit.tuichat.R;
import com.tencent.qcloud.tuikit.tuichat.TUIChatConstants;
import com.tencent.qcloud.tuikit.tuichat.TUIChatService;
import com.tencent.qcloud.tuikit.tuichat.ui.page.TUIC2CChatActivity;
import com.tencent.qcloud.tuikit.tuichat.util.FileReaderUtils;
import com.tencent.qcloud.tuikit.tuichat.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class EncryptUtil {


    private static final int BASE_IMAGE_LENGTH = 2332;
    private static final int BASE_AUDIO_LENGTH = 5502;
    private static final String ENCRYPT_LOADING = "数据加密中";
    private static final String ENCRYPT_SUCCESS = "加密成功";
    private static final String ENCRYPT_FAIL = "加密失败";
    private static final String ENCRYPT_COMPLETE = "处理完成";
    private static final String DE_ENCRYPT_LOADING = "数据解密中";
    private static final String DE_ENCRYPT_SUCCESS = "解密成功";
    private static final String DE_ENCRYPT_FAIL = "解密失败";
    private static final String AUDIO_CANNOT_ENCRYPT_TIPS = "由于流媒体的特殊性，语音无法被加密，若需要加密，请使用发送文件";
    private static final String VIDEO_CANNOT_ENCRYPT_TIPS = "由于流媒体的特殊性，视频无法被加密，若需要加密，请使用发送文件";
    private static final String MESSAGE_DECRYPT_TIPS = "**消息解密失败**";



    public static void encryptMessage(EncryptConfig encryptConfig){
        V2TIMMessage message = encryptConfig.getMessage();
        int msgType = message.getElemType();

        switch (msgType){
            case V2TIMMessage.V2TIM_ELEM_TYPE_TEXT:
                if (encryptConfig.isEncrypt()) {
                    stringEncrypt(encryptConfig);
                } else {
                    stringDeEncrypt(encryptConfig);
                }
                break;
            case V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE:
                if (encryptConfig.isEncrypt()) {
                    imageEncrypt(encryptConfig);
                }
                break;
            case V2TIMMessage.V2TIM_ELEM_TYPE_FILE:
                if (encryptConfig.isEncrypt()){
                    fileEncrypt(encryptConfig);
                }
                else {
                    File file = new File(message.getFileElem().getPath());
                    if (file.exists()){file.delete();}
                }
                break;
            case V2TIMMessage.V2TIM_ELEM_TYPE_SOUND:
                if (encryptConfig.isEncrypt()){
                    ToastUtil.toastLongMessage(AUDIO_CANNOT_ENCRYPT_TIPS);
                }
                break;
            case V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO:
                if (encryptConfig.isEncrypt()){
                    ToastUtil.toastLongMessage(VIDEO_CANNOT_ENCRYPT_TIPS);
                }
                break;

        }
    }






    public static void stringEncrypt(EncryptConfig encryptConfig){
        V2TIMMessage message = encryptConfig.getMessage();
        String str = message.getTextElem().getText();

        EncryptData encryptData = new EncryptData();
        encryptData.setEncryptData(str.getBytes(Charset.defaultCharset()));
        try {
            Boolean flag = encryptData.Encrypt(encryptConfig);
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | IOException e) {
            e.printStackTrace();
            ToastUtil.toastLongMessage(ENCRYPT_FAIL);
            return;
        }
        byte[] bytes = null;

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(encryptData);
            bytes = bo.toByteArray();
            bo.close();oo.close();
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtil.toastLongMessage(ENCRYPT_FAIL);
            return;
        }

        String newStr = new String(bytes,StandardCharsets.ISO_8859_1);

        message.getTextElem().setText(newStr);



    }



    public static void stringDeEncrypt(EncryptConfig encryptConfig){
        V2TIMMessage message = encryptConfig.getMessage();
        String str = message.getTextElem().getText();


        byte[] obj = str.getBytes(StandardCharsets.ISO_8859_1);

        String msg = str;
        EncryptData encryptData = null;

        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(obj);
            ObjectInputStream oo = new ObjectInputStream(bi);
            encryptData = (EncryptData) oo.readObject();oo.close();bi.close();
            Boolean flag = encryptData.deEnCrypt(encryptConfig);
            msg = new String(encryptData.getEncryptData(),Charset.defaultCharset());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e){
            e.printStackTrace();
            return;
        }catch (Exception e) {
            e.printStackTrace();
            message.getTextElem().setText(MESSAGE_DECRYPT_TIPS);
            return;
        }

        message.getTextElem().setText(msg);



    }



    public static String fileEncrypt(EncryptConfig encryptConfig){
        V2TIMMessage message = encryptConfig.getMessage();
        V2TIMFileElem fileElem = message.getFileElem();
        String path = fileElem.getPath();


        ToastUtil.toastLongMessage(ENCRYPT_LOADING);
        String newPath = path;

        File file = new File(path);
        EncryptFileData encryptFileData = new EncryptFileData();
        encryptFileData.setEncryptData(file);
        Boolean flag = null;
        try {
            flag = encryptFileData.Encrypt(encryptConfig);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException | InvalidKeyException e) {
            ToastUtil.toastLongMessage(ENCRYPT_FAIL);
            return path;
        }

        String filename = file.getName();
        String suffix = filename.substring(filename.lastIndexOf('.'));
        String tempPath = file.getParentFile() + File.separator + encryptFileData.getDataMD5() + "_";
        String configPath = file.getParentFile()+File.separator+"config"+suffix;

        try {
            EncryptFileData.saveBytesToFile(configPath,ObjectToByteArray(encryptFileData));
            List<String> filePathList = new ArrayList<>();
            filePathList.add(configPath);
            for (int i = 0; i < encryptFileData.encryptFileCount; i++) {
                filePathList.add(tempPath+i+suffix);
            }
            // 压缩加密包
            compressFiles(file.getParentFile()+File.separator,filename,filePathList);

            if (flag==false){
                ToastUtil.toastLongMessage(ENCRYPT_FAIL);
            }else{ToastUtil.toastLongMessage(ENCRYPT_SUCCESS);}

        } catch (IOException e) {
            e.printStackTrace();
        }


        return newPath;
    }

    public static String fileDeEncrypt(String path){

        ToastUtil.toastShortMessage(DE_ENCRYPT_LOADING);
        try {
            unzip(path);
            File file = new File(path);
            String filename = file.getName();
            String suffix = filename.substring(filename.lastIndexOf('.'));
            String tempPath = file.getParentFile() + File.separator + "config"+suffix;
            EncryptFileData encryptFileData = (EncryptFileData) ByteArrayToObject(FileReaderUtils.readByNIO(new File(tempPath)));
            encryptFileData.setFile(file);
            Boolean flag = encryptFileData.deEnCrypt(new EncryptConfig(null));
            //if (file.exists()){file.delete();}
            if (flag==false){
                ToastUtil.toastLongMessage(DE_ENCRYPT_FAIL);
            }
            else {ToastUtil.toastLongMessage(DE_ENCRYPT_SUCCESS);}

        } catch (Exception e) {
            e.printStackTrace();
        }


        return path;
    }


    @SuppressWarnings("ResourceType")
    public static String imageEncrypt(EncryptConfig encryptConfig){
        V2TIMMessage message = encryptConfig.getMessage();
        V2TIMImageElem imageElement = message.getImageElem();
        String path = imageElement.getPath();

        String originPath = EncryptManager.getInstance().getFileOriginPath(path);
        if (originPath!=null){
            path = originPath;
        }

        ToastUtil.toastLongMessage(ENCRYPT_LOADING);
        String newPath = path;
        Resources resources = TUIChatService.getAppContext().getResources();
        InputStream inputStream = resources.openRawResource(R.drawable.encrypt_image);
        byte[] baseImage = new byte[BASE_IMAGE_LENGTH];
        byte[] fileBuffer;
        byte[] obj;
        byte[] buffer;

        try {
            inputStream.read(baseImage);inputStream.close();
            File file = new File(path);
            fileBuffer = FileReaderUtils.readByNIO(file);
            EncryptData encryptData = new EncryptData();
            encryptData.setEncryptData(fileBuffer);
            encryptData.setOriginPath(path);

            try {
                Boolean flag = encryptData.Encrypt(encryptConfig);
            } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                e.printStackTrace();
                ToastUtil.toastLongMessage(ENCRYPT_FAIL);
                return path;
            }

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(encryptData);
            obj = bo.toByteArray();
            bo.close();oo.close();

            buffer = new byte[BASE_IMAGE_LENGTH+obj.length];

            for (int i = 0; i < BASE_IMAGE_LENGTH; i++) {
                buffer[i] = baseImage[i];
            }
            for (int i = 0; i < obj.length; i++) {
                buffer[i+BASE_IMAGE_LENGTH] = obj[i];
            }

            String filename = file.getName();
            String suffix = filename.substring(filename.lastIndexOf('.'));
            newPath = file.getParentFile() + File.separator + encryptData.getDataMD5() + suffix;
            file = new File(newPath);
            if (file.exists()){ToastUtil.toastShortMessage(ENCRYPT_SUCCESS);return newPath;}
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(buffer);fileOutputStream.close();

            ToastUtil.toastShortMessage(ENCRYPT_SUCCESS);


        } catch (IOException e) {
            e.printStackTrace();
            ToastUtil.toastShortMessage(ENCRYPT_FAIL);

        }

        return newPath;


    }

    public static void imageDeEncrypt(String path){

        byte[] fileBuffer;
        byte[] obj;
        byte[] buffer;
        ToastUtil.toastShortMessage(DE_ENCRYPT_LOADING);


        try {
            File file = new File(path);
            buffer = FileReaderUtils.readByNIO(file);
            obj = new byte[buffer.length-BASE_IMAGE_LENGTH];
            for (int i = 0; i < obj.length; i++) {
                obj[i] = buffer[i+BASE_IMAGE_LENGTH];
            }
            ByteArrayInputStream bi = new ByteArrayInputStream(obj);
            ObjectInputStream oo = new ObjectInputStream(bi);
            EncryptData encryptData = (EncryptData) oo.readObject();oo.close();bi.close();

            Boolean flag = null;
            try {
                flag = encryptData.deEnCrypt(new EncryptConfig(null));
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtil.toastLongMessage(DE_ENCRYPT_FAIL);
                return;

            }
            fileBuffer = encryptData.getEncryptData();

            if (flag==true){ToastUtil.toastShortMessage(DE_ENCRYPT_SUCCESS);
            }
            else {ToastUtil.toastShortMessage(DE_ENCRYPT_FAIL);}

            file = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(fileBuffer);fileOutputStream.close();

            //fileTest(path);
            //ToastUtil.toastShortMessage(DE_ENCRYPT_SUCCESS);

        }
        catch (IOException e) {
            ToastUtil.toastShortMessage(DE_ENCRYPT_FAIL);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            ToastUtil.toastShortMessage(DE_ENCRYPT_FAIL);
            e.printStackTrace();
        }
        return ;
    }

    public static String imageDeEncryptToGetPath(String path){
        byte[] obj;
        byte[] buffer;
        //ToastUtil.toastShortMessage(DE_ENCRYPT_LOADING);
        String originPath = path;

        try {
            File file = new File(path);
            buffer = FileReaderUtils.readByNIO(file);
            obj = new byte[buffer.length-BASE_IMAGE_LENGTH];
            for (int i = 0; i < obj.length; i++) {
                obj[i] = buffer[i+BASE_IMAGE_LENGTH];
            }
            ByteArrayInputStream bi = new ByteArrayInputStream(obj);
            ObjectInputStream oo = new ObjectInputStream(bi);
            EncryptData encryptData = (EncryptData) oo.readObject();oo.close();bi.close();

            //Boolean flag = encryptData.deEnCrypt();
            originPath = encryptData.getOriginPath();

        }
        catch (IOException e) {
            //ToastUtil.toastShortMessage(DE_ENCRYPT_FAIL);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            //ToastUtil.toastShortMessage(DE_ENCRYPT_FAIL);
            e.printStackTrace();
        }
        return originPath;
    }

    public static String getFileMD5Path(String path){

        File file = new File(path);
        String dataMD5 = EncryptFileData.getMD5(file);
        String filename = file.getName();
        String suffix = filename.substring(filename.lastIndexOf('.'));
        String newPath = file.getParentFile() + File.separator + dataMD5 + suffix;

        EncryptManager.getInstance().setFileMD5HitOriginPath(newPath,path);

        return newPath;


    }

    public static String bytesToString(byte[] arg, int length) {
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }


    public static byte[] ObjectToByteArray(Object obj){
        byte[] bytes = null;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            bytes = bo.toByteArray();
            bo.close();oo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
    public static Object ByteArrayToObject(byte[] bytes){
        Object obj = null;
        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oo = new ObjectInputStream(bi);
            obj = oo.readObject();
            oo.close();bi.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        return obj;
    }



    private static void fileTest(String path){
        Boolean flag = FileUtil.saveImageToGallery(TUIChatService.getAppContext(), path);
        System.out.println(flag);
    }

    private static void progressDialogDismiss(ProgressDialog progressDialog,String msg){
        progressDialog.dismiss();
        Toast.makeText(TUIChatService.getAppContext(),msg,Toast.LENGTH_SHORT).show();
    }

    private static void progressDialogDismiss(ProgressDialog progressDialog){
        progressDialog.dismiss();
    }

    /**
     * @param zipPathDir  压缩包路径 ，如 /home/data/zip-folder/
     * @param zipFileName 压缩包名称 ，如 测试文件.zip
     * @param fileList    要压缩的文件列表（绝对路径），如 /home/person/test/测试.doc，/home/person/haha/测试.doc
     * @return
     */
    public static void compressFiles(String zipPathDir, String zipFileName, List<String> fileList) {

        File f = new File(zipPathDir + zipFileName);
        if (f.exists()){f.delete();}

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(zipPathDir + zipFileName)))) {
            File zipFile = new File(zipPathDir);
            if (!zipFile.exists()) {
                zipFile.mkdirs();
            }
            for (String filePath : fileList) {
                File file = new File(filePath);

                if (file.exists()) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);
                    byte[] buffer = new byte[2048];
                    compressSingleFile(file, zos, buffer);
                    file.delete();
                }
            }
            zos.flush();
        } catch (Exception e) {
            System.out.println("压缩所有文件成zip包出错" + e);
        }
    }

    //压缩单个文件
    public static void compressSingleFile(File file, ZipOutputStream zos, byte[] buffer) {
        int len;
        try (FileInputStream fis = new FileInputStream(file)) {
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
                zos.flush();
            }
            zos.closeEntry();
        } catch (IOException e) {
            System.out.println("====压缩单个文件异常====" + e);
        }
    }

    /**
     * 解压文件的方法
     * @param srcFile 要解压的文件
     * @return
     * @throws Exception
     */
    public static int unzip(String srcFile) throws Exception{
        File file = new File(srcFile);
        String parentPath = file.getParentFile()+File.separator;
        int flag=0;
        if(!srcFile.isEmpty()){
            ZipFile zipFile=new ZipFile(srcFile);//获取解压的文件
            Enumeration<ZipEntry> entrys = (Enumeration<ZipEntry>) zipFile.entries();
            while (entrys.hasMoreElements()){
                ZipEntry zipEntry = entrys.nextElement();
                System.out.println("文件解压中…… " + zipEntry.getName());
                BufferedInputStream bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(parentPath+zipEntry.getName()));
                int len = -1;
                byte[] bytes = new byte[2048];
                while ((len = bufferedInputStream.read(bytes)) != -1){
                    bufferedOutputStream.write(bytes,0,len);
                }
                bufferedInputStream.close();bufferedOutputStream.close();
                flag++;
            }
            zipFile.close();
        }
        return flag;
    }










}
