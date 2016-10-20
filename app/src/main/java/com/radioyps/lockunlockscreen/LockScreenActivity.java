package com.radioyps.lockunlockscreen;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class LockScreenActivity extends AppCompatActivity implements View.OnClickListener {


    private Button lock;
    private Button disable;
    private Button enable;
    static final int RESULT_ENABLE = 1;

    private DevicePolicyManager deviceManger;
    private ActivityManager activityManager;
    private ComponentName compName;
    private  boolean mStopThreadUpdate = false;
    private  int mCount = 0;
    private  PowerManager.WakeLock wakeLock = null;
    private final static String TAG = LockScreenActivity.class.getName();
    public static Handler mHandler = null;
    private TextView curenntConnected = null;
    private TextView infoReceived = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        deviceManger = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        activityManager = (ActivityManager) getSystemService(
                Context.ACTIVITY_SERVICE);
        compName = new ComponentName(this, MyAdmin.class);

        infoReceived = (TextView) findViewById(R.id.msg_received);
        curenntConnected = (TextView) findViewById(R.id.connected_ip_info);

        lock = (Button) findViewById(R.id.lock);
        lock.setOnClickListener(this);

        disable = (Button) findViewById(R.id.btnDisable);
        enable = (Button) findViewById(R.id.btnEnable);
        disable.setOnClickListener(this);
        enable.setOnClickListener(this);

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                String msgRecevied = null;
                    switch (msg.what){
                        case CommonConstants.MSG_WEAKUP_DEVICE:
                            unLockScreen();
                            Log.i(TAG, "handleMessage()>>  MSG_WEAKUP_DEVICE");
                            break;
                        case CommonConstants.MSG_UPDATE_INFO_RECEVIED:
                            msgRecevied = (String)msg.obj;
                            infoReceived.setText(msgRecevied);
                            unLockScreen();
                            Log.i(TAG, "handleMessage()>>  MSG_UPDATE_INFO_RECEVIED");
                            break;
                        case CommonConstants.MSG_UPDATE_CURRENT_CONNECTED_IP:
                            msgRecevied = (String)msg.obj;
                            curenntConnected.setText(msgRecevied);
                            unLockScreen();
                            Log.i(TAG, "handleMessage()>>  MSG_UPDATE_CURRENT_CONNECTED_IP");
                            break;
                    }

                }
           };

        Intent intent = new Intent(this, SocketConnectServer.class);
        startService(intent);

    }

    @Override
    public void onClick(View v) {

        if (v == lock) {
            boolean active = deviceManger.isAdminActive(compName);
            if (active) {
                deviceManger.lockNow();
                releaseWakeLock();
                startUpdateMessageThread();
            }
        }
         /**/
        if (v == enable) {
            Intent intent = new Intent(DevicePolicyManager
                    .ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Additional text explaining why this needs to be added.");
            startActivityForResult(intent, RESULT_ENABLE);
        }

        if (v == disable) {
            deviceManger.removeActiveAdmin(compName);
            updateButtonStates();
        }
    }

    private void updateButtonStates() {

        boolean active = deviceManger.isAdminActive(compName);
        if (active) {
            enable.setEnabled(false);
            disable.setEnabled(true);

        } else {
            enable.setEnabled(true);
            disable.setEnabled(false);
        }
    }
private  void releaseWakeLock(){
    try{
        wakeLock.release();
    }catch (Exception e){
        Log.e(TAG, "trying to release unlocked wakelock");
    }

}

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    Log.i(TAG, "Admin enabled!");
                } else {
                    Log.i(TAG, "Admin enable FAILED!");
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }




    private void startUpdateMessageThread(){
        mStopThreadUpdate = false;
        mCount = 0;
        Thread initThread = new Thread(new updateMessageThread());
        initThread.start();
    }

    private void unLockScreen(){
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock kl = km .newKeyguardLock("MyKeyguardLock");
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);


        kl.disableKeyguard();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
        Log.i(TAG, "unLockSceen()>> acquire the wakelock");
        wakeLock.acquire();
    }

    private class updateMessageThread extends Thread {
        public void run() {

            while (!mStopThreadUpdate) {
                mCount += 1;
                Log.i(TAG, "updateMessageThread> run() mCount " + mCount);
                if(mCount == 10){
                    Message.obtain(mHandler, CommonConstants.MSG_WEAKUP_DEVICE,"try wake up device" ).sendToTarget();
                    mStopThreadUpdate= true;
                    mCount = 0;
                }
                try {
                    updateMessageThread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }
    }
}

