package com.mersad.gold18;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class GoldPriceService extends Service {

    private static final String DIGIKALA_API =
            "https://api.digikala.com/wealth/v1/prices/";

    private static final String MILLI_API =
            "https://milli.gold/api/v1/public/milli-price/external";

    private static final String CHANNEL_ID =
            "gold_background";

    private static final String ALERT_CHANNEL_ID =
            "gold_alerts";

    private long lastHistorySave = 0;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private long getInterval() {

        return getSharedPreferences(
                "settings",
                MODE_PRIVATE
        ).getLong(
                "background_interval",
                10000
        );
    }

    private String getSource() {

        return getSharedPreferences(
                "gold_settings",
                MODE_PRIVATE
        ).getString(
                "price_source",
                "digikala"
        );
    }

    private String getSourceName() {

        return "milli".equals(getSource())
                ? "میلی"
                : "دیجی‌کالا";
    }

    private String getApiUrl() {

        return "milli".equals(getSource())
                ? MILLI_API
                : DIGIKALA_API;
    }

    private final Runnable checker =
            new Runnable() {

                @Override
                public void run() {

                    checkPrice();

                    handler.postDelayed(
                            this,
                            getInterval()
                    );
                }
            };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        Intent restartService =
                new Intent(getApplicationContext(), GoldPriceService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartService);
        } else {
            getApplicationContext().startService(restartService);
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();

        createChannels();

        Intent openIntent =
                new Intent(
                        this,
                        MainActivity.class
                );

        openIntent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        1001,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        Notification notification =
                new Notification.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                R.drawable.ic_notification_gold
                        )
                        .setContentTitle("MrGold")
                        .setContentText(
                                "رصد " +
                                getSourceName() +
                                " در پس‌زمینه فعال است"
                        )
                        .setContentIntent(
                                pendingIntent
                        )
                        .setOngoing(true)
                        .build();

        startForeground(
                1001,
                notification
        );

        handler.post(checker);
    }

    private void checkPrice() {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(getApiUrl());

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line =
                        reader.readLine()) != null) {

                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );

                long price =
                        parsePrice(json);

                String source =
                        getSource();

                long now =
                        System.currentTimeMillis();

                if (now - lastHistorySave >= 60_000) {

                    PriceHistory.add(
                            this,
                            price,
                            source
                    );

                    lastHistorySave = now;
                }

                updateBackgroundNotification(
                        price,
                        source
                );

                checkTargets(
                        price,
                        source
                );

            } catch (Exception ignored) {

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private long parsePrice(JSONObject json)
            throws Exception {

        if ("milli".equals(getSource())) {

            return json
                    .getJSONObject("data")
                    .getLong("price18");
        }

        return json
                .getJSONObject("gold18")
                .getLong("price");
    }

    private void updateBackgroundNotification(
            long currentPrice,
            String source) {

        try {

            JSONArray history =
                    PriceHistory.get(
                            this,
                            source
                    );

            long highest = currentPrice;
            long lowest = currentPrice;
            long previous = currentPrice;

            for (int i = 0;
                 i < history.length();
                 i++) {

                JSONObject item =
                        history.getJSONObject(i);

                long value =
                        item.getLong("price");

                if (value > highest) {
                    highest = value;
                }

                if (value < lowest) {
                    lowest = value;
                }

                if (i ==
                        history.length() - 1) {

                    previous = value;
                }
            }

            long change =
                    currentPrice - previous;

            String changeText;

            if (change > 0) {

                changeText =
                        "🟢 ▲ +" +
                        formatPrice(change) +
                        " تومان";

            } else if (change < 0) {

                changeText =
                        "🔴 ▼ -" +
                        formatPrice(
                                Math.abs(change)
                        ) +
                        " تومان";

            } else {

                changeText =
                        "⚪ — بدون تغییر";
            }

            Intent openIntent =
                    new Intent(
                            this,
                            MainActivity.class
                    );

            openIntent.setFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            this,
                            1001,
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE
                    );

            String title =
                    "MrGold • " +
                    sourceName(source) +
                    " • طلای ۱۸ عیار";

            String compactText =
                    "💰 " +
                    formatPrice(currentPrice) +
                    " تومان   " +
                    changeText;

            String details =
                    "🟡 منبع: " +
                    sourceName(source) +
                    "\n\n" +
                    "💰 قیمت لحظه‌ای\n" +
                    formatPrice(currentPrice) +
                    " تومان\n\n" +
                    "📈 بیشترین\n" +
                    formatPrice(highest) +
                    " تومان\n\n" +
                    "📉 کمترین\n" +
                    formatPrice(lowest) +
                    " تومان\n\n" +
                    "📊 تغییر\n" +
                    changeText;

            Notification.Builder builder =
                    new Notification.Builder(
                            this,
                            CHANNEL_ID
                    )
                            .setSmallIcon(
                                    R.drawable.ic_notification_gold
                            )
                            .setContentTitle(title)
                            .setContentText(
                                    compactText
                            )
                            .setStyle(
                                    new Notification.BigTextStyle()
                                            .bigText(details)
                                            .setBigContentTitle(title)
                            )
                            .setContentIntent(
                                    pendingIntent
                            )
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .setShowWhen(false)
                            .setCategory(
                                    Notification.CATEGORY_STATUS
                            )
                            .setVisibility(
                                    Notification.VISIBILITY_PUBLIC
                            )
                            .setColor(
                                    change > 0
                                            ? 0xFF2E7D32
                                            : change < 0
                                            ? 0xFFC62828
                                            : 0xFF757575
                            );

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    Context.NOTIFICATION_SERVICE
                            );

            if (manager != null) {

                manager.notify(
                        1001,
                        builder.build()
                );
            }

        } catch (Exception ignored) {
        }
    }

    private String sourceName(String source) {

        return "milli".equals(source)
                ? "میلی"
                : "دیجی‌کالا";
    }

    private void checkTargets(
            long price,
            String source) {

        SharedPreferences prefs =
                getSharedPreferences(
                        "gold_settings",
                        MODE_PRIVATE
                );

        String suffix =
                "_alerted_" + source;

        checkSingleTarget(
                prefs,
                price,
                "high_target",
                "high_enabled",
                "high_alerted" + suffix,
                true,
                "🔺 هدف بالای طلا"
        );

        checkSingleTarget(
                prefs,
                price,
                "high_target_2",
                "high_enabled_2",
                "high_alerted_2" + suffix,
                true,
                "🔺 هدف بالای طلا ۲"
        );

        checkSingleTarget(
                prefs,
                price,
                "low_target",
                "low_enabled",
                "low_alerted" + suffix,
                false,
                "🔻 هدف پایین طلا"
        );

        checkSingleTarget(
                prefs,
                price,
                "low_target_2",
                "low_enabled_2",
                "low_alerted_2" + suffix,
                false,
                "🔻 هدف پایین طلا ۲"
        );
    }

    private void checkSingleTarget(
            SharedPreferences prefs,
            long price,
            String targetKey,
            String enabledKey,
            String alertedKey,
            boolean isHigh,
            String title) {

        try {

            String targetText =
                    prefs.getString(
                            targetKey,
                            ""
                    );

            boolean enabled =
                    prefs.getBoolean(
                            enabledKey,
                            true
                    );

            boolean alerted =
                    prefs.getBoolean(
                            alertedKey,
                            false
                    );

            if (!enabled ||
                    targetText.isEmpty()) {

                return;
            }

            long target =
                    Long.parseLong(
                            targetText
                    );

            boolean reached =
                    isHigh
                            ? price >= target
                            : price <= target;

            if (reached && !alerted) {

                sendAlert(
                        title,
                        "قیمت به " +
                        formatPrice(price) +
                        " تومان رسید."
                );

                prefs.edit()
                        .putBoolean(
                                alertedKey,
                                true
                        )
                        .apply();

            } else if (!reached) {

                prefs.edit()
                        .putBoolean(
                                alertedKey,
                                false
                        )
                        .apply();
            }

        } catch (Exception ignored) {
        }
    }

    private void createChannels() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            NotificationChannel channel =
                    new NotificationChannel(
                            ALERT_CHANNEL_ID,
                            "هشدار قیمت طلا",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "هشدار رسیدن قیمت طلا به اهداف تعیین‌شده"
            );

            manager.createNotificationChannel(
                    channel
            );

            NotificationChannel backgroundChannel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "رصد قیمت در پس‌زمینه",
                            NotificationManager.IMPORTANCE_LOW
                    );

            backgroundChannel.setDescription(
                    "نمایش قیمت لحظه‌ای طلا در پس‌زمینه"
            );

            manager.createNotificationChannel(
                    backgroundChannel
            );
        }
    }

    private String formatPrice(long price) {

        return String.format(
                Locale.US,
                "%,d",
                price
        );
    }

    private void sendAlert(
            String title,
            String message) {

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Notification notification =
                new Notification.Builder(
                        this,
                        ALERT_CHANNEL_ID
                )
                        .setSmallIcon(
                                R.drawable.ic_notification_gold
                        )
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(
                                Notification.PRIORITY_HIGH
                        )
                        .build();

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            manager.notify(
                    (int)
                            System.currentTimeMillis(),
                    notification
            );
        }
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacks(checker);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }
}
