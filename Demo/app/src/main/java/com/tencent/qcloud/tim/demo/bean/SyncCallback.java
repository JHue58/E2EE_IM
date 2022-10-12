package com.tencent.qcloud.tim.demo.bean;

public class SyncCallback {
    public Boolean flag = null;
    private Boolean finish = false;
    public void waitingCallback(){
        while (true){
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(flag+" "+finish);
            if (flag==null && finish==false){
                continue;
            }
            break;
        }
    }
    public void methodFinish(){
        finish = true;
    }
}
