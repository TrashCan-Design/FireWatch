package com.example.firewatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatusPollerService extends Service {

    public static boolean isRunning = false;
    private static final String CHANNEL_ID = "firewatch_monitoring";
    private static final String ALERT_CHANNEL_ID = "firewatch_alerts";
    private static final int FOREGROUND_NOTIFICATION_ID = 999;
    private static final int FIRE_NOTIFICATION_ID = 1001;

    private final Handler handler = new Handler();
    private Runnable pollRunnable;
    private Set<String> lastFireDevices = new HashSet<>();
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        prefs = getSharedPreferences("firewatch_prefs", MODE_PRIVATE);

        createNotificationChannels();
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if monitoring is enabled
                if (prefs.getBoolean("monitoring_enabled", true)) {
                    pollOnce();
                }
                handler.postDelayed(this, 5000); // Poll every 5 seconds
            }
        };
        handler.post(pollRunnable);
    }

    private void createNotificationChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Monitoring channel (low priority)
        NotificationChannel monitoringChannel = new NotificationChannel(
                CHANNEL_ID,
                "Background Monitoring",
                NotificationManager.IMPORTANCE_LOW
        );
        monitoringChannel.setDescription("Shows that FireWatch is monitoring");
        monitoringChannel.enableLights(false);
        monitoringChannel.enableVibration(false);
        nm.createNotificationChannel(monitoringChannel);

        // Alert channel (high priority)
        NotificationChannel alertChannel = new NotificationChannel(
                ALERT_CHANNEL_ID,
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

    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FireWatch Monitoring")
                .setContentText("Watching for fire alerts...")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void pollOnce() {
        SupabaseApi api = ApiClient.api(this);

        api.getStatus().enqueue(new Callback<List<ApiModels.StatusRow>>() {
            @Override
            public void onResponse(Call<List<ApiModels.StatusRow>> call, Response<List<ApiModels.StatusRow>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                Set<String> currentFireDevices = new HashSet<>();
                List<String> fireLocationsList = new ArrayList<>();

                for (ApiModels.StatusRow row : response.body()) {
                    if ("fire".equalsIgnoreCase(row.last_status)) {
                        currentFireDevices.add(row.esp32_id);
                        fireLocationsList.add(row.getDisplayName());
                    }
                }

                // Send alert if there are new fire devices or ongoing fire
                if (!currentFireDevices.isEmpty()) {
                    // Check if this is a new fire or continuing fire
                    boolean isNewFire = !currentFireDevices.equals(lastFireDevices);

                    if (isNewFire || !lastFireDevices.isEmpty()) {
                        sendPersistentFireAlert(fireLocationsList, currentFireDevices.size());
                        // Broadcast to MainActivity to update UI
                        sendFireAlertBroadcast();
                    }
                } else {
                    // Clear fire notification if all clear
                    if (!lastFireDevices.isEmpty()) {
                        clearFireNotification();
                        // Broadcast to MainActivity to update UI
                        sendFireAlertBroadcast();
                    }
                }

                lastFireDevices = currentFireDevices;
            }

            @Override
            public void onFailure(Call<List<ApiModels.StatusRow>> call, Throwable t) {
                // Connection failed - keep trying
            }
        });
    }

    private void sendFireAlertBroadcast() {
        Intent intent = new Intent("com.example.firewatch.FIRE_ALERT");
        sendBroadcast(intent);
    }

    private void sendPersistentFireAlert(List<String> locations, int count) {
        // Check if notifications are allowed
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String title = "🔥 FIRE DETECTED!";
        String text;

        if (count == 1 && !locations.isEmpty()) {
            text = "Fire alert at " + locations.get(0);
        } else {
            text = "Fire detected at " + count + " location(s)";
        }

        // Create intent to open app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Create emergency call action
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(android.net.Uri.parse("tel:101"));
        PendingIntent callPendingIntent = PendingIntent.getActivity(
                this, 1, callIntent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text + "\n\nTap to view details or call emergency services."))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true) // Make it persistent
                .setAutoCancel(false) // Don't dismiss on tap
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_call, "Call 101", callPendingIntent)
                .setColor(Color.RED)
                .setColorized(true)
                .setVibrate(new long[]{0, 400, 200, 400})
                .setLights(Color.RED, 1000, 1000);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(FIRE_NOTIFICATION_ID, builder.build());
    }

    private void clearFireNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(FIRE_NOTIFICATION_ID);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        handler.removeCallbacks(pollRunnable);
        clearFireNotification();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}