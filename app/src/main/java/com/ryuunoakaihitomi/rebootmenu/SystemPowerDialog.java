package com.ryuunoakaihitomi.rebootmenu;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

import com.ryuunoakaihitomi.rebootmenu.util.DebugLog;
import com.ryuunoakaihitomi.rebootmenu.util.TextToast;

/**
 * 辅助服务
 * Created by ZQY on 2018/2/12.
 */

public class SystemPowerDialog extends AccessibilityService {

    public static final String POWER_DIALOG_ACTION = "SPD.POWER_DIALOG_ACTION";
    private boolean isBroadcastRegistered;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            new DebugLog("onReceive: will perform AccessibilityService.GLOBAL_ACTION_POWER_DIALOG");
            //调用系统电源菜单核心代码
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
            new TextToast(getApplicationContext(), getString(R.string.spd_showed));
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        new DebugLog("SystemPowerDialog.onServiceConnected", DebugLog.LogLevel.V);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            loadNoticeBar();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(POWER_DIALOG_ACTION);
            registerReceiver(broadcastReceiver, intentFilter);
            isBroadcastRegistered = true;
        } else {
            new DebugLog("onServiceConnected: Build.VERSION_CODES.LOLLIPOP?", DebugLog.LogLevel.E);
            System.exit(-1);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopForeground(true);
        new DebugLog("SystemPowerDialog.onUnbind", DebugLog.LogLevel.V);
        if (isBroadcastRegistered) {
            isBroadcastRegistered = false;
            unregisterReceiver(broadcastReceiver);
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        String className = accessibilityEvent.getClassName().toString();
        new DebugLog("SystemPowerDialog.onAccessibilityEvent className:" + className, DebugLog.LogLevel.V);
        //使用root模式就没有必要保留辅助服务
        if (RootMode.class.getName().equals(className) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            disableSelf();
    }

    @Override
    public void onInterrupt() {
    }

    //目前大部分环境中都无效的保活方式
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    //常驻通知
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void loadNoticeBar() {
        Notification.Builder builder;
        final String CHANNEL_ID = "SPD";
        final int NOTIFICATION_ID = 1;
        //Oreo以上适配通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //尽管IMPORTANCE_MIN在26中无效...
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.service_name), NotificationManager.IMPORTANCE_MIN);
            //noinspection ConstantConditions
            getSystemService(NotificationManager.class).createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(this, CHANNEL_ID);
            //根据文档：如果非要用一个来代替的话，使用免root的锁屏
            builder.setShortcutId("ur_l");
        } else
            builder = new Notification.Builder(this);
        builder
                .setContentTitle(getString(R.string.service_simple_name))
                .setContentText(getString(R.string.notification_notice))
                .setContentIntent(PendingIntent.getActivity(this, 0, getPackageManager().getLaunchIntentForPackage(getPackageName()), 0))
                .setOngoing(true)
                //尽管在26上不能折叠通知（需要手动设置），但可以将其放置在较低的位置（已废弃）
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.mipmap.ic_launcher)
                //没有必要显示在锁屏上
                .setVisibility(Notification.VISIBILITY_SECRET)
                //秒表指示
                .setUsesChronometer(true);
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }
}
