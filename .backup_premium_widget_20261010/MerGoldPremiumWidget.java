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
            int height = 100;
            Bitmap bitmap = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);

            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for (int i = 0; i < history.length(); i++) {
                long value = history.getJSONObject(i).getLong("price");
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            min = Math.min(min, currentPrice);
            max = Math.max(max, currentPrice);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0xFFE5C66B);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            if (max <= min) {
                canvas.drawLine(8, height / 2f, width - 8, height / 2f, paint);
                return bitmap;
            }

            Path path = new Path();
            float left = 8f;
            float right = width - 8f;
            float top = 8f;
            float bottom = height - 8f;
            int count = history.length();

            for (int i = 0; i < count; i++) {
                long value = history.getJSONObject(i).getLong("price");
                float x = left + (float) i / (count - 1) * (right - left);
                float y = bottom
                        - (float) (value - min) / (float) (max - min)
                        * (bottom - top);

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            canvas.drawPath(path, paint);

            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(0xFFFFE8A0);
            dot.setStyle(Paint.Style.FILL);
            canvas.drawCircle(right, bottom
                    - (float) (history.getJSONObject(count - 1).getLong("price") - min)
                    / (float) (max - min) * (bottom - top), 7f, dot);

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
