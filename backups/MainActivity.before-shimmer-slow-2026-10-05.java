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
import android.animation.ValueAnimator;
import android.graphics.LinearGradient;
import android.graphics.Shader;
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
    private ValueAnimator goldPriceShimmerAnimator;
    private void startGoldPriceShimmer() {

        if (priceText == null) {
            return;
        }

        if (goldPriceShimmerAnimator != null) {
            goldPriceShimmerAnimator.cancel();
        }

        goldPriceShimmerAnimator =
                ValueAnimator.ofFloat(-1f, 2f);

        goldPriceShimmerAnimator.setDuration(2600);
        goldPriceShimmerAnimator.setStartDelay(800);
        goldPriceShimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        goldPriceShimmerAnimator.setRepeatMode(ValueAnimator.RESTART);

        goldPriceShimmerAnimator.addUpdateListener(
                animation -> {

                    if (priceText == null) {
                        return;
                    }

                    float progress =
                            (float) animation.getAnimatedValue();

                    float width =
                            Math.max(
                                    priceText.getWidth(),
                                    1
                            );

                    float center =
                            progress * width;

                    LinearGradient gradient =
                            new LinearGradient(
                                    center - width * 0.28f,
                                    0,
                                    center + width * 0.28f,
                                    0,
                                    new int[]{
                                            0xFF806000,
                                            0xFFD4AF37,
                                            0xFFFFF1A8,
                                            0xFFD4AF37,
                                            0xFF806000
                                    },
                                    new float[]{
                                            0.0f,
                                            0.35f,
                                            0.5f,
                                            0.65f,
                                            1.0f
                                    },
                                    Shader.TileMode.CLAMP
                            );

                    priceText.getPaint().setShader(gradient);
                    priceText.invalidate();
                }
        );

        goldPriceShimmerAnimator.start();
    }

    private void stopGoldPriceShimmer() {

        if (goldPriceShimmerAnimator != null) {
            goldPriceShimmerAnimator.cancel();
            goldPriceShimmerAnimator = null;
        }

        if (priceText != null) {
            priceText.getPaint().setShader(null);
            priceText.invalidate();
        }
    }

    private TextView digikalaComparePrice;
    private TextView milliComparePrice;
    private TextView snappGoldComparePrice;
    private TextView tgjuComparePrice;
    private TextView marketDiffText;

    private TextView lastUpdateText;
    private TextView statusText;
    private TextView valueText;
    private TextView quickWeightText;
    private TextView quickPriceBasisText;
    private TextView quickSourceText;
    private TextView calculatorLivePrice;
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

    private LinearLayout highBox;
    private LinearLayout lowBox;
    private TextView highTab;
    private TextView lowTab;
    private EditText amountInput;

    private EditText madeWeightInput;
    private EditText madeWageInput;
    private EditText madeProfitInput;
    private EditText madeTaxInput;

    private TextView madeBaseValue;
    private TextView madeFinalValue;

    private EditText meltedWeightInput;
    private EditText meltedKaratInput;

    private TextView meltedBaseValue;
    private TextView meltedFinalValue;

    private EditText sellWeightInput;
    private EditText sellKaratInput;
    private EditText sellDeductionInput;

    private TextView sellBaseValue;
    private TextView sellFinalValue;

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

