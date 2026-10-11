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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;


public class MainActivity extends Activity {

    private void saveFirebaseToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }

                    String token = task.getResult();

                    Log.d("MERGOLD_FCM", "FCM TOKEN = " + token);

                    Toast.makeText(
                            MainActivity.this,
                            "FCM آماده شد",
                            Toast.LENGTH_LONG
                    ).show();

                    getSharedPreferences(
                            "mrgold_firebase",
                            MODE_PRIVATE
                    ).edit()
                            .putString("fcm_token", token)
                            .apply();
                });
    }


    private TextView priceText;
    private TextView digikalaCompareText;
    private TextView milliCompareText;
    private TextView snappGoldCompareText;
    private TextView tgjuCompareText;
    private TextView marketDiffText;

    private TextView lastUpdateText;
    private TextView statusText;
    private TextView valueText;
    private TextView priceChangeText;
    private TextView chartStatsText;
    private TextView smartAnalysisText;
    private TextView highestText;
    private TextView lowestText;

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
    private TextView highStatus;
    private TextView highStatus2;
    private TextView lowStatus;
    private TextView lowStatus2;

    private RadioButton digikalaRadio;
    private RadioButton milliRadio;
    private RadioButton tgjuRadio;
    private RadioButton snappGoldRadio;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Handler comparisonHandler =
            new Handler(Looper.getMainLooper());

    private static final long COMPARISON_REFRESH_MS =
            30_000L;

    private SharedPreferences prefs;

    private static final String DIGIKALA_API =
            "https://api.digikala.com/wealth/v1/prices/";

    private static final String TGJU_URL =
            "https://www.tgju.org/profile/geram18";

    private static final String SNAPPGOLD_API = "https://snappgold.net/api/backend/api/v1/products/gold-price";

    private static final String MILLI_API =
            "https://milli.gold/api/v1/public/milli-price/external";

    private static final String CHANNEL_ID =
            "gold_alerts";

    private long currentPrice = 0;
    private long previousPrice = 0;

    private long chartRangeMillis =
            24L * 60L * 60L * 1000L;

    private final Runnable comparisonUpdater =
            new Runnable() {

                @Override
                public void run() {

                    updateSourceComparison();

                    comparisonHandler.postDelayed(
                            this,
                            COMPARISON_REFRESH_MS
                    );
                }
            };

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

        saveFirebaseToken();

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

        boolean backgroundEnabled =
                prefs.getBoolean(
                        "background_enabled",
                        true
                );

        if (backgroundEnabled) {
            startPriceService();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (prefs != null) {

            boolean dark =
                    prefs.getBoolean(
                            "dark_mode",
                            false
                    );

            android.util.Log.d(
                    "MrGoldDarkMode",
                    "onResume dark=" + dark
            );

            LinearLayout root =
                    findViewById(
                            R.id.mainRoot
                    );

            if (root != null) {

                applyColors(
                        root,
                        dark
                );
            }
        }
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

        digikalaCompareText =
                findViewById(
                        R.id.digikalaCompareText
                );

        milliCompareText =
                findViewById(
                        R.id.milliCompareText
                );

        snappGoldCompareText =
                findViewById(
                        R.id.snappGoldCompareText
                );

        tgjuCompareText =
                findViewById(
                        R.id.tgjuCompareText
                );

        marketDiffText =
                findViewById(
                        R.id.marketDiffText
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

        smartAnalysisText =
                findViewById(
                        R.id.smartAnalysisText
                );

        highestText =
                findViewById(
                        R.id.highestText
                );

        lowestText =
                findViewById(
                        R.id.lowestText
                );

        chartView =
                findViewById(
                        R.id.chartView
                );

        ImageButton settingsButton =
                findViewById(
                        R.id.settingsButton
                );

        settingsButton.setOnClickListener(
                v -> {
                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    SettingsActivity.class
                            );

                    startActivity(intent);
                }
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

        highStatus =
                findViewById(
                        R.id.highStatus
                );

        highStatus2 =
                findViewById(
                        R.id.highStatus2
                );

        lowStatus =
                findViewById(
                        R.id.lowStatus
                );

        lowStatus2 =
                findViewById(
                        R.id.lowStatus2
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

        tgjuRadio =
                findViewById(
                        R.id.tgjuRadio
                );
snappGoldRadio = findViewById(R.id.snappGoldRadio);

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

        Button days7Button =
                findViewById(
                        R.id.days7Button
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

        tgjuRadio.setChecked(
                "tgju".equals(source)
        );
        snappGoldRadio.setChecked("snappgold".equals(getSource()));

        digikalaRadio.setOnClickListener(
                v -> changeSource("digikala")
        );

        milliRadio.setOnClickListener(
                v -> changeSource("milli")
        );

        tgjuRadio.setOnClickListener(
                v -> changeSource("tgju")
        );
        snappGoldRadio.setOnClickListener(v -> changeSource("snappgold"));

        amountInput.setText(
                prefs.getString(
                        "gold_amount",
                        ""
                )
        );

        amountInput.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        calculateGoldValue();
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {
                    }
                }
        );

        highInput.setText(
                prefs.getString(
                        "high_target_" + getSourceKey(),
                        ""
                )
        );

        highSwitch.setChecked(
                prefs.getBoolean(
                        "high_enabled_" + getSourceKey(),
                        true
                )
        );

        highInput2.setText(
                prefs.getString(
                        "high_target_2_" + getSourceKey(),
                        ""
                )
        );

        highSwitch2.setChecked(
                prefs.getBoolean(
                        "high_enabled_2_" + getSourceKey(),
                        true
                )
        );

        lowInput.setText(
                prefs.getString(
                        "low_target_" + getSourceKey(),
                        ""
                )
        );

        lowSwitch.setChecked(
                prefs.getBoolean(
                        "low_enabled_" + getSourceKey(),
                        true
                )
        );

        lowInput2.setText(
                prefs.getString(
                        "low_target_2_" + getSourceKey(),
                        ""
                )
        );

        lowSwitch2.setChecked(
                prefs.getBoolean(
                        "low_enabled_2_" + getSourceKey(),
                        true
                )
        );

        saveButton.setOnClickListener(
                v -> saveSettings()
        );

        chartRangeMillis =
                24L * 60L * 60L * 1000L;

        dayButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            1L * 60L * 60L * 1000L;

                    updateChart();
                }
        );

        weekButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            6L * 60L * 60L * 1000L;

                    updateChart();
                }
        );

        monthButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            24L * 60L * 60L * 1000L;

                    updateChart();
                }
        );

        days7Button.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            7L * 24L * 60L * 60L * 1000L;

                    updateChart();
                }
        );

        LinearLayout marketPage =
                findViewById(
                        R.id.marketPage
                );

        LinearLayout toolsPage =
                findViewById(
                        R.id.toolsPage
                );

        LinearLayout marketNavButton =
                findViewById(
                        R.id.marketNavButton
                );

        LinearLayout toolsNavButton =
                findViewById(
                        R.id.toolsNavButton
                );

        TextView marketNavText =
                findViewById(
                        R.id.marketNavText
                );

        TextView toolsNavText =
                findViewById(
                        R.id.toolsNavText
                );

        marketNavButton.setOnClickListener(
                v -> {

                    marketPage.setVisibility(
                            View.VISIBLE
                    );

                    toolsPage.setVisibility(
                            View.GONE
                    );

                    marketNavText.setTextColor(
                            Color.rgb(
                                    184,
                                    138,
                                    22
                            )
                    );

                    toolsNavText.setTextColor(
                            Color.rgb(
                                    119,
                                    119,
                                    119
                            )
                    );
                }
        );

        toolsNavButton.setOnClickListener(
                v -> {

                    marketPage.setVisibility(
                            View.GONE
                    );

                    toolsPage.setVisibility(
                            View.VISIBLE
                    );

                    marketNavText.setTextColor(
                            Color.rgb(
                                    119,
                                    119,
                                    119
                            )
                    );

                    toolsNavText.setTextColor(
                            Color.rgb(
                                    184,
                                    138,
                                    22
                            )
                    );
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

        if ("milli".equals(getSource())) {
            return "میلی";
        }

        if ("tgju".equals(getSource())) {
            return "TGJU";
        }

        if ("snappgold".equals(getSource())) {
            return "اسنپ‌گلد";
        }

        return "دیجی‌کالا";
    }

    private String getSourceKey() {

        return getSource();
    }

    private void loadSourceSettings() {

        String sourceKey = getSourceKey();

        highInput.setText(
                prefs.getString(
                        "high_target_" + sourceKey,
                        ""
                )
        );

        highSwitch.setChecked(
                prefs.getBoolean(
                        "high_enabled_" + sourceKey,
                        true
                )
        );

        highInput2.setText(
                prefs.getString(
                        "high_target_2_" + sourceKey,
                        ""
                )
        );

        highSwitch2.setChecked(
                prefs.getBoolean(
                        "high_enabled_2_" + sourceKey,
                        true
                )
        );

        lowInput.setText(
                prefs.getString(
                        "low_target_" + sourceKey,
                        ""
                )
        );

        lowSwitch.setChecked(
                prefs.getBoolean(
                        "low_enabled_" + sourceKey,
                        true
                )
        );

        lowInput2.setText(
                prefs.getString(
                        "low_target_2_" + sourceKey,
                        ""
                )
        );

        lowSwitch2.setChecked(
                prefs.getBoolean(
                        "low_enabled_2_" + sourceKey,
                        true
                )
        );
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

        loadSourceSettings();

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

        if (prefs.getBoolean(
                "background_enabled",
                true
        )) {
            restartPriceService();
        }

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
                        "high_target_" + getSourceKey(),
                        highInput.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "low_target_" + getSourceKey(),
                        lowInput.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "high_target_2_" + getSourceKey(),
                        highInput2.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "low_target_2_" + getSourceKey(),
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
                        "high_enabled_" + getSourceKey(),
                        highSwitch.isChecked()
                )
                .putBoolean(
                        "low_enabled_" + getSourceKey(),
                        lowSwitch.isChecked()
                )
                .putBoolean(
                        "high_enabled_2_" + getSourceKey(),
                        highSwitch2.isChecked()
                )
                .putBoolean(
                        "low_enabled_2_" + getSourceKey(),
                        lowSwitch2.isChecked()
                )
                .apply();

        statusText.setText(
                "تنظیمات ذخیره شد ✓"
        );

        calculateGoldValue();

        if (currentPrice > 0) {
            updateAlertStatuses(currentPrice);
        }

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

            double milligram =
                    Double.parseDouble(
                            amount
                    );

            double grams =
                    milligram / 1000.0;

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

        if ("milli".equals(getSource())) {
            return MILLI_API;
        }

        if ("tgju".equals(getSource())) {
            return TGJU_URL;
        }

        if ("snappgold".equals(getSource())) {
            return SNAPPGOLD_API;
        }

        return DIGIKALA_API;
    }

    private long fetchTgjuPrice(HttpURLConnection connection)
            throws Exception {

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()
                        )
                );

        StringBuilder html =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            html.append(line);
        }

        reader.close();

        String page = html.toString();

        String label =
                "<td class=\"text-right\">نرخ فعلی</td>";

        int labelIndex =
                page.indexOf(label);

        if (labelIndex == -1) {
            throw new Exception("TGJU current price label not found");
        }

        int valueStart =
                page.indexOf(
                        "<td class=\"text-left\">",
                        labelIndex
                );

        if (valueStart == -1) {
            throw new Exception("TGJU current price value not found");
        }

        valueStart +=
                "<td class=\"text-left\">".length();

        int valueEnd =
                page.indexOf(
                        "</td>",
                        valueStart
                );

        if (valueEnd == -1) {
            throw new Exception("TGJU current price end not found");
        }

        String priceText =
                page.substring(
                        valueStart,
                        valueEnd
                )
                .replace(",", "")
                .trim();

        return Long.parseLong(priceText) / 10;
    }

    private long parsePrice(
            JSONObject json)
            throws Exception {

        if ("milli".equals(getSource())) {

            return json
                    .getJSONObject("data")
                    .getLong("price18") * 100;
        }

        return json
                .getJSONObject("gold18")
                .getLong("price") * 100;
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

    private long fetchSnappGoldPrice() throws Exception {
        URL url = new URL(SNAPPGOLD_API);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        conn.disconnect();

        return Long.parseLong(response.toString().trim());
    }

    private long parseTgjuHtml(String page)
            throws Exception {

        String label =
                "<td class=\"text-right\">نرخ فعلی</td>";

        int labelIndex =
                page.indexOf(label);

        if (labelIndex == -1) {
            throw new Exception("TGJU current price label not found");
        }

        int valueStart =
                page.indexOf(
                        "<td class=\"text-left\">",
                        labelIndex
                );

        if (valueStart == -1) {
            throw new Exception("TGJU current price value not found");
        }

        valueStart +=
                "<td class=\"text-left\">".length();

        int valueEnd =
                page.indexOf("</td>", valueStart);

        if (valueEnd == -1) {
            throw new Exception("TGJU current price end not found");
        }

        String priceText =
                page.substring(valueStart, valueEnd)
                        .replace(",", "")
                        .trim();

        return Long.parseLong(priceText) / 10;
    }

    private long fetchPriceFromSource(String source)
            throws Exception {

        String apiUrl;

        if ("milli".equals(source)) {
            apiUrl = MILLI_API;
        } else if ("tgju".equals(source)) {
            apiUrl = TGJU_URL;
        } else if ("snappgold".equals(source)) {
            return fetchSnappGoldPrice();
        } else {
            apiUrl = DIGIKALA_API;
        }

        HttpURLConnection connection = null;

        try {
            URL url = new URL(apiUrl);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);

            android.util.Log.d(
                    "MerGoldComparison",
                    "REQUEST " + source +
                    " HTTP=" + connection.getResponseCode()
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

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            android.util.Log.d(
                    "MerGoldComparison",
                    "RESPONSE " + source +
                    " LENGTH=" + response.length()
            );

            if ("tgju".equals(source)) {
                return parseTgjuHtml(response.toString());
            }

            JSONObject json =
                    new JSONObject(response.toString());

            if ("milli".equals(source)) {
                return json
                        .getJSONObject("data")
                        .getLong("price18") * 100;
            }

            return json
                    .getJSONObject("gold18")
                    .getLong("price") * 100;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private long parseComparisonJson(String source, JSONObject json)
            throws Exception {

        if ("milli".equals(source)) {
            return json
                    .getJSONObject("data")
                    .getLong("price18") * 100;
        }

        return json
                .getJSONObject("gold18")
                .getLong("price") * 100;
    }

    private long fetchComparisonPrice(String source)
            throws Exception {

        String urlString;

        if ("digikala".equals(source)) {
            urlString = DIGIKALA_API;
        } else if ("milli".equals(source)) {
            urlString = MILLI_API;
        } else if ("snappgold".equals(source)) {
            urlString = SNAPPGOLD_API;
        } else {
            urlString = TGJU_URL;
        }

        HttpURLConnection connection = null;

        try {
            connection =
                    (HttpURLConnection)
                            new URL(urlString).openConnection();

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

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            if ("tgju".equals(source)) {
                return parseTgjuHtml(
                        response.toString()
                );
            }

            if ("snappgold".equals(source)) {
                return Long.parseLong(
                        response.toString().trim()
                );
            }

            JSONObject json =
                    new JSONObject(
                            response.toString()
                    );

            return parseComparisonJson(
                    source,
                    json
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

                String source =
                        getSource();

                long price;
                JSONObject json = null;

                if ("tgju".equals(source)) {
                    price = parseTgjuHtml(response.toString());
                } else if ("snappgold".equals(source)) {
                    price = Long.parseLong(
                            response.toString().trim()
                    );
                } else {
                    json =
                            new JSONObject(
                                    response.toString()
                            );

                    price = parsePrice(json);
                }

                long oldPrice =
                        currentPrice;

                previousPrice =
                        oldPrice;

                currentPrice =
                        price;

                long widgetChange =
                        price - oldPrice;

                String widgetSource =
                        getSourceName();

                prefs.edit()
                        .putLong(
                                "widget_price",
                                price
                        )
                        .putLong(
                                "widget_change",
                                widgetChange
                        )
                        .putString(
                                "widget_source",
                                widgetSource
                        )
                        .apply();

                MerGoldWidget.updateWidgets(
                        this
                );

                PriceHistory.add(
                        this,
                        price,
                        source
                );

                final String sourceDate;

                if (!"tgju".equals(source)) {
                    sourceDate = parseSourceDate(json);
                } else {
                    sourceDate = "";
                }

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

                    updateAlertStatuses(
                            price
                    );

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

    private void updateSourceComparison() {

        new Thread(() -> {

            java.util.Map<String, Long> prices =
                    new java.util.HashMap<>();

            String[] sources = {
                    "digikala",
                    "milli",
                    "snappgold",
                    "tgju"
            };

            for (String source : sources) {

                try {

                    JSONArray history =
                            PriceHistory.get(
                                    MainActivity.this,
                                    source
                            );

                    if (history.length() > 0) {

                        JSONObject lastItem =
                                history.getJSONObject(
                                        history.length() - 1
                                );

                        long price =
                                lastItem.getLong("price");

                        if (price > 0) {
                            prices.put(
                                    source,
                                    price
                            );
                        }
                    }

                } catch (Exception e) {

                    android.util.Log.e(
                            "MerGoldComparison",
                            "History read failed for " +
                            source,
                            e
                    );
                }
            }

            runOnUiThread(() -> {

                updateComparisonText(
                        digikalaCompareText,
                        "دیجی‌کالا",
                        prices.get("digikala"),
                        prices.containsKey("digikala")
                );

                updateComparisonText(
                        milliCompareText,
                        "میلی",
                        prices.get("milli"),
                        prices.containsKey("milli")
                );

                updateComparisonText(
                        snappGoldCompareText,
                        "اسنپ‌گلد",
                        prices.get("snappgold"),
                        prices.containsKey("snappgold")
                );

                updateComparisonText(
                        tgjuCompareText,
                        "TGJU",
                        prices.get("tgju"),
                        prices.containsKey("tgju")
                );

                long minPrice = Long.MAX_VALUE;
                long maxPrice = Long.MIN_VALUE;
                int count = 0;

                for (Long price : prices.values()) {

                    if (price != null && price > 0) {

                        minPrice =
                                Math.min(
                                        minPrice,
                                        price
                                );

                        maxPrice =
                                Math.max(
                                        maxPrice,
                                        price
                                );

                        count++;
                    }
                }

                if (count >= 2) {

                    long difference =
                            maxPrice - minPrice;

                    marketDiffText.setText(
                            String.format(
                                    Locale.US,
                                    "اختلاف بازار: %,d تومان",
                                    difference
                            )
                    );

                } else {

                    marketDiffText.setText(
                            "اختلاف بازار: —"
                    );
                }
            });

        }).start();
    }

    private void updateComparisonText(
            TextView textView,
            String sourceName,
            Long price,
            boolean available) {

        if (textView == null) {
            return;
        }

        if (available && price != null && price > 0) {

            textView.setText(
                    String.format(
                            Locale.US,
                            "%s    %,d تومان",
                            sourceName,
                            price
                    )
            );

            textView.setTextColor(
                    Color.rgb(
                            51,
                            51,
                            51
                    )
            );

        } else {

            textView.setText(
                    sourceName +
                    "    در دسترس نیست"
            );

            textView.setTextColor(
                    Color.rgb(
                            150,
                            150,
                            150
                    )
            );
        }
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
                    "📈 بیشترین: ---   📉 کمترین: ---   💰 آخرین: ---   🔄 تغییر: ---"
            );

            if (highestText != null) {
                highestText.setText("---");
            }

            if (lowestText != null) {
                lowestText.setText("---");
            }

            return;
        }

        long highest =
                Long.MIN_VALUE;

        long lowest =
                Long.MAX_VALUE;

        long firstPrice = 0;
        long latest = 0;

        try {

            for (int i = 0;
                 i < data.length();
                 i++) {

                JSONObject item =
                        data.getJSONObject(i);

                long price =
                        item.getLong("price");

                if (i == 0) {
                    firstPrice = price;
                }

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

            long change =
                    latest - firstPrice;

            double changePercent =
                    firstPrice != 0
                            ? (change * 100.0) / firstPrice
                            : 0;

            String trendText;

            if (changePercent > 0.10) {
                trendText = "📈 روند صعودی";
            } else if (changePercent < -0.10) {
                trendText = "📉 روند نزولی";
            } else {
                trendText = "➖ روند تقریباً ثابت";
            }

            long distanceFromHigh =
                    highest - latest;

            long distanceFromLow =
                    latest - lowest;

            long lastTime = 0;

            try {
                JSONObject lastItem =
                        data.getJSONObject(
                                data.length() - 1
                        );

                lastTime =
                        lastItem.getLong("time");

            } catch (Exception ignored) {
            }

            String lastTimeText = "---";

            if (lastTime > 0) {

                java.text.SimpleDateFormat timeFormat =
                        new java.text.SimpleDateFormat(
                                "HH:mm",
                                Locale.US
                        );

                lastTimeText =
                        timeFormat.format(
                                new java.util.Date(lastTime)
                        );
            }

            String changeText;

            if (change > 0) {
                changeText =
                        String.format(
                                Locale.US,
                                "🔺 +%,d",
                                change
                        );
            } else if (change < 0) {
                changeText =
                        String.format(
                                Locale.US,
                                "🔻 %,d",
                                change
                        );
            } else {
                changeText = "➖ ۰";
            }

            chartStatsText.setText(
                    String.format(
                            Locale.US,
                            "📈 بیشترین: %,d   📉 کمترین: %,d   💰 آخرین: %,d   🔄 تغییر: %s تومان",
                            highest,
                            lowest,
                            latest,
                            changeText
                    )
            );

            if (smartAnalysisText != null) {

                smartAnalysisText.setText(
                        String.format(
                                Locale.US,
                                "🧠 %s   •   تغییر: %+.2f%%\n" +
                                "⬆️ فاصله تا سقف: %,d تومان   •   " +
                                "⬇️ فاصله تا کف: %,d تومان\n" +
                                "🕐 آخرین ثبت: %s",
                                trendText,
                                changePercent,
                                distanceFromHigh,
                                distanceFromLow,
                                lastTimeText
                        )
                );
            }

            if (highestText != null) {
                highestText.setText(
                        String.format(Locale.US, "%,d", highest)
                );
            }

            if (lowestText != null) {
                lowestText.setText(
                        String.format(Locale.US, "%,d", lowest)
                );
            }

        } catch (Exception ignored) {
        }
    }

    private void updateAlertStatuses(long price) {

        String sourceKey = getSourceKey();

        updateSingleAlertStatus(
                highStatus,
                "high_target_" + sourceKey,
                "high_enabled_" + sourceKey,
                price,
                true
        );

        updateSingleAlertStatus(
                highStatus2,
                "high_target_2_" + sourceKey,
                "high_enabled_2_" + sourceKey,
                price,
                true
        );

        updateSingleAlertStatus(
                lowStatus,
                "low_target_" + sourceKey,
                "low_enabled_" + sourceKey,
                price,
                false
        );

        updateSingleAlertStatus(
                lowStatus2,
                "low_target_2_" + sourceKey,
                "low_enabled_2_" + sourceKey,
                price,
                false
        );
    }

    private void updateSingleAlertStatus(
            TextView statusView,
            String targetKey,
            String enabledKey,
            long price,
            boolean high) {

        if (statusView == null) {
            return;
        }

        boolean enabled =
                prefs.getBoolean(
                        enabledKey,
                        true
                );

        String targetText =
                prefs.getString(
                        targetKey,
                        ""
                );

        if (!enabled) {

            statusView.setText(
                    "⚪ هشدار غیرفعال"
            );

            return;
        }

        if (targetText.isEmpty()) {

            statusView.setText(
                    "⚪ هدف تعیین نشده"
            );

            return;
        }

        try {

            long target =
                    Long.parseLong(targetText);

            boolean reached =
                    high
                            ? price >= target
                            : price <= target;

            if (reached) {

                statusView.setText(
                        "🔔 هشدار فعال شده"
                );

                return;
            }

            long distance =
                    Math.abs(
                            target - price
                    );

            long nearLimit =
                    Math.max(
                            1000,
                            target / 100
                    );

            if (distance <= nearLimit) {

                statusView.setText(
                        "🟠 نزدیک به هدف"
                );

            } else {

                statusView.setText(
                        "🟢 در انتظار رسیدن به هدف"
                );
            }

        } catch (Exception e) {

            statusView.setText(
                    "⚪ هدف نامعتبر"
            );
        }
    }

    private void checkTargets(
            long price) {

        String source =
                getSource();

        String suffix =
                "_alerted_" + source;

        String highTargetKey =
                "high_target_" + source;

        String highEnabledKey =
                "high_enabled_" + source;

        String highTarget2Key =
                "high_target_2_" + source;

        String highEnabled2Key =
                "high_enabled_2_" + source;

        String lowTargetKey =
                "low_target_" + source;

        String lowEnabledKey =
                "low_enabled_" + source;

        String lowTarget2Key =
                "low_target_2_" + source;

        String lowEnabled2Key =
                "low_enabled_2_" + source;

        checkTarget(
                price,
                highTargetKey,
                highEnabledKey,
                "high_alerted" + suffix,
                true,
                "🔺 هدف بالای طلا"
        );

        checkTarget(
                price,
                highTarget2Key,
                highEnabled2Key,
                "high_alerted_2" + suffix,
                true,
                "🔺 هدف بالای طلا ۲"
        );

        checkTarget(
                price,
                lowTargetKey,
                lowEnabledKey,
                "low_alerted" + suffix,
                false,
                "🔻 هدف پایین طلا"
        );

        checkTarget(
                price,
                lowTarget2Key,
                lowEnabled2Key,
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

        int background = dark
                ? Color.rgb(18, 18, 18)
                : Color.WHITE;

        int cardBackground = dark
                ? Color.rgb(32, 32, 32)
                : Color.WHITE;

        int primaryText = dark
                ? Color.rgb(245, 245, 245)
                : Color.rgb(45, 45, 45);

        int secondaryText = dark
                ? Color.rgb(185, 185, 185)
                : Color.rgb(110, 110, 110);

        layout.setBackgroundColor(background);

        applyColorsRecursive(
                layout,
                dark,
                cardBackground,
                primaryText,
                secondaryText
        );
    }

    private void applyColorsRecursive(
            View view,
            boolean dark,
            int cardBackground,
            int primaryText,
            int secondaryText) {

        if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                applyColorsRecursive(
                        group.getChildAt(i),
                        dark,
                        cardBackground,
                        primaryText,
                        secondaryText
                );
            }
        }

        if (view instanceof TextView) {

            TextView textView = (TextView) view;

            int currentColor =
                    textView.getCurrentTextColor();

            if (dark) {

                if (currentColor == Color.rgb(119, 119, 119)
                        || currentColor == Color.rgb(138, 128, 107)
                        || currentColor == Color.rgb(117, 105, 67)) {

                    textView.setTextColor(
                            secondaryText
                    );

                } else {

                    textView.setTextColor(
                            primaryText
                    );
                }
            }
        }

        if (view instanceof EditText) {

            EditText editText =
                    (EditText) view;

            if (dark) {
                editText.setTextColor(
                        primaryText
                );
                editText.setHintTextColor(
                        secondaryText
                );
            }
        }

        if (view instanceof Button) {

            Button button =
                    (Button) view;

            if (dark) {
                button.setTextColor(
                        primaryText
                );
            }
        }

        if (view instanceof RadioButton) {

            RadioButton radioButton =
                    (RadioButton) view;

            if (dark) {
                radioButton.setTextColor(
                        primaryText
                );
            }
        }

        if (view instanceof Switch) {

            Switch switchView =
                    (Switch) view;

            if (dark) {
                switchView.setTextColor(
                        primaryText
                );
            }
        }

        if (view instanceof Spinner) {

            Spinner spinner =
                    (Spinner) view;

            if (dark) {
                spinner.setBackgroundColor(
                        cardBackground
                );
            }
        }

        if (view.getId() == R.id.priceCard
                || view.getId() == R.id.sourceCard
                || view.getId() == R.id.statsCard
                || view.getId() == R.id.chartCard
                || view.getId() == R.id.amountCard
                ) {

            view.setBackgroundColor(
                    dark
                            ? cardBackground
                            : Color.WHITE
            );
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
                .setColor(
                        title.contains("بالای")
                                ? 0xFF2E7D32
                                : 0xFFC62828
                )
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(message)
                )
                .setAutoCancel(true)
                .setPriority(
                        Notification.PRIORITY_HIGH
                )
                .setCategory(
                        Notification.CATEGORY_ALARM
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

        comparisonHandler.removeCallbacks(
                comparisonUpdater
        );

        super.onDestroy();
    }
}
