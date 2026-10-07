package com.mersad.gold18;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MerGoldFirebaseService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "gold_push";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : "MrGold";

        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : "";

        showNotification(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        getSharedPreferences(
                "mrgold_firebase",
                MODE_PRIVATE
        ).edit()
                .putString("fcm_token", token)
                .apply();
    }

    private void showNotification(String title, String body) {

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "اعلان‌های MrGold",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(
                    this,
                    CHANNEL_ID
            );
        } else {
            builder = new Notification.Builder(this);
        }

        builder
                .setSmallIcon(R.drawable.ic_notification_gold)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(body)
                )
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH);

        manager.notify(
                (int) System.currentTimeMillis(),
                builder.build()
        );
    }
}
