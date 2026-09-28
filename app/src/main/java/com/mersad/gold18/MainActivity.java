package com.mersad.gold18;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView priceText;
    private TextView lastUpdateText;
    private TextView statusText;
    private TextView valueText;
    private TextView priceChangeText;
    private TextView chartStatsText;

    private PriceChartView chartView;

    private EditText highInput;
    private EditText highInput2;
    private EditText lowInput;
    private EditText lowInput2;
    private EditText amountInput;

    private Switch highSwitch;
    private Switch highSwitch2;
    private Switch lowSwitch;
    private Switch lowSwitch2;
    private Switch darkSwitch;

    private Spinner refreshSpinner;

    private RadioButton digikalaRadio;
    private RadioButton milliRadio;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private SharedPreferences prefs;

    private static final String DIGIKALA_API =
            "https://api.digikala.com/wealth/v1/prices/";

    private static final String MILLI_API =
            "https://milli.gold/api/v1/public/milli-price/external";

    private static final String CHANNEL_ID =
            "gold_alerts";

    private long currentPrice = 0;
    private long previousPrice = 0;

    private long chartRangeMillis =
            24L * 60L * 60L * 1000L;

    private final Runnable updater =
            new Runnable() {

                @Override
                public void run() {

                    getGoldPrice();

                    handler.postDelayed(
                            this,
                            getRefreshTime()
                    );
                }
            };

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        prefs =
                getSharedPreferences(
                        "gold_settings",
                        MODE_PRIVATE
                );

        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    100
            );
        }

        buildUI();

        getGoldPrice();

        handler.postDelayed(
                updater,
                getRefreshTime()
        );

        startPriceService();
    }

    private void buildUI() {

        boolean dark =
                prefs.getBoolean(
                        "dark_mode",
                        false
                );

        setContentView(
                R.layout.activity_main
        );

        LinearLayout root =
                findViewById(
                        R.id.mainRoot
                );

        priceText =
                findViewById(
                        R.id.priceText
                );

        priceChangeText =
                findViewById(
                        R.id.priceChangeText
                );

        lastUpdateText =
                findViewById(
                        R.id.lastUpdateText
                );

        chartStatsText =
                findViewById(
                        R.id.chartStatsText
                );

        chartView =
                findViewById(
                        R.id.chartView
                );

        refreshSpinner =
                findViewById(
                        R.id.refreshSpinner
                );

        amountInput =
                findViewById(
                        R.id.amountInput
                );

        valueText =
                findViewById(
                        R.id.valueText
                );

        highInput =
                findViewById(
                        R.id.highInput
                );

        highSwitch =
                findViewById(
                        R.id.highSwitch
                );

        highInput2 =
                findViewById(
                        R.id.highInput2
                );

        highSwitch2 =
                findViewById(
                        R.id.highSwitch2
                );

        lowInput =
                findViewById(
                        R.id.lowInput
                );

        lowSwitch =
                findViewById(
                        R.id.lowSwitch
                );

        lowInput2 =
                findViewById(
                        R.id.lowInput2
                );

        lowSwitch2 =
                findViewById(
                        R.id.lowSwitch2
                );

        darkSwitch =
                findViewById(
                        R.id.darkSwitch
                );

        statusText =
                findViewById(
                        R.id.statusText
                );

        digikalaRadio =
                findViewById(
                        R.id.digikalaRadio
                );

        milliRadio =
                findViewById(
                        R.id.milliRadio
                );

        Spinner backgroundSpinner =
                findViewById(
                        R.id.backgroundSpinner
                );

        TextView backgroundStatus =
                findViewById(
                        R.id.backgroundStatus
                );

        Button dayButton =
                findViewById(
                        R.id.dayButton
                );

        Button weekButton =
                findViewById(
                        R.id.weekButton
                );

        Button monthButton =
                findViewById(
                        R.id.monthButton
                );

        Button saveButton =
                findViewById(
                        R.id.saveButton
                );

        String source =
                getSource();

        digikalaRadio.setChecked(
                "digikala".equals(source)
        );

        milliRadio.setChecked(
                "milli".equals(source)
        );

        digikalaRadio.setOnClickListener(
                v -> changeSource("digikala")
        );

        milliRadio.setOnClickListener(
                v -> changeSource("milli")
        );

        String[] times = {
                "۱ ثانیه",
                "۵ ثانیه",
                "۱۰ ثانیه",
                "۳۰ ثانیه"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        times
                );

        refreshSpinner.setAdapter(
                adapter
        );

        refreshSpinner.setSelection(
                prefs.getInt(
                        "refresh_index",
                        0
                )
        );

        refreshSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        prefs.edit()
                                .putInt(
                                        "refresh_index",
                                        position
                                )
                                .apply();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        String[] backgroundTimes = {
                "۱ ثانیه",
                "۱۰ ثانیه",
                "۲۰ ثانیه"
        };

        ArrayAdapter<String> backgroundAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        backgroundTimes
                );

        backgroundSpinner.setAdapter(
                backgroundAdapter
        );

        int savedBackground =
                prefs.getInt(
                        "background_interval_index",
                        1
                );

        backgroundSpinner.setSelection(
                savedBackground
        );

        backgroundStatus.setText(
                "🟢 رصد " +
                getSourceName() +
                " در پس‌زمینه فعال — هر " +
                backgroundTimes[
                        savedBackground
                ]
        );

        backgroundSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        long[] intervals = {
                                1000L,
                                10000L,
                                20000L
                        };

                        backgroundStatus.setText(
                                "🟢 رصد " +
                                getSourceName() +
                                " در پس‌زمینه فعال — هر " +
                                backgroundTimes[position]
                        );

                        prefs.edit()
                                .putInt(
                                        "background_interval_index",
                                        position
                                )
                                .apply();

                        getSharedPreferences(
                                "settings",
                                MODE_PRIVATE
                        )
                                .edit()
                                .putLong(
                                        "background_interval",
                                        intervals[position]
                                )
                                .apply();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        amountInput.setText(
                prefs.getString(
                        "gold_amount",
                        ""
                )
        );

        highInput.setText(
                prefs.getString(
                        "high_target",
                        ""
                )
        );

        highSwitch.setChecked(
                prefs.getBoolean(
                        "high_enabled",
                        true
                )
        );

        highInput2.setText(
                prefs.getString(
                        "high_target_2",
                        ""
                )
        );

        highSwitch2.setChecked(
                prefs.getBoolean(
                        "high_enabled_2",
                        true
                )
        );

        lowInput.setText(
                prefs.getString(
                        "low_target",
                        ""
                )
        );

        lowSwitch.setChecked(
                prefs.getBoolean(
                        "low_enabled",
                        true
                )
        );

        lowInput2.setText(
                prefs.getString(
                        "low_target_2",
                        ""
                )
        );

        lowSwitch2.setChecked(
                prefs.getBoolean(
                        "low_enabled_2",
                        true
                )
        );

        darkSwitch.setChecked(
                dark
        );

        saveButton.setOnClickListener(
                v -> saveSettings()
        );

        darkSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    prefs.edit()
                            .putBoolean(
                                    "dark_mode",
                                    isChecked
                            )
                            .apply();
                }
        );

        chartRangeMillis =
                24L * 60L * 60L * 1000L;

        dayButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            24L * 60L * 60L * 1000L;

                    updateChart();
                }
        );

        weekButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            7L * 24L * 60L * 60L * 1000L;

                    updateChart();
                }
        );

        monthButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            30L * 24L * 60L * 60L * 1000L;

                    updateChart();
                }
        );

        if (root != null) {

            root.setLayoutDirection(
                    View.LAYOUT_DIRECTION_RTL
            );

            root.setTextDirection(
                    View.TEXT_DIRECTION_RTL
            );

            applyColors(
                    root,
                    dark
            );
        }
    }

    private String getSource() {

        return prefs.getString(
                "price_source",
                "digikala"
        );
    }

    private String getSourceName() {

        return "milli".equals(getSource())
                ? "میلی"
                : "دیجی‌کالا";
    }

    private void changeSource(
            String source) {

        String oldSource =
                getSource();

        if (source.equals(oldSource)) {
            return;
        }

        prefs.edit()
                .putString(
                        "price_source",
                        source
                )
                .apply();

        currentPrice = 0;
        previousPrice = 0;

        priceText.setText(
                "در حال دریافت قیمت..."
        );

        priceChangeText.setText(
                "⚪ — بدون تغییر"
        );

        statusText.setText(
                "منبع قیمت تغییر کرد: " +
                getSourceName()
        );

        updateChart();

        getGoldPrice();

        restartPriceService();

        Toast.makeText(
                this,
                "منبع قیمت: " +
                getSourceName(),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void startPriceService() {

        Intent serviceIntent =
                new Intent(
                        this,
                        GoldPriceService.class
                );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(
                    serviceIntent
            );

        } else {

            startService(
                    serviceIntent
            );
        }
    }

    private void restartPriceService() {

        Intent serviceIntent =
                new Intent(
                        this,
                        GoldPriceService.class
                );

        stopService(
                serviceIntent
        );

        startPriceService();
    }

    private void saveSettings() {

        prefs.edit()
                .putString(
                        "high_target",
                        highInput.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "low_target",
                        lowInput.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "high_target_2",
                        highInput2.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "low_target_2",
                        lowInput2.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "gold_amount",
                        amountInput.getText()
                                .toString()
                                .trim()
                )
                .putBoolean(
                        "high_enabled",
                        highSwitch.isChecked()
                )
                .putBoolean(
                        "low_enabled",
                        lowSwitch.isChecked()
                )
                .putBoolean(
                        "high_enabled_2",
                        highSwitch2.isChecked()
                )
                .putBoolean(
                        "low_enabled_2",
                        lowSwitch2.isChecked()
                )
                .apply();

        statusText.setText(
                "تنظیمات ذخیره شد ✓"
        );

        calculateGoldValue();

        Toast.makeText(
                this,
                "تنظیمات ذخیره شد",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void calculateGoldValue() {

        try {

            String amount =
                    amountInput.getText()
                            .toString()
                            .trim();

            if (amount.isEmpty() ||
                    currentPrice <= 0) {

                valueText.setText(
                        "ارزش طلا: ---"
                );

                return;
            }

            double sot =
                    Double.parseDouble(
                            amount
                    );

            double grams =
                    sot / 1000.0;

            double value =
                    grams * currentPrice;

            valueText.setText(
                    "ارزش طلا: " +
                    String.format(
                            Locale.US,
                            "%,.0f تومان",
                            value
                    )
            );

        } catch (Exception e) {

            valueText.setText(
                    "ارزش طلا: ---"
            );
        }
    }

    private long getRefreshTime() {

        int index =
                prefs.getInt(
                        "refresh_index",
                        0
                );

        switch (index) {

            case 1:
                return 5000L;

            case 2:
                return 10000L;

            case 3:
                return 30000L;

            default:
                return 1000L;
        }
    }

    private String getApiUrl() {

        return "milli".equals(getSource())
                ? MILLI_API
                : DIGIKALA_API;
    }

    private long parsePrice(
            JSONObject json)
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

    private String parseSourceDate(
            JSONObject json) {

        try {

            if ("milli".equals(getSource())) {

                String date =
                        json.getJSONObject("data")
                                .optString(
                                        "date",
                                        ""
                                );

                if (!date.isEmpty()) {
                    return date.replace("T", " ");
                }
            }

        } catch (Exception ignored) {
        }

        return new java.text.SimpleDateFormat(
                "HH:mm:ss",
                Locale.US
        ).format(
                new java.util.Date()
        );
    }

    private void getGoldPrice() {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(
                                getApiUrl()
                        );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "GET"
                );

                connection.setConnectTimeout(
                        5000
                );

                connection.setReadTimeout(
                        5000
                );

                connection.setUseCaches(
                        false
                );

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

                long oldPrice =
                        currentPrice;

                previousPrice =
                        oldPrice;

                currentPrice =
                        price;

                PriceHistory.add(
                        this,
                        price,
                        source
                );

                String sourceDate =
                        parseSourceDate(json);

                runOnUiThread(() -> {

                    priceText.setText(
                            String.format(
                                    Locale.US,
                                    "%,d تومان",
                                    price
                            )
                    );

                    if (oldPrice > 0) {

                        long difference =
                                price - oldPrice;

                        double percent =
                                difference *
                                100.0 /
                                oldPrice;

                        if (difference > 0) {

                            priceChangeText.setText(
                                    String.format(
                                            Locale.US,
                                            "🟢 ▲ افزایش %+,d تومان  (%+.2f%%)",
                                            difference,
                                            percent
                                    )
                            );

                            priceChangeText.setTextColor(
                                    Color.rgb(
                                            46,
                                            125,
                                            50
                                    )
                            );

                        } else if (difference < 0) {

                            priceChangeText.setText(
                                    String.format(
                                            Locale.US,
                                            "🔴 ▼ کاهش %,d تومان  (%+.2f%%)",
                                            Math.abs(difference),
                                            percent
                                    )
                            );

                            priceChangeText.setTextColor(
                                    Color.rgb(
                                            198,
                                            40,
                                            40
                                    )
                            );

                        } else {

                            priceChangeText.setText(
                                    "⚪ — بدون تغییر (0.00%)"
                            );

                            priceChangeText.setTextColor(
                                    Color.rgb(
                                            117,
                                            117,
                                            117
                                    )
                            );
                        }

                    } else {

                        priceChangeText.setText(
                                "⚪ — بدون تغییر"
                        );
                    }

                    lastUpdateText.setText(
                            "آخرین بروزرسانی " +
                            getSourceName() +
                            ": " +
                            sourceDate
                    );

                    statusText.setText(
                            "منبع فعال: " +
                            getSourceName()
                    );

                    calculateGoldValue();

                    updateChart();

                    checkTargets(
                            price
                    );
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        priceText.setText(
                                "خطا در دریافت قیمت " +
                                getSourceName()
                        )
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void updateChart() {

        if (chartView == null) {
            return;
        }

        JSONArray allData =
                PriceHistory.get(
                        this,
                        getSource()
                );

        JSONArray dataToShow =
                allData;

        if (chartRangeMillis != 0) {

            long cutoff =
                    System.currentTimeMillis()
                            - chartRangeMillis;

            JSONArray filtered =
                    new JSONArray();

            for (int i = 0;
                 i < allData.length();
                 i++) {

                try {

                    JSONObject item =
                            allData.getJSONObject(i);

                    long time =
                            item.getLong("time");

                    if (time >= cutoff) {
                        filtered.put(item);
                    }

                } catch (Exception ignored) {
                }
            }

            dataToShow =
                    filtered;
        }

        updateChartStats(
                dataToShow
        );

        chartView.setData(
                dataToShow
        );
    }

    private void updateChartStats(
            JSONArray data) {

        if (chartStatsText == null) {
            return;
        }

        if (data == null ||
                data.length() == 0) {

            chartStatsText.setText(
                    "📈 بیشترین: ---   📉 کمترین: ---   💰 آخرین: ---"
            );

            return;
        }

        long highest =
                Long.MIN_VALUE;

        long lowest =
                Long.MAX_VALUE;

        long latest = 0;

        try {

            for (int i = 0;
                 i < data.length();
                 i++) {

                JSONObject item =
                        data.getJSONObject(i);

                long price =
                        item.getLong("price");

                highest =
                        Math.max(
                                highest,
                                price
                        );

                lowest =
                        Math.min(
                                lowest,
                                price
                        );

                latest =
                        price;
            }

            chartStatsText.setText(
                    String.format(
                            Locale.US,
                            "📈 بیشترین: %,d   📉 کمترین: %,d   💰 آخرین: %,d تومان",
                            highest,
                            lowest,
                            latest
                    )
            );

        } catch (Exception ignored) {
        }
    }

    private void checkTargets(
            long price) {

        String source =
                getSource();

        String suffix =
                "_alerted_" + source;

        checkTarget(
                price,
                "high_target",
                "high_enabled",
                "high_alerted" + suffix,
                true,
                "🔺 هدف بالای طلا"
        );

        checkTarget(
                price,
                "high_target_2",
                "high_enabled_2",
                "high_alerted_2" + suffix,
                true,
                "🔺 هدف بالای طلا ۲"
        );

        checkTarget(
                price,
                "low_target",
                "low_enabled",
                "low_alerted" + suffix,
                false,
                "🔻 هدف پایین طلا"
        );

        checkTarget(
                price,
                "low_target_2",
                "low_enabled_2",
                "low_alerted_2" + suffix,
                false,
                "🔻 هدف پایین طلا ۲"
        );
    }

    private void checkTarget(
            long price,
            String targetKey,
            String enabledKey,
            String alertedKey,
            boolean high,
            String title) {

        try {

            String text =
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
                    text.isEmpty()) {

                return;
            }

            long target =
                    Long.parseLong(text);

            boolean reached =
                    high
                            ? price >= target
                            : price <= target;

            if (reached && !alerted) {

                sendNotification(
                        title,
                        "قیمت " +
                        getSourceName() +
                        " به " +
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

    private String formatPrice(
            long price) {

        return String.format(
                Locale.US,
                "%,d",
                price
        );
    }

    private void applyColors(
            LinearLayout layout,
            boolean dark) {

        int background =
                dark
                        ? Color.rgb(
                                25,
                                25,
                                25
                        )
                        : Color.WHITE;

        int text =
                dark
                        ? Color.WHITE
                        : Color.DKGRAY;

        layout.setBackgroundColor(
                background
        );

        for (int i = 0;
             i < layout.getChildCount();
             i++) {

            View child =
                    layout.getChildAt(i);

            if (child instanceof TextView) {

                ((TextView) child)
                        .setTextColor(text);
            }
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "هشدار قیمت طلا",
                            NotificationManager
                                    .IMPORTANCE_HIGH
                    );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    private void sendNotification(
            String title,
            String message) {

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission
                                .POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            builder =
                    new Notification.Builder(
                            this,
                            CHANNEL_ID
                    );

        } else {

            builder =
                    new Notification.Builder(
                            this
                    );
        }

        builder
                .setSmallIcon(
                        R.drawable.ic_notification_gold
                )
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(
                        Notification.PRIORITY_HIGH
                );

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            manager.notify(
                    (int)
                            System.currentTimeMillis(),
                    builder.build()
            );
        }
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(
                updater
        );

        super.onDestroy();
    }
}
