package com.tencent.qcloud.tim.demo.dbUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Process;

import androidx.annotation.RequiresApi;

import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMUserFullInfo;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.qcloud.tim.demo.R;
import com.tencent.qcloud.tim.demo.bean.UserInfo;
import com.tencent.qcloud.tim.demo.main.MainActivity;
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.tencent.qcloud.tuikit.tuichat.Encrypt.RSAUtils;
import com.tencent.qcloud.tuikit.tuichat.Encrypt.RSAKeyManager;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
@RequiresApi(api = Build.VERSION_CODES.O)
public class MyDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "nana.db";
    private static final String TABLE_NAME = "user_info";
    private static final int DB_VERSION = 1;
    private static MyDBHelper myDBHelper = null;
    private SQLiteDatabase mRDB = null;
    private SQLiteDatabase mWDB = null;
    private Context context;

    private MyDBHelper(Context context){
        super(context,DB_NAME,null,DB_VERSION);
        this.context = context;
    }

    public static MyDBHelper getInstance(Context context){
        if (myDBHelper==null){
            myDBHelper = new MyDBHelper(context);
        }
        return myDBHelper;
    }

    public SQLiteDatabase openReadLink(){
        if (mRDB == null || !mRDB.isOpen()){
            mRDB = myDBHelper.getReadableDatabase();
        }
        return mRDB;
    }
    public SQLiteDatabase openWriteLink(){
        if (mWDB == null || !mWDB.isOpen()){
            mWDB = myDBHelper.getWritableDatabase();
        }
        return mWDB;
    }
    public void closeLink(){
        if (mRDB != null && mRDB.isOpen()){
            mRDB.close();
            mRDB = null;
        }
        if (mWDB != null && mWDB.isOpen()){
            mWDB.close();
            mWDB = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "CREATE TABLE IF NOT EXISTS "+TABLE_NAME+" ("+
                "user VARCHAR NOT NULL,"+
                "publicKey VARCHAR(1024) NOT NULL,"+
                "privateKey VARCHAR(1024) NOT NULL);";
        sqLiteDatabase.execSQL(sql);



    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    // 生成用户私钥对象
    public List<String> setPrivateKeyInstance(){
        List<String> keyList = searchKey();
        String pukString = keyList.get(0);
        String prkString = keyList.get(1);
        RSAKeyManager rsaKeyManager = RSAKeyManager.getInstance();
        rsaKeyManager.setMyPrivateKey(prkString);
        rsaKeyManager.setMyPublicKey(pukString);
        return keyList;
    }

    public void showFirstAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Resources resources = context.getResources();
        builder.setTitle(resources.getString(R.string.disclaimers_titles));
        String[] item = new String[7];
        item[0] = resources.getString(R.string.disclaimers_1);
        item[1] = resources.getString(R.string.disclaimers_2);
        item[2] = resources.getString(R.string.disclaimers_3);
        item[3] = resources.getString(R.string.disclaimers_4);
        item[4] = resources.getString(R.string.disclaimers_5);
        item[5] = resources.getString(R.string.disclaimers_6);
        item[6] = resources.getString(R.string.disclaimers_7);
        StringBuffer buffer = new StringBuffer();
        for (String s :
                item) {
            buffer.append(s);
            buffer.append('\n');
        }
        builder.setMessage(buffer.toString());
        builder.setPositiveButton(resources.getString(R.string.disclaimers_accept), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ToastUtil.toastShortMessage(resources.getString(R.string.disclaimers_thank));
                DefaultKey();
                closeLink();
            }
        });
        builder.setNegativeButton(resources.getString(R.string.disclaimers_refuse), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                UserInfo.getInstance().cleanUserInfo();
                Process.killProcess(Process.myPid());
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                deleteKey();
                ToastUtil.toastShortMessage(resources.getString(R.string.disclaimers_key));
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                UserInfo.getInstance().cleanUserInfo();
                Process.killProcess(Process.myPid());
            }
        });
        alertDialog.show();
    }

    public void Default(Context context){
        this.context = context;
        if (searchKey()==null){
            showFirstAlertDialog();
            DefaultKey();
        }
        else{
            setPrivateKeyInstance();
            uploadPublicKey(setPrivateKeyInstance().get(0));
            ToastUtil.toastLongMessage("私钥认证成功");
            closeLink();
        }

    }

    private void DefaultKey(){
        // 为用户添加公私钥(本地生成)
        try {
            List<String> keyStringList = RSAUtils.getRSAKeyString(1024);
            String pukString = keyStringList.get(0);
            String prkString = keyStringList.get(1);
            openWriteLink();
            insertKey(pukString,prkString);
            uploadPublicKey(pukString);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        setPrivateKeyInstance();

    }

    private Long insertKey(String publicKey,String privateKey){
        String user = UserInfo.getInstance().getUserId();
        ContentValues values = new ContentValues();
        values.put("user",user);
        values.put("privateKey",privateKey);
        values.put("publicKey",publicKey);
        return mWDB.insert(TABLE_NAME,null,values);
    }
    private void uploadPublicKey(String publicKey){
        String user = UserInfo.getInstance().getUserId();
        V2TIMManager v2TIMManager = V2TIMManager.getInstance();
        List<String> l = new ArrayList<>();
        l.add(user);
        v2TIMManager.getUsersInfo(l, new V2TIMValueCallback<List<V2TIMUserFullInfo>>() {
            @Override
            public void onSuccess(List<V2TIMUserFullInfo> v2TIMUserFullInfos) {
                V2TIMUserFullInfo myInfo = null;
                for (V2TIMUserFullInfo info :
                        v2TIMUserFullInfos) {
                    HashMap<String,byte[]> customInfo = info.getCustomInfo();
                    customInfo.put("RSAKEY",publicKey.getBytes(Charset.defaultCharset()));
                    info.setCustomInfo(customInfo);
                    myInfo = info;
                }

                v2TIMManager.setSelfInfo(myInfo, new V2TIMCallback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(int i, String s) {
                        ToastUtil.toastLongMessage("公私钥创建失败");
                    }
                });

            }

            @Override
            public void onError(int i, String s) {
                ToastUtil.toastShortMessage("公私钥创建失败");
            }
        });
    }
    public List<String> searchKey(){
        String user = UserInfo.getInstance().getUserId();
        String sql = "select * from "+TABLE_NAME+" where user='"+user+"'";
        Cursor cursor = mRDB.rawQuery(sql,null);
        cursor.moveToFirst();
        if (cursor.moveToPosition(0) != true){
            cursor.close();
            return null;
        }
        List<String> keyList = new ArrayList<>();
        keyList.add(cursor.getString(1));
        keyList.add(cursor.getString(2));
        cursor.close();
        return keyList;
    }
    public void deleteKey(){
        String userID = RSAKeyManager.getInstance().getMyUserID();
        getReadableDatabase().delete(TABLE_NAME,"user=?",new String[]{userID});
    }
}