private final java.util.IdentityHashMap<TextView, Integer> originalTextColors =
        new java.util.IdentityHashMap<>();

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

        try {
            buildUI();
        } catch (Throwable e) {
            android.widget.TextView error = new android.widget.TextView(this);
            error.setText(
                    "MRGOLD CRASH FOUND\\n\\n" +
                    e.toString() + "\\n\\n" +
                    android.util.Log.getStackTraceString(e)
            );
            error.setTextSize(14);
            error.setTextColor(android.graphics.Color.RED);
            error.setPadding(24, 24, 24, 24);
            setContentView(error);
            return;
        }

        getGoldPrice();

        updateSourceComparison();

        comparisonHandler.postDelayed(
                comparisonUpdater,
                COMPARISON_REFRESH_MS
        );

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

        startGoldPriceShimmer();

        if (prefs != null) {

            boolean dark =
                getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                ).getBoolean(
                        "dark_mode",
                        false
                );

            android.util.Log.d(
                    "MrGoldDarkMode",
                    "onResume dark=" + dark
            );

            LinearLayout container =
                    findViewById(R.id.mainContainer);

            ScrollView scroll =
                    findViewById(R.id.mainScroll);

            if (container != null) {
                container.setBackgroundColor(
                        dark
                                ? Color.rgb(18, 18, 18)
                                : Color.rgb(246, 244, 239)
                );
            }

            if (scroll != null) {
                scroll.setBackgroundColor(
                        dark
                                ? Color.rgb(18, 18, 18)
                                : Color.rgb(246, 244, 239)
                );
            }

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

            if (chartView != null) {
                chartView.setDarkMode(dark);
            }
        }
    }

    private void buildUI() {

        boolean dark =
                getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                ).getBoolean(
                        "dark_mode",
                        false
                );

        setContentView(
                R.layout.activity_main
        );

        LinearLayout container =
                findViewById(R.id.mainContainer);

        ScrollView scroll =
                findViewById(R.id.mainScroll);

        if (container != null) {
            container.setBackgroundColor(
                    dark
                            ? Color.rgb(18, 18, 18)
                            : Color.rgb(246, 244, 239)
            );
        }

        if (scroll != null) {
            scroll.setBackgroundColor(
                    dark
                            ? Color.rgb(18, 18, 18)
                            : Color.rgb(246, 244, 239)
            );
        }

        LinearLayout root =
                findViewById(
                        R.id.mainRoot
                );

        priceText =
                findViewById(
                        R.id.priceText
                );



        digikalaComparePrice =
                findViewById(
                        R.id.digikalaComparePrice
                );



        milliComparePrice =
                findViewById(
                        R.id.milliComparePrice
                );



        snappGoldComparePrice =
                findViewById(
                        R.id.snappGoldComparePrice
                );



        tgjuComparePrice =
                findViewById(
                        R.id.tgjuComparePrice
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

        if (chartView != null) {
            chartView.setDarkMode(dark);
        }

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

        quickWeightText =
                findViewById(
                        R.id.quickWeightText
                );

        quickPriceBasisText =
                findViewById(
                        R.id.quickPriceBasisText
                );

        quickSourceText =
                findViewById(
                        R.id.quickSourceText
                );

        calculatorLivePrice =
                findViewById(
                        R.id.calculatorLivePrice
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

        highBox =
                findViewById(
                        R.id.highBox
                );

        lowBox =
                findViewById(
                        R.id.lowBox
                );

        highTab =
                findViewById(
                        R.id.highTab
                );

        lowTab =
                findViewById(
                        R.id.lowTab
                );

        statusText =
                findViewById(
                        R.id.statusText
                );

        highBox.setVisibility(View.VISIBLE);
        lowBox.setVisibility(View.GONE);

        highTab.setOnClickListener(v -> {
            highBox.setVisibility(View.VISIBLE);
            lowBox.setVisibility(View.GONE);

            highTab.setBackgroundResource(R.drawable.target_tab_high);
            highTab.setTextColor(Color.parseColor("#355D28"));

            lowTab.setBackgroundResource(0);
            lowTab.setTextColor(Color.parseColor("#777777"));
        });

        lowTab.setOnClickListener(v -> {
            highBox.setVisibility(View.GONE);
            lowBox.setVisibility(View.VISIBLE);

            lowTab.setBackgroundResource(R.drawable.target_tab_low);
            lowTab.setTextColor(Color.parseColor("#355D28"));

            highTab.setBackgroundResource(0);
            highTab.setTextColor(Color.parseColor("#777777"));
        });

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

        Button chartLineButton =
                findViewById(
                        R.id.chartLineButton
                );

        Button chartCandleButton =
                findViewById(
                        R.id.chartCandleButton
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

        setupCalculatorModes();

        amountInput.setText(
                prefs.getString(
                        "gold_amount",
                        ""
                )
        );

        madeWeightInput =
                findViewById(R.id.madeWeightInput);

        madeWageInput =
                findViewById(R.id.madeWageInput);

        madeProfitInput =
                findViewById(R.id.madeProfitInput);

        madeTaxInput =
                findViewById(R.id.madeTaxInput);

        madeBaseValue =
                findViewById(R.id.madeBaseValue);

        madeFinalValue =
                findViewById(R.id.madeFinalValue);

        meltedWeightInput =
                findViewById(R.id.meltedWeightInput);

        meltedKaratInput =
                findViewById(R.id.meltedKaratInput);

        meltedBaseValue =
                findViewById(R.id.meltedBaseValue);

        meltedFinalValue =
                findViewById(R.id.meltedFinalValue);

        sellWeightInput =
                findViewById(R.id.sellWeightInput);

        sellKaratInput =
                findViewById(R.id.sellKaratInput);

        sellDeductionInput =
                findViewById(R.id.sellDeductionInput);

        sellBaseValue =
                findViewById(R.id.sellBaseValue);

        sellFinalValue =
                findViewById(R.id.sellFinalValue);

        android.text.TextWatcher madeWatcher =
                new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        calculateMadeGoldValue();
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {}
                };

        madeWeightInput.addTextChangedListener(madeWatcher);
        madeWageInput.addTextChangedListener(madeWatcher);
        madeProfitInput.addTextChangedListener(madeWatcher);
        madeTaxInput.addTextChangedListener(madeWatcher);

        android.text.TextWatcher meltedWatcher =
                new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        calculateMeltedGoldValue();
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {}
                };

        meltedWeightInput.addTextChangedListener(meltedWatcher);
        meltedKaratInput.addTextChangedListener(meltedWatcher);

        android.text.TextWatcher sellWatcher =
                new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        calculateSellGoldValue();
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {}
                };

        sellWeightInput.addTextChangedListener(sellWatcher);
        sellKaratInput.addTextChangedListener(sellWatcher);
        sellDeductionInput.addTextChangedListener(sellWatcher);

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

        // وضعیت فعال دکمه‌های نمودار
        java.util.function.Consumer<Button> selectRangeButton =
                selected -> {
                    dayButton.setSelected(false);
                    weekButton.setSelected(false);
                    monthButton.setSelected(false);
                    days7Button.setSelected(false);
                    selected.setSelected(true);
                };

        java.util.function.Consumer<Button> selectChartButton =
                selected -> {
                    chartLineButton.setSelected(false);
                    chartCandleButton.setSelected(false);
                    selected.setSelected(true);
                };

        dayButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            1L * 60L * 60L * 1000L;

                    selectRangeButton.accept(dayButton);
                    updateChart();
                }
        );

        weekButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            6L * 60L * 60L * 1000L;

                    selectRangeButton.accept(weekButton);
                    updateChart();
                }
        );

        monthButton.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            24L * 60L * 60L * 1000L;

                    selectRangeButton.accept(monthButton);
                    updateChart();
                }
        );

        days7Button.setOnClickListener(
                v -> {

                    chartRangeMillis =
                            7L * 24L * 60L * 60L * 1000L;

                    selectRangeButton.accept(days7Button);
                    updateChart();
                }
        );

        // نوع نمودار
        chartLineButton.setOnClickListener(
                v -> {
                    selectChartButton.accept(chartLineButton);
                    if (chartView != null) {
                        chartView.setChartType(false);
                    }
                }
        );

        chartCandleButton.setOnClickListener(
                v -> {
                    selectChartButton.accept(chartCandleButton);
                    if (chartView != null) {
                        chartView.setChartType(true);
                    }
                }
        );

        // وضعیت پیش‌فرض: نمودار خطی + بازه ۲۴ ساعت
        selectChartButton.accept(chartLineButton);
        selectRangeButton.accept(monthButton);

        if (chartView != null) {
            chartView.setChartType(false);
        }

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

                    marketNavButton.setBackgroundResource(
                            R.drawable.nav_button_gold
                    );

                    toolsNavButton.setBackgroundResource(
                            R.drawable.calculator_tab_inactive
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

                    marketNavButton.setBackgroundResource(
                            R.drawable.calculator_tab_inactive
                    );

                    toolsNavButton.setBackgroundResource(
                            R.drawable.nav_button_gold
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

            applyColors(root, dark);
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

    private void setupCalculatorModes() {

        TextView quick =
                findViewById(R.id.calcModeQuick);

        TextView made =
                findViewById(R.id.calcModeMade);

        TextView melted =
                findViewById(R.id.calcModeMelted);

        TextView sell =
                findViewById(R.id.calcModeSell);

        LinearLayout quickPanel =
                findViewById(R.id.calcQuickPanel);

        LinearLayout madePanel =
                findViewById(R.id.calcMadePanel);

        LinearLayout meltedPanel =
                findViewById(R.id.calcMeltedPanel);

        LinearLayout sellPanel =
                findViewById(R.id.calcSellPanel);

        View.OnClickListener listener = v -> {

            quickPanel.setVisibility(View.GONE);
            madePanel.setVisibility(View.GONE);
            meltedPanel.setVisibility(View.GONE);
            sellPanel.setVisibility(View.GONE);

            quick.setTextColor(Color.rgb(102, 102, 102));
            made.setTextColor(Color.rgb(102, 102, 102));
            melted.setTextColor(Color.rgb(102, 102, 102));
            sell.setTextColor(Color.rgb(102, 102, 102));

            quick.setBackgroundResource(R.drawable.calculator_tab_inactive);
            made.setBackgroundResource(R.drawable.calculator_tab_inactive);
            melted.setBackgroundResource(R.drawable.calculator_tab_inactive);
            sell.setBackgroundResource(R.drawable.calculator_tab_inactive);

            if (v == quick) {
                quickPanel.setVisibility(View.VISIBLE);
                quick.setTextColor(Color.WHITE);
                quick.setBackgroundResource(R.drawable.button_gold);
                calculateGoldValue();

            } else if (v == made) {
                madePanel.setVisibility(View.VISIBLE);
                made.setTextColor(Color.WHITE);
                made.setBackgroundResource(R.drawable.button_gold);

            } else if (v == melted) {
                meltedPanel.setVisibility(View.VISIBLE);
                melted.setTextColor(Color.WHITE);
                melted.setBackgroundResource(R.drawable.button_gold);

            } else if (v == sell) {
                sellPanel.setVisibility(View.VISIBLE);
                sell.setTextColor(Color.WHITE);
                sell.setBackgroundResource(R.drawable.button_gold);
            }
        };

        quick.setOnClickListener(listener);
        made.setOnClickListener(listener);
        melted.setOnClickListener(listener);
        sell.setOnClickListener(listener);

        quick.performClick();
    }

    private void calculateGoldValue() {

        try {

            String amount =
                    amountInput.getText()
                            .toString()
                            .trim();

            if (quickSourceText != null) {
                quickSourceText.setText(
                        "منبع قیمت: " + getSourceName()
                );
            }

            if (amount.isEmpty() ||
                    currentPrice <= 0) {

                valueText.setText(
                        "---"
                );

                if (quickWeightText != null) {
                    quickWeightText.setText("وزن: --- گرم");
                }

                if (quickPriceBasisText != null) {
                    quickPriceBasisText.setText(
                            "بر اساس قیمت لحظه‌ای: ---"
                    );
                }

                return;
            }

            double milligram =
                    Double.parseDouble(
                            amount
                    );

            double grams =
                    milligram / 1000.0;

            if (quickWeightText != null) {
                quickWeightText.setText(
                        "وزن: " +
                        String.format(
                                Locale.US,
                                "%,.3f گرم",
                                grams
                        )
                );
            }

            if (quickPriceBasisText != null) {
                quickPriceBasisText.setText(
                        String.format(
                                Locale.US,
                                "%,d تومان",
                                currentPrice
                        )
                );
            }

            double value =
                    grams * currentPrice;

            valueText.setText(
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

            if (quickWeightText != null) {
                quickWeightText.setText("وزن: --- گرم");
            }

            if (quickPriceBasisText != null) {
                quickPriceBasisText.setText(
                        "بر اساس قیمت لحظه‌ای: ---"
                );
            }
        }
    }

    private void calculateMadeGold() {

        try {

            if (currentPrice <= 0) {
                madeBaseValue.setText("ارزش پایه طلا: ---");
                madeFinalValue.setText("قیمت نهایی: ---");
                return;
            }

            String weightText =
                    madeWeightInput.getText().toString().trim();

            String wageText =
                    madeWageInput.getText().toString().trim();

            String profitText =
                    madeProfitInput.getText().toString().trim();

            String taxText =
                    madeTaxInput.getText().toString().trim();

            if (weightText.isEmpty()) {
                madeBaseValue.setText("ارزش پایه طلا: ---");
                madeFinalValue.setText("قیمت نهایی: ---");
                return;
            }

            double weight =
                    Double.parseDouble(weightText);

            double wagePercent =
                    wageText.isEmpty()
                            ? 0
                            : Double.parseDouble(wageText);

            double profitPercent =
                    profitText.isEmpty()
                            ? 0
                            : Double.parseDouble(profitText);

            double taxPercent =
                    taxText.isEmpty()
                            ? 0
                            : Double.parseDouble(taxText);

            if (weight <= 0) {
                madeBaseValue.setText("ارزش پایه طلا: ---");
                madeFinalValue.setText("قیمت نهایی: ---");
                return;
            }

            double baseValue =
                    weight * currentPrice;

            double wage =
                    baseValue * wagePercent / 100.0;

            double profitBase =
                    baseValue + wage;

            double profit =
                    profitBase * profitPercent / 100.0;

            double taxBase =
                    wage + profit;

            double tax =
                    taxBase * taxPercent / 100.0;

            double finalValue =
                    baseValue + wage + profit + tax;

            madeBaseValue.setText(
                    "ارزش پایه طلا: " +
                    String.format(
                            Locale.US,
                            "%,.0f تومان",
                            baseValue
                    )
            );

            madeFinalValue.setText(
                    "قیمت نهایی: " +
                    String.format(
                            Locale.US,
                            "%,.0f تومان",
                            finalValue
                    )
            );

        } catch (Exception e) {

            madeBaseValue.setText("ارزش پایه طلا: ---");
            madeFinalValue.setText("قیمت نهایی: ---");
        }
    }


    private void calculateSellGoldValue() {

        try {

            if (currentPrice <= 0) {
                sellBaseValue.setText("ارزش قبل از کسر: ---");
                sellFinalValue.setText("مبلغ دریافتی: ---");
                return;
            }

            double weight = getNumber(sellWeightInput);
            double karat = getNumber(sellKaratInput);
            double deduction = getNumber(sellDeductionInput);

            if (weight <= 0 || karat <= 0) {
                sellBaseValue.setText("ارزش قبل از کسر: ---");
                sellFinalValue.setText("مبلغ دریافتی: ---");
                return;
            }

            double baseValue =
                    weight * currentPrice * karat / 750.0;

            double finalValue =
                    baseValue * (1.0 - deduction / 100.0);

            sellBaseValue.setText(
                    String.format(
                            Locale.US,
                            "ارزش قبل از کسر: %,.0f تومان",
                            baseValue
                    )
            );

            sellFinalValue.setText(
                    String.format(
                            Locale.US,
                            "%,.0f تومان",
                            finalValue
                    )
            );

        } catch (Exception e) {

            sellBaseValue.setText("ارزش قبل از کسر: ---");
            sellFinalValue.setText("مبلغ دریافتی: ---");
        }
    }


    private void calculateMeltedGoldValue() {

        try {

            if (currentPrice <= 0) {
                meltedBaseValue.setText("ارزش محاسباتی: ---");
                meltedFinalValue.setText("ارزش نهایی: ---");
                return;
            }

            double weight = getNumber(meltedWeightInput);
            double karat = getNumber(meltedKaratInput);

            if (weight <= 0 || karat <= 0) {
                meltedBaseValue.setText("ارزش محاسباتی: ---");
                meltedFinalValue.setText("ارزش نهایی: ---");
                return;
            }

            double goldValue =
                    weight * currentPrice;

            double adjustedValue =
                    goldValue * karat / 750.0;

            meltedBaseValue.setText(
                    String.format(
                            Locale.US,
                            "ارزش بر اساس وزن: %,.0f تومان",
                            goldValue
                    )
            );

            meltedFinalValue.setText(
                    String.format(
                            Locale.US,
                            "%,.0f تومان",
                            adjustedValue
                    )
            );

        } catch (Exception e) {

            meltedBaseValue.setText("ارزش محاسباتی: ---");
            meltedFinalValue.setText("ارزش نهایی: ---");
        }
    }


    private void calculateMadeGoldValue() {

        try {

            if (currentPrice <= 0) {
                return;
            }

            double weight = getNumber(madeWeightInput);
            double wage = getNumber(madeWageInput);
            double profit = getNumber(madeProfitInput);
            double tax = getNumber(madeTaxInput);

            double goldValue = weight * currentPrice;

            double wageValue = goldValue * wage / 100.0;

            double profitValue =
                    (goldValue + wageValue) * profit / 100.0;

            double taxValue =
                    (wageValue + profitValue) * tax / 100.0;

            double finalValue =
                    goldValue +
                    wageValue +
                    profitValue +
                    taxValue;


            madeBaseValue.setText(
                    String.format(
                            Locale.US,
                            "ارزش خام طلا: %,.0f تومان",
                            goldValue
                    )
            );


            madeFinalValue.setText(
                    String.format(
                            Locale.US,
                            "%,.0f تومان",
                            finalValue
                    )
            );


        } catch(Exception e) {

            madeFinalValue.setText("---");
        }
    }


    private double getNumber(EditText editText) {

        try {

            String value =
                    editText.getText()
                            .toString()
                            .trim();

            if(value.isEmpty())
                return 0;

            return Double.parseDouble(value);

        } catch(Exception e) {

            return 0;
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

    private void getGoldPrice() {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                final String requestSource =
                        getSource();

                final String requestUrl;

                if ("milli".equals(requestSource)) {
                    requestUrl = MILLI_API;
                } else if ("tgju".equals(requestSource)) {
                    requestUrl = TGJU_URL;
                } else if ("snappgold".equals(requestSource)) {
                    requestUrl = SNAPPGOLD_API;
                } else {
                    requestUrl = DIGIKALA_API;
                }

                URL url =
                        new URL(
                                requestUrl
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
                        requestSource;

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

                    if (calculatorLivePrice != null) {
                        calculatorLivePrice.setText(
                                String.format(
                                        Locale.US,
                                        "%,d تومان",
                                        price
                                )
                        );
                    }

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
}
            }

            runOnUiThread(() -> {

                updateComparisonText(
                        digikalaComparePrice,
                        prices.get("digikala"),
                        prices.containsKey("digikala")
                );

                updateComparisonText(
                        milliComparePrice,
                        prices.get("milli"),
                        prices.containsKey("milli")
                );

                updateComparisonText(
                        snappGoldComparePrice,
                        prices.get("snappgold"),
                        prices.containsKey("snappgold")
                );

                updateComparisonText(
                        tgjuComparePrice,
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
            TextView priceTextView,
            Long price,
            boolean available) {

        if (priceTextView == null) {
            return;
        }

        if (available && price != null && price > 0) {

            priceTextView.setText(
                    String.format(
                            Locale.US,
                            "%,d تومان",
                            price
                    )
            );

            priceTextView.setTextColor(
                    Color.rgb(
                            51,
                            51,
                            51
                    )
            );

        } else {

            priceTextView.setText(
                    "در دسترس نیست"
            );

            priceTextView.setTextColor(
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

        String rapidChangeText =
                prefs.getString(
                        "rapid_change_percent_" + getSource(),
                        "0"
                );

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

                double rapidPercent =
                        Double.parseDouble(rapidChangeText);

                String shortTermText;

                if (rapidPercent >= 0.5) {
                    shortTermText = "⚡ حرکت کوتاه‌مدت: صعودی";
                } else if (rapidPercent <= -0.5) {
                    shortTermText = "⚡ حرکت کوتاه‌مدت: نزولی";
                } else {
                    shortTermText = "⚡ حرکت کوتاه‌مدت: آرام";
                }

                String positionText;

                long range =
                        highest - lowest;

                if (range > 0 &&
                        distanceFromHigh <= range * 0.15) {

                    positionText =
                            "📍 موقعیت: نزدیک سقف بازه";

                } else if (range > 0 &&
                        distanceFromLow <= range * 0.15) {

                    positionText =
                            "📍 موقعیت: نزدیک کف بازه";

                } else {

                    positionText =
                            "📍 موقعیت: میانه بازه";
                }

                String summaryText;

                if (changePercent > 0.10 &&
                        rapidPercent > 0.10) {

                    summaryText =
                            "🧠 جمع‌بندی: حرکت اخیر هم‌جهت با روند بازه است.";

                } else if (changePercent < -0.10 &&
                        rapidPercent < -0.10) {

                    summaryText =
                            "🧠 جمع‌بندی: کاهش اخیر هم‌جهت با روند بازه است.";

                } else if (Math.abs(changePercent) <= 0.10 &&
                        Math.abs(rapidPercent) <= 0.10) {

                    summaryText =
                            "🧠 جمع‌بندی: نوسان قیمت در این بازه محدود است.";

                } else {

                    summaryText =
                            "🧠 جمع‌بندی: حرکت کوتاه‌مدت با روند کلی کاملاً هم‌جهت نیست.";
                }

                // ==============================
                // Market Score — تحلیل چندشاخصی
                // ==============================

                double trendScore;

                if (changePercent >= 1.0) {
                    trendScore = 30.0;
                } else if (changePercent >= 0.10) {
                    trendScore =
                            15.0 +
                            ((changePercent - 0.10) / 0.90) * 15.0;
                } else if (changePercent <= -1.0) {
                    trendScore = 0.0;
                } else if (changePercent < -0.10) {
                    trendScore =
                            ((changePercent + 1.0) / 0.90) * 15.0;
                } else {
                    trendScore = 15.0;
                }

                double rapidScore;

                if (rapidPercent >= 0.5) {
                    rapidScore = 25.0;
                } else if (rapidPercent > 0.10) {
                    rapidScore =
                            15.0 +
                            ((rapidPercent - 0.10) / 0.40) * 10.0;
                } else if (rapidPercent <= -0.5) {
                    rapidScore = 0.0;
                } else if (rapidPercent < -0.10) {
                    rapidScore =
                            ((rapidPercent + 0.5) / 0.40) * 15.0;
                } else {
                    rapidScore = 15.0;
                }

                double positionScore;

                if (range <= 0) {

                    positionScore = 10.0;

                } else {

                    double positionRatio =
                            distanceFromLow * 1.0 / range;

                    if (positionRatio <= 0.15) {
                        positionScore = 20.0;
                    } else if (positionRatio >= 0.85) {
                        positionScore = 0.0;
                    } else {
                        positionScore =
                                20.0 -
                                ((positionRatio - 0.15) / 0.70) * 20.0;
                    }
                }

                double alignmentScore;

                boolean trendPositive =
                        changePercent > 0.10;

                boolean trendNegative =
                        changePercent < -0.10;

                boolean rapidPositive =
                        rapidPercent > 0.10;

                boolean rapidNegative =
                        rapidPercent < -0.10;

                if ((trendPositive && rapidPositive) ||
                    (trendNegative && rapidNegative)) {

                    alignmentScore = 15.0;

                } else if (!trendPositive &&
                           !trendNegative &&
                           !rapidPositive &&
                           !rapidNegative) {

                    alignmentScore = 7.5;

                } else {

                    alignmentScore = 3.0;
                }

                double dataScore;

                if (data.length() >= 60) {
                    dataScore = 10.0;
                } else if (data.length() >= 30) {
                    dataScore = 8.0;
                } else if (data.length() >= 15) {
                    dataScore = 6.0;
                } else if (data.length() >= 5) {
                    dataScore = 4.0;
                } else {
                    dataScore = 2.0;
                }

                int marketScore =
                        (int) Math.round(
                                Math.max(
                                        0.0,
                                        Math.min(
                                                100.0,
                                                trendScore +
                                                rapidScore +
                                                positionScore +
                                                alignmentScore +
                                                dataScore
                                        )
                                )
                        );

                int confidenceScore;

                if (data.length() >= 60) {
                    confidenceScore = 85;
                } else if (data.length() >= 30) {
                    confidenceScore = 75;
                } else if (data.length() >= 15) {
                    confidenceScore = 65;
                } else if (data.length() >= 5) {
                    confidenceScore = 50;
                } else {
                    confidenceScore = 30;
                }

                if (alignmentScore >= 15.0) {
                    confidenceScore += 10;
                } else if (alignmentScore <= 3.0) {
                    confidenceScore -= 10;
                }

                confidenceScore =
                        Math.max(
                                0,
                                Math.min(
                                        100,
                                        confidenceScore
                                )
                        );

                String marketState;

                if (marketScore >= 70) {
                    marketState =
                            "🟢 وضعیت بازار: مثبت";
                } else if (marketScore >= 55) {
                    marketState =
                            "🟡 وضعیت بازار: متمایل به مثبت";
                } else if (marketScore >= 45) {
                    marketState =
                            "⚪ وضعیت بازار: خنثی";
                } else if (marketScore >= 30) {
                    marketState =
                            "🟠 وضعیت بازار: متمایل به منفی";
                } else {
                    marketState =
                            "🔴 وضعیت بازار: منفی";
                }

                String scoreText =
                        String.format(
                                Locale.US,
                                "📊 امتیاز بازار: %d/100   •   اطمینان: %d%%",
                                marketScore,
                                confidenceScore
                        );

                // ==============================
                // Entry Position — ارزیابی چندبازه‌ای موقعیت ورود
                // ==============================

                JSONArray allHistory =
                        PriceHistory.get(
                                this,
                                getSource()
                        );

                long currentPrice = latest;

                long latestHistoryTime =
                        Long.MIN_VALUE;

                for (int i = 0;
                     i < allHistory.length();
                     i++) {

                    try {

                        JSONObject item =
                                allHistory.getJSONObject(i);

                        long itemTime =
                                item.getLong("time");

                        if (itemTime > latestHistoryTime) {

                            latestHistoryTime =
                                    itemTime;

                            currentPrice =
                                    item.getLong("price");
                        }

                    } catch (Exception ignored) {
                    }
                }

                // ==============================
                // Entry Position V2 — امتیازدهی ۱۰۰ امتیازی
                // ==============================

                double entryScore = 0.0;

                double score24h = 50.0;
                double score7d = 50.0;
                double score30d = 50.0;

                int validRanges = 0;

                long nowForEntry =
                        System.currentTimeMillis();

                long[] rangeDurations = {
                        24L * 60L * 60L * 1000L,
                        7L * 24L * 60L * 60L * 1000L,
                        30L * 24L * 60L * 60L * 1000L
                };

                double[] rangeWeights = {
                        25.0,
                        20.0,
                        15.0
                };

                double[] rangeScores = {
                        50.0,
                        50.0,
                        50.0
                };

                double validWeightTotal = 0.0;

                for (int r = 0;
                     r < rangeDurations.length;
                     r++) {

                    long cutoffEntry =
                            nowForEntry - rangeDurations[r];

                    long rangeHighEntry =
                            Long.MIN_VALUE;

                    long rangeLowEntry =
                            Long.MAX_VALUE;

                    int rangeCountEntry = 0;

                    for (int i = 0;
                         i < allHistory.length();
                         i++) {

                        try {

                            JSONObject item =
                                    allHistory.getJSONObject(i);

                            long itemTime =
                                    item.getLong("time");

                            if (itemTime >= cutoffEntry) {

                                long itemPrice =
                                        item.getLong("price");

                                rangeHighEntry =
                                        Math.max(
                                                rangeHighEntry,
                                                itemPrice
                                        );

                                rangeLowEntry =
                                        Math.min(
                                                rangeLowEntry,
                                                itemPrice
                                        );

                                rangeCountEntry++;
                            }

                        } catch (Exception ignored) {
                        }
                    }

                    if (rangeCountEntry > 0 &&
                        rangeHighEntry > rangeLowEntry) {

                        double positionRatioEntry =
                                (currentPrice - rangeLowEntry) * 1.0
                                        / (rangeHighEntry - rangeLowEntry);

                        double positionScoreEntry;

                        if (positionRatioEntry <= 0.15) {

                            positionScoreEntry = 100.0;

                        } else if (positionRatioEntry >= 0.85) {

                            positionScoreEntry = 0.0;

                        } else {

                            positionScoreEntry =
                                    100.0 -
                                    ((positionRatioEntry - 0.15) / 0.70)
                                            * 100.0;
                        }

                        rangeScores[r] =
                                positionScoreEntry;

                        validWeightTotal +=
                                rangeWeights[r];

                        validRanges++;
                    }
                }

                score24h = rangeScores[0];
                score7d = rangeScores[1];
                score30d = rangeScores[2];

                // جایگاه قیمت در ۲۴ ساعت، ۷ روز و ۳۰ روز
                entryScore +=
                        score24h * 0.25;

                entryScore +=
                        score7d * 0.20;

                entryScore +=
                        score30d * 0.15;

                // حرکت ۵ دقیقه‌ای — ۱۵ امتیاز
                double rapidScoreEntry;

                if (rapidPercent <= -0.5) {

                    rapidScoreEntry = 80.0;

                } else if (rapidPercent < -0.10) {

                    rapidScoreEntry =
                            60.0 +
                            ((-rapidPercent - 0.10) / 0.40)
                                    * 20.0;

                } else if (rapidPercent <= 0.10) {

                    rapidScoreEntry = 60.0;

                } else if (rapidPercent < 0.5) {

                    rapidScoreEntry =
                            60.0 -
                            ((rapidPercent - 0.10) / 0.40)
                                    * 30.0;

                } else {

                    rapidScoreEntry = 30.0;
                }

                entryScore +=
                        rapidScoreEntry * 0.15;

                // فاصله از سقف — ۱۰ امتیاز
                double highDistanceScore;

                if (range <= 0) {

                    highDistanceScore = 50.0;

                } else {

                    double highDistanceRatio =
                            distanceFromHigh * 1.0 / range;

                    if (highDistanceRatio >= 0.50) {

                        highDistanceScore = 100.0;

                    } else if (highDistanceRatio <= 0.15) {

                        highDistanceScore = 0.0;

                    } else {

                        highDistanceScore =
                                ((highDistanceRatio - 0.15) / 0.35)
                                        * 100.0;
                    }
                }

                entryScore +=
                        highDistanceScore * 0.10;

                // روند کلی — ۱۰ امتیاز
                double trendScoreEntry;

                if (changePercent <= -1.0) {

                    trendScoreEntry = 80.0;

                } else if (changePercent < -0.10) {

                    trendScoreEntry =
                            60.0 +
                            ((-changePercent - 0.10) / 0.90)
                                    * 20.0;

                } else if (changePercent <= 0.10) {

                    trendScoreEntry = 60.0;

                } else if (changePercent < 1.0) {

                    trendScoreEntry =
                            60.0 -
                            ((changePercent - 0.10) / 0.90)
                                    * 30.0;

                } else {

                    trendScoreEntry = 30.0;
                }

                entryScore +=
                        trendScoreEntry * 0.10;

                // کیفیت داده — ۵ امتیاز
                double dataQualityScore;

                if (allHistory.length() >= 60) {

                    dataQualityScore = 100.0;

                } else if (allHistory.length() >= 30) {

                    dataQualityScore = 80.0;

                } else if (allHistory.length() >= 15) {

                    dataQualityScore = 60.0;

                } else if (allHistory.length() >= 5) {

                    dataQualityScore = 40.0;

                } else {

                    dataQualityScore = 20.0;
                }

                entryScore +=
                        dataQualityScore * 0.05;

                entryScore =
                        Math.max(
                                0.0,
                                Math.min(
                                        100.0,
                                        entryScore
                                )
                        );

                // محافظ سقف:
                // اگر قیمت در چند بازه نزدیک سقف باشد،
                // صرفاً روند مثبت باعث مساعد شدن ورود نشود.
                int nearHighRanges = 0;

                if (score24h <= 15.0) {
                    nearHighRanges++;
                }

                if (score7d <= 15.0) {
                    nearHighRanges++;
                }

                if (score30d <= 15.0) {
                    nearHighRanges++;
                }

                if (nearHighRanges >= 2) {

                    entryScore =
                            Math.min(
                                    entryScore,
                                    59.0
                            );
                }

                String purchaseAssessment;
                String purchaseReason;

                if (validRanges == 0) {

                    purchaseAssessment =
                            "💡 موقعیت ورود: خنثی";

                    purchaseReason =
                            "داده کافی برای مقایسه موقعیت قیمت در بازه‌های مختلف وجود ندارد.";

                } else if (entryScore >= 70.0) {

                    purchaseAssessment =
                            "💡 موقعیت ورود: مساعد";

                    purchaseReason =
                            "قیمت در بخش پایین‌تر محدوده‌های اخیر قرار دارد و شاخص‌های کوتاه‌مدت مانع مهمی نشان نمی‌دهند.";

                } else if (entryScore >= 45.0) {

                    purchaseAssessment =
                            "💡 موقعیت ورود: خنثی";

                    purchaseReason =
                            "قیمت در محدوده میانی قرار دارد و شاخص‌ها برتری مشخصی برای موقعیت ورود نشان نمی‌دهند.";

                } else {

                    purchaseAssessment =
                            "💡 موقعیت ورود: محتاطانه";

                    purchaseReason =
                            "قیمت نسبت به سقف‌های اخیر در موقعیت بالاتری قرار دارد یا حرکت کوتاه‌مدت ریسک ورود را بیشتر کرده است.";
                }

                smartAnalysisText.setText(
                        String.format(
                                Locale.US,
                                "%s   •   تغییر بازه: %+.2f%%\n" +
                                "%s   •   تغییر: %+.2f%%\n" +
                                "%s\n" +
                                "%s\n" +
                                "%s\n" +
                                "%s\n" +
                                "%s\n" +
                                "%s\n" +
                                "🕐 آخرین ثبت: %s",
                                trendText,
                                changePercent,
                                shortTermText,
                                rapidPercent,
                                positionText,
                                summaryText,
                                marketState,
                                scoreText,
                                purchaseAssessment,
                                purchaseReason,
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

        if (view instanceof TextView
                && !(view instanceof RadioButton
                && (view.getId() == R.id.digikalaRadio
                || view.getId() == R.id.milliRadio
                || view.getId() == R.id.tgjuRadio
                || view.getId() == R.id.snappGoldRadio))) {

            TextView textView = (TextView) view;

            if (!originalTextColors.containsKey(textView)) {
                originalTextColors.put(
                        textView,
                        textView.getCurrentTextColor()
                );
            }

            int currentColor =
                    textView.getCurrentTextColor();

            if (dark) {

                if (currentColor == Color.rgb(119, 119, 119)
                        || currentColor == Color.rgb(138, 128, 107)
                        || currentColor == Color.rgb(117, 105, 67)
                        || currentColor == Color.rgb(85, 85, 85)
                        || currentColor == Color.rgb(119, 119, 119)
                        || currentColor == Color.rgb(136, 136, 136)
                        || currentColor == Color.rgb(138, 138, 138)) {

                    textView.setTextColor(
                            secondaryText
                    );

                } else if (currentColor == Color.rgb(32, 32, 32)) {

                    textView.setTextColor(
                            primaryText
                    );

                } else if (currentColor == Color.rgb(53, 93, 40)
                        || currentColor == Color.rgb(79, 126, 58)) {

                    textView.setTextColor(
                            Color.rgb(170, 210, 145)
                    );

                } else {

                    textView.setTextColor(
                            primaryText
                    );
                }

            } else {

                Integer originalColor =
                        originalTextColors.get(textView);

                if (originalColor != null) {
                    textView.setTextColor(originalColor);
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
                if (button.getId() == R.id.saveButton) {
                    button.setTextColor(
                            Color.rgb(255, 255, 255)
                    );
                } else {
                    button.setTextColor(
                            Color.rgb(34, 34, 34)
                    );
                }
            }
        }

        if (view instanceof RadioButton) {

            RadioButton radioButton =
                    (RadioButton) view;

            if (dark) {
                int id = radioButton.getId();

                if (id == R.id.digikalaRadio
                        || id == R.id.milliRadio
                        || id == R.id.tgjuRadio
                        || id == R.id.snappGoldRadio) {

                    radioButton.setTypeface(
                            radioButton.getTypeface(),
                            android.graphics.Typeface.BOLD
                    );

                } else {
                    radioButton.setTextColor(primaryText);
                }
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
                || view.getId() == R.id.comparisonCard
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

        stopGoldPriceShimmer();

        super.onDestroy();
    }
}
