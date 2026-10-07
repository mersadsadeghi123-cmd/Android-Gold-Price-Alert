package com.mersad.gold18;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private android.content.SharedPreferences prefs;

    private Spinner refreshSpinner;
    private Spinner backgroundSpinner;
    private Switch backgroundSwitch;
    private Switch darkSwitch;

    private ScrollView settingsScroll;
    private LinearLayout settingsRoot;
    private TextView backgroundStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_settings
        );

        prefs =
                getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                );

        settingsScroll =
                findViewById(
                        R.id.settingsScroll
                );

        settingsRoot =
                findViewById(
                        R.id.settingsRoot
                );

        refreshSpinner =
                findViewById(
                        R.id.settingsRefreshSpinner
                );

        backgroundSpinner =
                findViewById(
                        R.id.settingsBackgroundSpinner
                );

        backgroundSwitch =
                findViewById(
                        R.id.settingsBackgroundSwitch
                );

        darkSwitch =
                findViewById(
                        R.id.settingsDarkSwitch
                );

        backgroundStatus =
                findViewById(
                        R.id.settingsBackgroundStatus
                );

        setupRefresh();

        setupBackground();

        setupDarkMode();

        applyColors(
                prefs.getBoolean(
                        "dark_mode",
                        false
                )
        );
    }

    private void setupRefresh() {

        String[] times = {
                "۱۵ ثانیه",
                "۳۰ ثانیه",
                "۶۰ ثانیه"
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
    }

    private void setupBackground() {

        String[] backgroundTimes = {
                "۱۵ ثانیه",
                "۳۰ ثانیه",
                "۶۰ ثانیه"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        backgroundTimes
                );

        backgroundSpinner.setAdapter(
                adapter
        );

        int savedIndex =
                prefs.getInt(
                        "background_interval_index",
                        1
                );

        if (savedIndex < 0 ||
                savedIndex >= backgroundTimes.length) {

            savedIndex = 1;
        }

        backgroundSpinner.setSelection(
                savedIndex
        );

        boolean enabled =
                prefs.getBoolean(
                        "background_enabled",
                        true
                );

        backgroundSwitch.setChecked(
                enabled
        );

        updateBackgroundStatus(
                enabled,
                savedIndex
        );

        backgroundSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    prefs.edit()
                            .putBoolean(
                                    "background_enabled",
                                    isChecked
                            )
                            .apply();

                    updateBackgroundStatus(
                            isChecked,
                            backgroundSpinner
                                    .getSelectedItemPosition()
                    );

                    Intent serviceIntent =
                            new Intent(
                                    this,
                                    GoldPriceService.class
                            );

                    if (isChecked) {

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

                    } else {

                        stopService(
                                serviceIntent
                        );
                    }
                }
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
                                15000L,
                                30000L,
                                60000L
                        };

                        prefs.edit()
                                .putInt(
                                        "background_interval_index",
                                        position
                                )
                                .putLong(
                                        "background_interval",
                                        intervals[position]
                                )
                                .apply();

                        updateBackgroundStatus(
                                backgroundSwitch
                                        .isChecked(),
                                position
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }

    private void updateBackgroundStatus(
            boolean enabled,
            int position) {

        String[] times = {
                "۱۵ ثانیه",
                "۳۰ ثانیه",
                "۶۰ ثانیه"
        };

        if (!enabled) {

            backgroundStatus.setText(
                    "⚪ رصد پس‌زمینه غیرفعال"
            );

            return;
        }

        backgroundStatus.setText(
                "🟢 رصد پس‌زمینه فعال — هر " +
                times[position]
        );
    }

    private void setupDarkMode() {

        boolean dark =
                prefs.getBoolean(
                        "dark_mode",
                        false
                );

        darkSwitch.setChecked(
                dark
        );

        darkSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    prefs.edit()
                            .putBoolean(
                                    "dark_mode",
                                    isChecked
                            )
                            .apply();

                    applyColors(
                            isChecked
                    );
                }
        );
    }

    private void applyColors(
            boolean dark) {

        int background =
                dark
                        ? 0xFF121212
                        : 0xFFF6F4EF;

        int text =
                dark
                        ? 0xFFFFFFFF
                        : 0xFF303030;

        int secondary =
                dark
                        ? 0xFFBDBDBD
                        : 0xFF777777;

        settingsScroll.setBackgroundColor(
                background
        );

        settingsRoot.setBackgroundColor(
                background
        );

        applyTextColors(
                settingsRoot,
                text,
                secondary,
                dark
        );
    }

    private void applyTextColors(
            android.view.View view,
            int textColor,
            int secondaryColor,
            boolean dark) {

        if (view instanceof TextView) {

            TextView textView =
                    (TextView) view;

            int viewId = view.getId();

            if (viewId == R.id.settingsVersionText ||
                    viewId == R.id.settingsBackgroundStatus) {

                textView.setTextColor(
                        secondaryColor
                );

            } else {

                textView.setTextColor(
                        textColor
                );
            }
        }

        if (view instanceof LinearLayout) {

            LinearLayout layout =
                    (LinearLayout) view;

            if (dark && view.getId() != R.id.settingsRoot) {
                layout.setBackgroundColor(
                        0xFF202020
                );
            }

            for (int i = 0;
                 i < layout.getChildCount();
                 i++) {

                applyTextColors(
                        layout.getChildAt(i),
                        textColor,
                        secondaryColor,
                        dark
                );
            }
        }
    }
}
