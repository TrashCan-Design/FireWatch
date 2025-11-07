package com.example.firewatch;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;

public class FireWatchApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize API Client
        ApiClient.init(this);

        // Create notification channels
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Monitoring channel
        NotificationChannel monitoringChannel = new NotificationChannel(
                "firewatch_monitoring",
                "Background Monitoring",
                NotificationManager.IMPORTANCE_LOW
        );
        monitoringChannel.setDescription("Shows that FireWatch is monitoring in background");
        monitoringChannel.enableLights(false);
        monitoringChannel.enableVibration(false);
        nm.createNotificationChannel(monitoringChannel);

        // Alert channel
        NotificationChannel alertChannel = new NotificationChannel(
                "firewatch_alerts",
                "Fire Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.setDescription("Critical fire detection alerts");
        alertChannel.enableLights(true);
        alertChannel.setLightColor(Color.RED);
        alertChannel.enableVibration(true);
        alertChannel.setVibrationPattern(new long[]{0, 400, 200, 400, 200, 400});
        nm.createNotificationChannel(alertChannel);
    }
}