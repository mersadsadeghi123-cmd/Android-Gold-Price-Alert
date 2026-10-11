package com.mersad.gold18;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.graphics.Typeface;
import android.widget.RemoteViews;

import org.json.JSONArray;

import java.util.Locale;

public class MerGoldPremiumWidget extends AppWidgetProvider {

    public static void updateWidgets(Context context) {
        AppWidgetManager manager =
                AppWidgetManager.getInstance(context);

        ComponentName component =
                new ComponentName(context, MerGoldPremiumWidget.class);

        int[] ids = manager.getAppWidgetIds(component);

        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static void updateWidget(
            Context context,
            AppWidgetManager manager,
            int widgetId) {

        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.mergold_premium_widget
        );

        SharedPreferences prefs = context.getSharedPreferences(
                "gold_settings",
                Context.MODE_PRIVATE
        );

        long price = prefs.getLong("widget_price", 0);
        long change = prefs.getLong("widget_change", 0);
        long lastUpdate = prefs.getLong("widget_last_update", 0);
        boolean online = prefs.getBoolean("widget_online", false);

        String source = prefs.getString("widget_source", "دیجی‌کالا");
        String sourceKey = prefs.getString("price_source", "digikala");

        String priceText = price > 0
                ? String.format(Locale.US, "%,d تومان", price)
                : "در حال دریافت...";

        views.setTextViewText(R.id.premiumWidgetPrice, priceText);
        views.setTextViewText(
                R.id.premiumWidgetSource,
                "منبع: " + source
        );

        views.setTextViewText(
                R.id.premiumWidgetStatus,
                online ? "● آنلاین" : "● آخرین داده"
        );
        views.setTextColor(
                R.id.premiumWidgetStatus,
                online ? 0xFF75D6A0 : 0xFFE5C66B
        );

        JSONArray history = PriceHistory.get(context, sourceKey);
        long previous = price;

        if (history != null && history.length() >= 2) {
            try {
                previous = history.getJSONObject(history.length() - 2)
                        .getLong("price");
            } catch (Exception ignored) {
            }
        }

        double percent = previous > 0
                ? ((double) (price - previous) / previous) * 100.0
                : 0.0;

        String changeText;
        int changeColor;

        if (change > 0) {
            changeText = String.format(
                    Locale.US, "▲ +%,d تومان  |  +%.2f%%",
                    change, percent
            );
            changeColor = 0xFF75D6A0;
        } else if (change < 0) {
            changeText = String.format(
                    Locale.US, "▼ %,d تومان  |  %.2f%%",
                    change, percent
            );
            changeColor = 0xFFFF8585;
        } else {
            changeText = String.format(
                    Locale.US, "— بدون تغییر  |  %.2f%%", percent
            );
            changeColor = 0xFFE5C66B;
        }

        views.setTextViewText(
                R.id.premiumWidgetChange,
                price > 0 ? changeText : "—"
        );
        views.setTextColor(
                R.id.premiumWidgetChange,
                changeColor
        );

        String updateText = lastUpdate > 0
                ? android.text.format.DateFormat.format(
                        "HH:mm", lastUpdate
                ).toString()
                : "—";

        views.setTextViewText(
                R.id.premiumWidgetLastUpdate,
                updateText
        );

        Bitmap chart = createChart(context, sourceKey, price);
        if (chart != null) {
            views.setImageViewBitmap(
                    R.id.premiumWidgetChart,
                    chart
            );
        } else {
            views.setImageViewBitmap(
                    R.id.premiumWidgetChart,
                    Bitmap.createBitmap(
                            1, 1, Bitmap.Config.ARGB_8888
                    )
            );
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
                context,
                20000 + widgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(
                R.id.premiumWidgetPrice,
                openPending
        );

        Intent refreshIntent = new Intent(context, GoldPriceService.class);
        refreshIntent.setAction("com.mersad.gold18.WIDGET_REFRESH");

        PendingIntent refreshPending = PendingIntent.getService(
                context,
                30000 + widgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(
                R.id.premiumWidgetRefresh,
                refreshPending
        );

        manager.updateAppWidget(widgetId, views);
    }

    private static Bitmap createChart(
            Context context,
            String sourceKey,
            long currentPrice) {
        try {
            JSONArray history = PriceHistory.get(context, sourceKey);
            if (history == null || history.length() < 2) {
                return null;
            }

            int width = 600;
            int height = 150;
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

            float left = 12f;
            float right = width - 16f;
            float top = 18f;
            float bottom = height - 18f;
            float plotHeight = bottom - top;
            float plotWidth = right - left;

            Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
            grid.setColor(0x263F4148);
            grid.setStrokeWidth(1.5f);
            for (int i = 0; i < 4; i++) {
                float y = top + plotHeight * i / 3f;
                canvas.drawLine(left, y, right, y, grid);
            }

            int historyCount = history.length();
            int count = historyCount + 1;
            Path line = new Path();
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

            line.moveTo(xs[0], ys[0]);
            for (int i = 1; i < count; i++) {
                float midX = (xs[i - 1] + xs[i]) / 2f;
                float midY = (ys[i - 1] + ys[i]) / 2f;
                line.quadTo(xs[i - 1], ys[i - 1], midX, midY);
            }
            line.lineTo(xs[count - 1], ys[count - 1]);

            Path area = new Path(line);
            area.lineTo(right, bottom);
            area.lineTo(left, bottom);
            area.close();

            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(new LinearGradient(
                    0, top, 0, bottom,
                    0x66E5C66B, 0x00E5C66B,
                    Shader.TileMode.CLAMP
            ));
            canvas.drawPath(area, fill);

            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(10f);
            glow.setStrokeCap(Paint.Cap.ROUND);
            glow.setStrokeJoin(Paint.Join.ROUND);
            glow.setColor(0x55E5C66B);
            glow.setShadowLayer(9f, 0f, 0f, 0xAAE5C66B);
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
            halo.setColor(0x44FFE8A0);
            canvas.drawCircle(lastX, lastY, 12f, halo);

            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(0xFFFFE8A0);
            dot.setStyle(Paint.Style.FILL);
            canvas.drawCircle(lastX, lastY, 5f, dot);

            Paint center = new Paint(Paint.ANTI_ALIAS_FLAG);
            center.setColor(0xFFE7F3F8);
            canvas.drawCircle(lastX, lastY, 2f, center);

            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] ids) {
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    @Override
    public void onEnabled(Context context) {
        updateWidgets(context);
    }
}
