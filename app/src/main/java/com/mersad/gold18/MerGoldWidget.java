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
                                    0xFFC49A00,
                                    0xFFD4AF37,
                                    0xFFFFF1A8,
                                    0xFFD4AF37,
                                    0xFFC49A00
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

            paint.setColor(0xFFC49A00);
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
            JSONArray history = PriceHistory.get(context, sourceKey);

            if (history == null || history.length() < 2) {
                return null;
            }

            int width = 600;
            int height = 124;

            Bitmap bitmap = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);

            long min = currentPrice;
            long max = currentPrice;

            for (int i = 0; i < history.length(); i++) {
                long value = history.getJSONObject(i).getLong("price");
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            if (max == min) {
                long padding = Math.max(1L, Math.abs(max) / 10000L);
                min -= padding;
                max += padding;
            }

            float left = 10f;
            float right = width - 12f;
            float top = 14f;
            float bottom = height - 14f;
            float plotHeight = bottom - top;
            float plotWidth = right - left;

            Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
            grid.setColor(0x2677838C);
            grid.setStrokeWidth(1.5f);

            for (int i = 0; i < 4; i++) {
                float y = top + plotHeight * i / 3f;
                canvas.drawLine(left, y, right, y, grid);
            }

            int historyCount = history.length();
            int count = historyCount + 1;
            float[] xs = new float[count];
            float[] ys = new float[count];

            for (int i = 0; i < count; i++) {
                long value = i == historyCount
                        ? currentPrice
                        : history.getJSONObject(i).getLong("price");

                xs[i] = left + (float) i / (count - 1) * plotWidth;
                ys[i] = bottom
                        - ((float) (value - min) / (float) (max - min))
                        * plotHeight;
            }

            Path line = new Path();
            line.moveTo(xs[0], ys[0]);

            for (int i = 1; i < count; i++) {
                float midX = (xs[i - 1] + xs[i]) / 2f;
                float midY = (ys[i - 1] + ys[i]) / 2f;
                line.quadTo(xs[i - 1], ys[i - 1], midX, midY);
            }

            line.lineTo(xs[count - 1], ys[count - 1]);

            Path area = new Path(line);
            area.lineTo(xs[count - 1], bottom);
            area.lineTo(xs[0], bottom);
            area.close();

            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(new LinearGradient(
                    0, top, 0, bottom,
                    0x55E5B94B, 0x00E5B94B,
                    Shader.TileMode.CLAMP
            ));
            canvas.drawPath(area, fill);

            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(9f);
            glow.setStrokeCap(Paint.Cap.ROUND);
            glow.setStrokeJoin(Paint.Join.ROUND);
            glow.setColor(0x44E5B94B);
            glow.setShadowLayer(7f, 0f, 0f, 0x88E5B94B);
            canvas.drawPath(line, glow);

            Paint goldLine = new Paint(Paint.ANTI_ALIAS_FLAG);
            goldLine.setStyle(Paint.Style.STROKE);
            goldLine.setStrokeWidth(4f);
            goldLine.setStrokeCap(Paint.Cap.ROUND);
            goldLine.setStrokeJoin(Paint.Join.ROUND);
            goldLine.setShader(new LinearGradient(
                    left, top, right, bottom,
                    0xFFFFE8A0, 0xFFE5B94B,
                    Shader.TileMode.CLAMP
            ));
            canvas.drawPath(line, goldLine);

            float lastX = xs[count - 1];
            float lastY = ys[count - 1];

            Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
            halo.setColor(0x44E5B94B);
            canvas.drawCircle(lastX, lastY, 10f, halo);

            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(0xFFE5B94B);
            dot.setStyle(Paint.Style.FILL);
            canvas.drawCircle(lastX, lastY, 4.5f, dot);

            Paint center = new Paint(Paint.ANTI_ALIAS_FLAG);
            center.setColor(0xFFFFFFFF);
            canvas.drawCircle(lastX, lastY, 2f, center);

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
            color = 0xFF69F0AE;
        } else if (change < 0) {
            color = 0xFFFF8A80;
        } else {
            color = 0xFFC49A00;
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
