package com.mersad.gold18;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PriceChartView extends View {

    private JSONArray data = new JSONArray();

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path = new Path();
    private final Path fillPath = new Path();

    private final int GOLD = 0xFFD4AF37;
    private final int GOLD_DARK = 0xFFB38B16;
    private final int GRID = 0xFFEAEAEA;
    private final int TEXT = 0xFF858585;

    public PriceChartView(Context context) {
        super(context);
        init();
    }

    public PriceChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PriceChartView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4.5f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(GOLD);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(GRID);

        textPaint.setColor(TEXT);
        textPaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                )
        );

        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(GOLD);

        pointRingPaint.setStyle(Paint.Style.STROKE);
        pointRingPaint.setStrokeWidth(3f);
        pointRingPaint.setColor(GOLD_DARK);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0x18D4AF37);
    }

    public void setData(JSONArray newData) {

        if (newData == null) {
            data = new JSONArray();
        } else {
            data = newData;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.drawColor(0xFFFFFFFF);

        if (data.length() == 0) {

            textPaint.setTextSize(14f);
            textPaint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.NORMAL
                    )
            );

            String message =
                    "هنوز داده‌ای برای نمایش وجود ندارد";

            float messageWidth =
                    textPaint.measureText(message);

            canvas.drawText(
                    message,
                    (width - messageWidth) / 2f,
                    height / 2f,
                    textPaint
            );

            return;
        }

        /*
         * فضای نمودار
         *
         * سمت چپ برای قیمت‌ها
         * سمت راست برای نقطه آخر
         */
        float left = 72f;
        float right = width - 18f;
        float top = 18f;
        float bottom = height - 42f;

        float chartWidth = right - left;
        float chartHeight = bottom - top;

        if (chartWidth <= 0 || chartHeight <= 0) {
            return;
        }

        long minPrice = Long.MAX_VALUE;
        long maxPrice = Long.MIN_VALUE;

        for (int i = 0; i < data.length(); i++) {

            try {

                JSONObject item =
                        data.getJSONObject(i);

                long price =
                        item.getLong("price");

                if (price < minPrice) {
                    minPrice = price;
                }

                if (price > maxPrice) {
                    maxPrice = price;
                }

            } catch (Exception ignored) {
            }
        }

        if (minPrice == Long.MAX_VALUE) {
            return;
        }

        if (minPrice == maxPrice) {

            long padding =
                    Math.max(1000L, minPrice / 1000L);

            minPrice -= padding;
            maxPrice += padding;
        }

        long range =
                maxPrice - minPrice;

        long padding =
                Math.max(
                        1L,
                        range / 10L
                );

        minPrice -= padding;
        maxPrice += padding;

        // خطوط افقی
        textPaint.setTextSize(10f);
        textPaint.setColor(TEXT);
        textPaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                )
        );

        for (int i = 0; i <= 4; i++) {

            float y =
                    top +
                    chartHeight * i / 4f;

            canvas.drawLine(
                    left,
                    y,
                    right,
                    y,
                    gridPaint
            );

            long value =
                    maxPrice -
                    ((maxPrice - minPrice) * i / 4L);

            String label =
                    formatCompactPrice(value);

            canvas.drawText(
                    label,
                    4f,
                    y + 4f,
                    textPaint
            );
        }

        path.reset();
        fillPath.reset();

        float lastX = left;
        float lastY = bottom;

        int count = data.length();

        for (int i = 0; i < count; i++) {

            try {

                JSONObject item =
                        data.getJSONObject(i);

                long price =
                        item.getLong("price");

                float x;

                if (count == 1) {

                    x =
                            left +
                            chartWidth / 2f;

                } else {

                    x =
                            left +
                            chartWidth *
                            i /
                            (count - 1f);
                }

                float normalized =
                        (float)
                                (price - minPrice)
                        /
                        (float)
                                (maxPrice - minPrice);

                float y =
                        bottom -
                        normalized * chartHeight;

                if (i == 0) {

                    path.moveTo(x, y);

                    fillPath.moveTo(
                            x,
                            bottom
                    );

                    fillPath.lineTo(
                            x,
                            y
                    );

                } else {

                    path.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }

                lastX = x;
                lastY = y;

            } catch (Exception ignored) {
            }
        }

        // تکمیل محدوده زیر خط
        fillPath.lineTo(
                lastX,
                bottom
        );

        fillPath.close();

        canvas.drawPath(
                fillPath,
                fillPaint
        );

        canvas.drawPath(
                path,
                linePaint
        );

        // حلقه اطراف نقطه آخر
        canvas.drawCircle(
                lastX,
                lastY,
                7f,
                pointRingPaint
        );

        canvas.drawCircle(
                lastX,
                lastY,
                4f,
                pointPaint
        );

        // قیمت آخر
        try {

            JSONObject last =
                    data.getJSONObject(
                            data.length() - 1
                    );

            long lastPrice =
                    last.getLong("price");

            String priceText =
                    formatPrice(lastPrice);

            textPaint.setColor(GOLD_DARK);
            textPaint.setTextSize(11f);
            textPaint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            float priceWidth =
                    textPaint.measureText(
                            priceText
                    );

            float priceX =
                    lastX - priceWidth - 8f;

            if (priceX < left) {
                priceX = left;
            }

            float priceY =
                    lastY - 12f;

            if (priceY < 12f) {
                priceY = lastY + 18f;
            }

            canvas.drawText(
                    priceText,
                    priceX,
                    priceY,
                    textPaint
            );

        } catch (Exception ignored) {
        }

        // زمان شروع
        try {

            JSONObject first =
                    data.getJSONObject(0);

            long firstTime =
                    first.getLong("time");

            String firstTimeText =
                    formatTime(firstTime);

            textPaint.setColor(TEXT);
            textPaint.setTextSize(10f);
            textPaint.setTypeface(
                    Typeface.DEFAULT
            );

            canvas.drawText(
                    firstTimeText,
                    left,
                    height - 14f,
                    textPaint
            );

        } catch (Exception ignored) {
        }

        // زمان آخر
        try {

            JSONObject last =
                    data.getJSONObject(
                            data.length() - 1
                    );

            long lastTime =
                    last.getLong("time");

            String lastTimeText =
                    formatTime(lastTime);

            textPaint.setColor(TEXT);
            textPaint.setTextSize(10f);

            float timeWidth =
                    textPaint.measureText(
                            lastTimeText
                    );

            canvas.drawText(
                    lastTimeText,
                    right - timeWidth,
                    height - 14f,
                    textPaint
            );

        } catch (Exception ignored) {
        }
    }

    private String formatPrice(long value) {

        return String.format(
                Locale.US,
                "%,d",
                value
        );
    }

    private String formatCompactPrice(long value) {

        if (value >= 1000000L) {

            return String.format(
                    Locale.US,
                    "%.1fM",
                    value / 1000000.0
            );
        }

        if (value >= 1000L) {

            return String.format(
                    Locale.US,
                    "%.0fK",
                    value / 1000.0
            );
        }

        return String.format(
                Locale.US,
                "%d",
                value
        );
    }

    private String formatTime(long time) {

        return new SimpleDateFormat(
                "HH:mm",
                Locale.US
        ).format(
                new Date(time)
        );
    }
}
