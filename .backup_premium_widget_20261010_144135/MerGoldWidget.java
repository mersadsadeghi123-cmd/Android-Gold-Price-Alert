package com.mersad.gold18;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class MerGoldWidget extends AppWidgetProvider {

    public static void updateWidgets(Context context) {

        AppWidgetManager manager =
                AppWidgetManager.getInstance(context);

        ComponentName component =
                new ComponentName(
                        context,
                        MerGoldWidget.class
                );

        int[] ids =
                manager.getAppWidgetIds(component);

        for (int id : ids) {
            updateWidget(
                    context,
                    manager,
                    id
            );
        }
    }

    private static void updateWidget(
            Context context,
            AppWidgetManager manager,
            int widgetId) {

        RemoteViews views =
                new RemoteViews(
                        context.getPackageName(),
                        R.layout.mergold_widget
                );

        SharedPreferences prefs =
                context.getSharedPreferences(
                        "gold_settings",
                        Context.MODE_PRIVATE
                );

        long price =
                prefs.getLong(
                        "widget_price",
                        0
                );

        long change =
                prefs.getLong(
                        "widget_change",
                        0
                );

        String source =
                prefs.getString(
                        "widget_source",
                        "دیجی‌کالا"
                );

        String sourceKey =
                prefs.getString(
                        "price_source",
                        "digikala"
                );

        String highTarget =
                prefs.getString(
                        "high_target_" + sourceKey,
                        ""
                );

        String lowTarget =
                prefs.getString(
                        "low_target_" + sourceKey,
                        ""
                );

        long highest = price;
        long lowest = price;
        long previous = price;

        try {

            JSONArray history =
                    PriceHistory.get(
                            context,
                            sourceKey
                    );

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

                if (i == history.length() - 2) {
                    previous = value;
                }
            }

            if (history.length() == 1) {
                previous =
                        history.getJSONObject(0)
                                .getLong("price");
            }

        } catch (Exception ignored) {
        }

        double percentChange = 0;

        if (previous > 0) {
            percentChange =
                    ((double) (price - previous)
                            / previous) * 100.0;
        }

        long lastUpdate =
                prefs.getLong(
                        "widget_last_update",
                        0
                );

        boolean online =
                prefs.getBoolean(
                        "widget_online",
                        false
                );

        if (price > 0) {

            views.setImageViewBitmap(
                    R.id.widgetPrice,
                    createWidgetPriceBitmap(
                            price,
                            getWidgetShimmerPhase(prefs)
                    )
            );

            String changeText;

            if (change > 0) {

                changeText =
                        String.format(
                                Locale.US,
                                "▲ +%,d تومان",
                                change
                        );

            } else if (change < 0) {

                changeText =
                        String.format(
                                Locale.US,
                                "▼ %,d تومان",
                                change
                        );

            } else {

                changeText =
                        "— بدون تغییر";
            }

            views.setTextViewText(
                    R.id.widgetChange,
                    changeText
            );

        } else {

            views.setImageViewBitmap(
                    R.id.widgetPrice,
                    createWidgetPriceBitmap(
                            0,
                            getWidgetShimmerPhase(prefs)
                    )
            );

            views.setTextViewText(
                    R.id.widgetChange,
                    "—"
            );
        }

        views.setTextViewText(
                R.id.widgetSource,
                "منبع: " + source
        );

        String percentText;

        if (percentChange > 0) {

            percentText =
                    String.format(
                            Locale.US,
                            "▲ +%.2f%%",
                            percentChange
                    );

        } else if (percentChange < 0) {

            percentText =
                    String.format(
                            Locale.US,
                            "▼ %.2f%%",
                            percentChange
                    );

        } else {

            percentText =
                    "— 0.00%";
        }

        views.setTextViewText(
                R.id.widgetChange,
                change > 0 || change < 0
                        ? viewsToChangeText(change, percentText)
                        : percentText
        );

        views.setTextViewText(
                R.id.widgetHigh,
                formatWidgetPrice(highest)
        );
        Bitmap widgetChartBitmap =
                createWidgetChart(
                        context,
                        sourceKey,
                        price
                );

        if (widgetChartBitmap != null) {
            views.setImageViewBitmap(
                    R.id.widgetChart,
                    widgetChartBitmap
            );
        }


        views.setTextViewText(
                R.id.widgetLow,
                formatWidgetPrice(lowest)
        );

        views.setTextViewText(
                R.id.widgetHighTarget,
                highTarget.isEmpty()
                        ? "🔺 هدف بالا: —"
                        : "🔺 هدف بالا: " + highTarget
        );

        views.setTextViewText(
                R.id.widgetLowTarget,
                lowTarget.isEmpty()
                        ? "🔻 هدف پایین: —"
                        : "🔻 هدف پایین: " + lowTarget
        );

        views.setTextViewText(
                R.id.widgetStatus,
                online
                        ? "● آنلاین"
                        : "● آخرین داده"
        );

        String updateText =
                lastUpdate > 0
                        ? "آخرین بروزرسانی: " +
                          android.text.format.DateFormat.format(
                                  "HH:mm",
                                  lastUpdate
                          )
                        : "آخرین بروزرسانی: —";

        views.setTextViewText(
                R.id.widgetLastUpdate,
                updateText
        );

        Intent intent =
                new Intent(
                        context,
                        MainActivity.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        views.setOnClickPendingIntent(
                R.id.widgetPrice,
                pendingIntent
        );

        Intent refreshIntent =
                new Intent(
                        context,
                        GoldPriceService.class
                );

        refreshIntent.setAction(
                "com.mersad.gold18.WIDGET_REFRESH"
        );

        PendingIntent refreshPendingIntent =
                PendingIntent.getService(
                        context,
                        widgetId + 10000,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        views.setOnClickPendingIntent(
                R.id.widgetRefresh,
                refreshPendingIntent
        );

        manager.updateAppWidget(
                widgetId,
                views
        );
    }

    private static int getWidgetShimmerPhase(
            SharedPreferences prefs) {

        int phase =
                prefs.getInt(
                        "widget_shimmer_phase",
                        0
                );

        phase += 135;

        if (phase > 600) {
            phase = -300;
        }

        prefs.edit()
                .putInt(
                        "widget_shimmer_phase",
                        phase
                )
                .apply();

        return phase;
    }

    private static Bitmap createWidgetPriceBitmap(
            long price,
            int phase) {

        int width = 700;
        int height = 90;

        Bitmap bitmap =
                Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                );

        Canvas canvas =
                new Canvas(bitmap);

        Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        paint.setTextSize(72f);
        paint.setTextAlign(Paint.Align.CENTER);

        String text =
                price > 0
                        ? String.format(
                                Locale.US,
                                "%,d تومان",
                                price
                        )
                        : "در حال دریافت...";

        float centerX =
                width / 2f;

        Paint.FontMetrics metrics =
                paint.getFontMetrics();

        float centerY =
                height / 2f -
                (metrics.ascent + metrics.descent) / 2f;

        if (price > 0) {

            LinearGradient gradient =
                    new LinearGradient(
                            phase - 260f,
                            0,
                            phase + 260f,
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

            paint.setShader(gradient);

        } else {

            paint.setColor(0xFF806000);
        }

        canvas.drawText(
                text,
                centerX,
                centerY,
                paint
        );

        return bitmap;
    }

    private static Bitmap createWidgetChart(
            Context context,
            String sourceKey,
            long currentPrice) {

        try {
            JSONArray history =
                    PriceHistory.get(
                            context,
                            sourceKey
                    );

            if (history.length() < 2) {
                return null;
            }

            int width = 600;
            int height = 124;

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                    );

            Canvas canvas = new Canvas(bitmap);

            Paint paint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(0xFFB88A16);

            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for (int i = 0;
                 i < history.length();
                 i++) {

                long value =
                        history.getJSONObject(i)
                                .getLong("price");

                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            min = Math.min(min, currentPrice);
            max = Math.max(max, currentPrice);

            if (max <= min) {
                return null;
            }
            float left = 8f;
            float right = width - 8f;
            float top = 10f;
            float bottom = height - 10f;

            Path path = new Path();

            int count = history.length();

            for (int i = 0;
                 i < count;
                 i++) {

                long value =
                        history.getJSONObject(i)
                                .getLong("price");

                float x =
                        left +
                        ((float) i / (count - 1)) *
                        (right - left);

                float y =
                        bottom -
                        ((float) (value - min) /
                        (float) (max - min)) *
                        (bottom - top);

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            canvas.drawPath(
                    path,
                    paint
            );

            return bitmap;

        } catch (Exception ignored) {
            return null;
        }
    }
    private static CharSequence viewsToChangeText(
            long change,
            String percentText) {

        String amountText;

        if (change > 0) {

            amountText =
                    String.format(
                            Locale.US,
                            "▲ +%,d تومان",
                            change
                    );

        } else if (change < 0) {

            amountText =
                    String.format(
                            Locale.US,
                            "▼ %,d تومان",
                            change
                    );

        } else {

            amountText =
                    "— بدون تغییر";
        }

        String fullText =
                amountText + "  " + percentText;

        SpannableString result =
                new SpannableString(fullText);

        int color;

        if (change > 0) {
            color = 0xFF2E8B57;
        } else if (change < 0) {
            color = 0xFFC62828;
        } else {
            color = 0xFF806000;
        }

        result.setSpan(
                new ForegroundColorSpan(color),
                0,
                result.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return result;
    }

    private static String formatWidgetPrice(long price) {

        if (price <= 0) {
            return "—";
        }

        return String.format(
                Locale.US,
                "%,d",
                price
        );
    }

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] appWidgetIds) {

        for (int id : appWidgetIds) {

            updateWidget(
                    context,
                    manager,
                    id
            );
        }
    }

    @Override
    public void onEnabled(Context context) {
        updateWidgets(context);
    }

    @Override
    public void onDisabled(Context context) {
    }
}
