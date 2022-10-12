package com.tencent.qcloud.tuikit.tuichat;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity;
import com.tencent.qcloud.tuikit.tuichat.bean.ChatInfo;
import com.tencent.qcloud.tuikit.tuichat.bean.message.TUIMessageBean;
import com.tencent.qcloud.tuikit.tuichat.config.TUIChatConfigs;
import com.tencent.qcloud.tuikit.tuichat.ui.page.TUIBaseChatActivity;

import java.util.List;

public class NewMessageNotification {

    /**
     * 判断程序是否在后台
     * @param context
     * @return
     */
    public static boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }




    protected void sendNewNotification(V2TIMMessage msg, TUIMessageBean messageBean, Context context){
        if (!isBackground(context)){
            return;
        }
        NotificationChannel channel = new NotificationChannel("msg","新消息",NotificationManager.IMPORTANCE_HIGH);;

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        channel.enableLights(true);
        channel.setShowBadge(true);
        NotificationManager notificationManager = (NotificationManager)TUIChatService.getInstance().getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder=new Notification.Builder(context);

        PendingIntent pendingIntent= PendingIntent.getActivity(context,0,intent,0);

        String title = "";
        String name = "";
        if (msg.getNickName().equals("")){
            name = msg.getSender();
        }
        else {
            name = msg.getNickName();
        }
        if (!messageBean.isGroup()){
            title = "来自"+name+"的消息";
        }
        else{
            if (msg.getNameCard().equals("")){
                title = name+"发送的消息,来自群"+msg.getGroupID();
            }
            else {
                title = msg.getNameCard()+"发送的消息,来自群"+msg.getGroupID();
            }
        }

        Notification notification=builder.setSmallIcon(R.drawable.ic_launcher)//应用图标
                .setContentTitle(title)
                .setWhen(System.currentTimeMillis())
                .setContentText(messageBean.onGetDisplayString())
                .setAutoCancel(true)//点击后是否删除
                .setChannelId(channel.getId())
                //Intent 意向意图 启动Activity,Service
                //PendingIntent 对Intent的封装，将来要干什么，延迟的意向
                .setContentIntent(pendingIntent)//点击通知将要做什么
                .build();//创建一个通知



        notificationManager.notify(1,notification);




    }
}
