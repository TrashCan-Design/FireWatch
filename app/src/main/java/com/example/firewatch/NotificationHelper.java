package com.example.firewatch;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class NotificationHelper {
    public static void ensureChannel(Context c, String id, String name){
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm.getNotificationChannel(id) != null) return;
        NotificationChannel ch = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        ch.enableLights(true);
        ch.setLightColor(Color.RED);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    public static void notifyFire(Context c, String channelId){
        notifyFire(c, channelId, null);
    }

    public static void notifyFire(Context c, String channelId, List<String> deviceIds){
        String title = "🔥 FIRE DETECTED";
        String text;

        if(deviceIds != null && !deviceIds.isEmpty()){
            if(deviceIds.size() == 1){
                text = "Fire detected on " + deviceIds.get(0);
            } else {
                text = "Fire detected on " + deviceIds.size() + " devices: " + android.text.TextUtils.join(", ", deviceIds);
            }
        } else {
            text = "Your ESP32 reported FIRE.";
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(c);

        // Check runtime permission on Android 13+
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                c, android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            nm.notify(1001, b.build());
        } else {
            Log.w("NotificationHelper", "POST_NOTIFICATIONS not granted");
        }
    }
}