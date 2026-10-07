package com.mersad.gold18;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private int selectedIndex = -1;
    private float touchX = -1f;

    private final int GOLD = 0xFFD4AF37;
    private final int GOLD_DARK = 0xFFB38B16;
    private final int GRID = 0xFFEAEAEA;
    private final int TEXT = 0xFF858585;

    private boolean darkMode = false;

    private boolean candlestickMode = false;

    private static class Candle {
        long startTime;
        long open;
        long high;
        long low;
        long close;

        Candle(long startTime, long price) {
            this.startTime = startTime;
            this.open = price;
            this.high = price;
            this.low = price;
            this.close = price;
        }

        void add(long price) {
            if (price > high) high = price;
            if (price < low) low = price;
            close = price;
        }
    }

    public void setChartType(boolean candlestick) {
        candlestickMode = candlestick;
        selectedIndex = -1;
        touchX = -1f;
        invalidate();
    }

    private int chartTextColor() {
        return darkMode
                ? 0xFFE0E0E0
                : TEXT;
    }

    private int chartGridColor() {
        return darkMode
                ? 0xFF4A4A4A
                : GRID;
    }

    private int chartGoldDarkColor() {
        return darkMode
                ? 0xFFFFD966
                : GOLD_DARK;
    }

    private int chartBoxColor() {
        return darkMode
                ? 0xFF292929
                : 0xFFFDF8E8;
    }

    public void setDarkMode(boolean dark) {

        darkMode = dark;

        gridPaint.setColor(
                chartGridColor()
        );

        textPaint.setColor(
                chartTextColor()
        );

        pointRingPaint.setColor(
                chartGoldDarkColor()
        );

        invalidate();
    }

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
        gridPaint.setColor(chartGridColor());

        textPaint.setColor(chartTextColor());
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
    public boolean onTouchEvent(MotionEvent event) {

        if (data == null || data.length() == 0) {
            return true;
        }

        if (candlestickMode) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                touchX = event.getX();
                List<Candle> candles = buildCandles();
                float left = 72f;
                float right = getWidth() - 18f;
                float slot = (right - left) / Math.max(1, candles.size());
                int index = Math.round(((touchX - left) / slot) - 0.5f);
                selectedIndex = Math.max(0, Math.min(candles.size() - 1, index));
                invalidate();
                return true;
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {

            touchX = event.getX();

            float left = 72f;
            float right = getWidth() - 18f;

            float chartWidth = right - left;

            if (chartWidth <= 0) {
                return true;
            }

            float position =
                    (touchX - left) / chartWidth;

            position =
                    Math.max(0f, Math.min(1f, position));

            int count = data.length();

            if (count == 1) {
                selectedIndex = 0;
            } else {
                selectedIndex =
                        Math.round(
                                position * (count - 1)
                        );

                selectedIndex =
                        Math.max(
                                0,
                                Math.min(
                                        count - 1,
                                        selectedIndex
                                )
                        );
            }

            invalidate();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            invalidate();
            return true;
        }

        return true;
    }


    private List<Candle> buildCandles() {

        List<Candle> candles = new ArrayList<>();

        // هر کندل = ۵ دقیقه
        final long candleSize = 5L * 60L * 1000L;

        for (int i = 0; i < data.length(); i++) {

            try {
                JSONObject item = data.getJSONObject(i);

                long time = item.getLong("time");
                long price = item.getLong("price");

                long bucket =
                        (time / candleSize) * candleSize;

                if (candles.isEmpty()
                        || candles.get(candles.size() - 1).startTime != bucket) {

                    candles.add(
                            new Candle(
                                    bucket,
                                    price
                            )
                    );

                } else {

                    candles.get(
                            candles.size() - 1
                    ).add(price);
                }

            } catch (Exception ignored) {
            }
        }

        return candles;
    }

    private void drawCandlestickChart(
            Canvas canvas,
            float width,
            float height) {

        float left = 72f;
        float right = width - 18f;
        float top = 18f;
        float bottom = height - 42f;

        float chartWidth = right - left;
        float chartHeight = bottom - top;

        if (chartWidth <= 0 || chartHeight <= 0) {
            return;
        }

        List<Candle> candles = buildCandles();

        if (candles.isEmpty()) {
            return;
        }

        long minPrice = Long.MAX_VALUE;
        long maxPrice = Long.MIN_VALUE;

        for (Candle candle : candles) {

            minPrice =
                    Math.min(
                            minPrice,
                            candle.low
                    );

            maxPrice =
                    Math.max(
                            maxPrice,
                            candle.high
                    );
        }

        if (minPrice == maxPrice) {
            minPrice -= 1;
            maxPrice += 1;
        }

        textPaint.setTextSize(24f);
        textPaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                )
        );
        textPaint.setColor(
                chartTextColor()
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

            canvas.drawText(
                    formatCompactPrice(value),
                    4f,
                    y + 4f,
                    textPaint
            );
        }

        float candleSlot =
                chartWidth / Math.max(1, candles.size());

        float bodyWidth =
                Math.max(
                        5f,
                        Math.min(
                                18f,
                                candleSlot * 0.58f
                        )
                );

        Paint wickPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        wickPaint.setStrokeWidth(2.2f);

        Paint bodyPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        bodyPaint.setStyle(
                Paint.Style.FILL
        );

        for (int i = 0; i < candles.size(); i++) {

            Candle candle =
                    candles.get(i);

            float x =
                    left +
                    candleSlot * i +
                    candleSlot / 2f;

            float highY =
                    bottom -
                    ((float)(candle.high - minPrice)
                            / (float)(maxPrice - minPrice))
                            * chartHeight;

            float lowY =
                    bottom -
                    ((float)(candle.low - minPrice)
                            / (float)(maxPrice - minPrice))
                            * chartHeight;

            float openY =
                    bottom -
                    ((float)(candle.open - minPrice)
                            / (float)(maxPrice - minPrice))
                            * chartHeight;

            float closeY =
                    bottom -
                    ((float)(candle.close - minPrice)
                            / (float)(maxPrice - minPrice))
                            * chartHeight;

            boolean rising =
                    candle.close >= candle.open;

            int color;

            if (rising) {
                color =
                        darkMode
                                ? 0xFF63C174
                                : 0xFF3FAE5A;
            } else {
                color =
                        darkMode
                                ? 0xFFFF6B6B
                                : 0xFFE05252;
            }

            wickPaint.setColor(color);
            bodyPaint.setColor(color);

            canvas.drawLine(
                    x,
                    highY,
                    x,
                    lowY,
                    wickPaint
            );

            float bodyTop =
                    Math.min(
                            openY,
                            closeY
                    );

            float bodyBottom =
                    Math.max(
                            openY,
                            closeY
                    );

            if (Math.abs(bodyBottom - bodyTop) < 3f) {
                bodyBottom =
                        bodyTop + 3f;
            }

            canvas.drawRect(
                    x - bodyWidth / 2f,
                    bodyTop,
                    x + bodyWidth / 2f,
                    bodyBottom,
                    bodyPaint
            );
        }

        if (selectedIndex >= 0 && selectedIndex < candles.size()) {
            Candle c = candles.get(selectedIndex);

            float slot =
                    chartWidth / Math.max(1, candles.size());

            float x =
                    left +
                    slot * selectedIndex +
                    slot / 2f;

            float closeY =
                    bottom -
                    ((float)(c.close - minPrice)
                            / (float)(maxPrice - minPrice))
                    * chartHeight;

            Paint guide =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            guide.setColor(
                    darkMode
                            ? 0x88909090
                            : 0x66909090
            );

            guide.setStrokeWidth(1.5f);

            canvas.drawLine(
                    x,
                    top,
                    x,
                    bottom,
                    guide
            );

            boolean up =
                    c.close >= c.open;

            int candleColor =
                    up
                            ? (darkMode
                            ? 0xFF63C174
                            : 0xFF3FAE5A)
                            : (darkMode
                            ? 0xFFFF6B6B
                            : 0xFFE05252);

            Paint ring =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(2f);
            ring.setColor(candleColor);

            canvas.drawCircle(
                    x,
                    closeY,
                    8f,
                    ring
            );

            float density =
                    getResources()
                            .getDisplayMetrics()
                            .density;

            float scaledDensity =
                    getResources()
                            .getDisplayMetrics()
                            .scaledDensity;

            float boxWidth =
                    Math.min(
                            width - (12f * density),
                            145f * density
                    );

            float boxHeight =
                    68f * density;

            float boxLeft;

            if (x < width / 2f) {
                boxLeft =
                        x + (8f * density);
            } else {
                boxLeft =
                        x - boxWidth - (8f * density);
            }

            boxLeft =
                    Math.max(
                            6f * density,
                            Math.min(
                                    width - boxWidth - (6f * density),
                                    boxLeft
                            )
                    );

            float boxTop =
                    Math.max(
                            6f * density,
                            Math.min(
                                    height - boxHeight - (6f * density),
                                    closeY - boxHeight / 2f
                            )
                    );

            Paint box =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            box.setColor(
                    darkMode
                            ? 0xFF292929
                            : 0xFFFDF8E8
            );

            canvas.drawRoundRect(
                    boxLeft,
                    boxTop,
                    boxLeft + boxWidth,
                    boxTop + boxHeight,
                    12f * density,
                    12f * density,
                    box
            );

            Paint border =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(1.5f * density);
            border.setColor(candleColor);

            canvas.drawRoundRect(
                    boxLeft,
                    boxTop,
                    boxLeft + boxWidth,
                    boxTop + boxHeight,
                    12f * density,
                    12f * density,
                    border
            );

            float tx =
                    boxLeft + (8f * density);

            float titleSize =
                    10f * scaledDensity;

            float valueSize =
                    8f * scaledDensity;

            float lineGap =
                    12f * density;

            textPaint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            textPaint.setTextSize(titleSize);
            textPaint.setColor(candleColor);

            float titleWidth =
                    textPaint.measureText(
                            up
                                    ? "کندل صعودی"
                                    : "کندل نزولی"
                    );

            canvas.drawText(
                    up
                            ? "کندل صعودی"
                            : "کندل نزولی",
                    boxLeft + (boxWidth - titleWidth) / 2f,
                    boxTop + (17f * density),
                    textPaint
            );

            textPaint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.NORMAL
                    )
            );

            textPaint.setTextSize(valueSize);
            textPaint.setColor(chartTextColor());

            float leftCol =
                    boxLeft + (8f * density);

            float rightCol =
                    boxLeft + boxWidth / 2f + (2f * density);

            float firstY =
                    boxTop + (31f * density);

            canvas.drawText(
                    "باز: " + formatCompactPrice(c.open),
                    leftCol,
                    firstY,
                    textPaint
            );

            canvas.drawText(
                    "بسته: " + formatCompactPrice(c.close),
                    rightCol,
                    firstY,
                    textPaint
            );

            canvas.drawText(
                    "بیشترین: " + formatCompactPrice(c.high),
                    leftCol,
                    firstY + lineGap,
                    textPaint
            );

            canvas.drawText(
                    "کمترین: " + formatCompactPrice(c.low),
                    rightCol,
                    firstY + lineGap,
                    textPaint
            );

            double pct =
                    c.open == 0
                            ? 0.0
                            : ((double)c.close - c.open)
                            * 100.0 / c.open;

            textPaint.setColor(
                    pct >= 0
                            ? (darkMode
                            ? 0xFF8FE0A0
                            : 0xFF3FAE5A)
                            : (darkMode
                            ? 0xFFFF8585
                            : 0xFFE05252)
            );

            canvas.drawText(
                    String.format(
                            Locale.US,
                            "تغییر: %.2f%%",
                            pct
                    ),
                    leftCol,
                    firstY + lineGap * 2f,
                    textPaint
            );

            textPaint.setColor(
                    chartTextColor()
            );

            String time =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date(c.startTime)
                    );

            canvas.drawText(
                    "زمان: " + time,
                    rightCol,
                    firstY + lineGap * 2f,
                    textPaint
            );
        }

        // آخرین کندل را با یک حلقه ظریف مشخص می‌کنیم.
        if (!candles.isEmpty()) {

            Candle last =
                    candles.get(
                            candles.size() - 1
                    );

            float x =
                    left +
                    candleSlot *
                    (candles.size() - 1) +
                    candleSlot / 2f;

            float closeY =
                    bottom -
                    ((float)(last.close - minPrice)
                            / (float)(maxPrice - minPrice))
                            * chartHeight;

            Paint markerPaint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            markerPaint.setStyle(
                    Paint.Style.STROKE
            );

            markerPaint.setStrokeWidth(2f);

            markerPaint.setColor(
                    last.close >= last.open
                            ? (darkMode
                            ? 0xFF63C174
                            : 0xFF3FAE5A)
                            : (darkMode
                            ? 0xFFFF6B6B
                            : 0xFFE05252)
            );

            canvas.drawCircle(
                    x,
                    closeY,
                    7f,
                    markerPaint
            );
        }
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

        if (candlestickMode) {
            drawCandlestickChart(canvas, width, height);
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
        textPaint.setTextSize(24f);
        textPaint.setColor(chartTextColor());
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

        // رسم خط به صورت قطعه‌ای:
        // صعود = سبز / نزول = قرمز / بدون تغییر = طلایی
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

                    JSONObject previousItem =
                            data.getJSONObject(i - 1);

                    long previousPrice =
                            previousItem.getLong("price");

                    float previousX;

                    if (count == 1) {

                        previousX =
                                left +
                                chartWidth / 2f;

                    } else {

                        previousX =
                                left +
                                chartWidth *
                                (i - 1) /
                                (count - 1f);
                    }

                    float previousNormalized =
                            (float)
                                    (previousPrice - minPrice)
                            /
                            (float)
                                    (maxPrice - minPrice);

                    float previousY =
                            bottom -
                            previousNormalized * chartHeight;

                    if (price > previousPrice) {

                        linePaint.setColor(
                                darkMode
                                        ? 0xFF63C174
                                        : 0xFF3FAE5A
                        );

                    } else if (price < previousPrice) {

                        linePaint.setColor(
                                darkMode
                                        ? 0xFFFF6B6B
                                        : 0xFFE05252
                        );

                    } else {

                        linePaint.setColor(
                                darkMode
                                        ? 0xFFFFD966
                                        : GOLD_DARK
                        );
                    }

                    canvas.drawLine(
                            previousX,
                            previousY,
                            x,
                            y,
                            linePaint
                    );

                    fillPath.lineTo(
                            x,
                            y
                    );
                }

                lastX = x;
                lastY = y;

            } catch (Exception ignored) {
            }
        }

        // تکمیل محدوده زیر نمودار
        fillPath.lineTo(
                lastX,
                bottom
        );

        fillPath.close();

        canvas.drawPath(
                fillPath,
                fillPaint
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

        // نمایش نقطه انتخاب‌شده با لمس
        if (selectedIndex >= 0
                && selectedIndex < count) {

            try {

                JSONObject selected =
                        data.getJSONObject(selectedIndex);

                long selectedPrice =
                        selected.getLong("price");

                float selectedX;

                if (count == 1) {

                    selectedX =
                            left + chartWidth / 2f;

                } else {

                    selectedX =
                            left +
                            chartWidth *
                            selectedIndex /
                            (count - 1f);
                }

                float normalized =
                        (float)
                                (selectedPrice - minPrice)
                        /
                        (float)
                                (maxPrice - minPrice);

                float selectedY =
                        bottom -
                        normalized * chartHeight;

                Paint guidePaint =
                        new Paint(Paint.ANTI_ALIAS_FLAG);

                guidePaint.setColor(
                        darkMode
                                ? 0x88909090
                                : 0x66909090
                );

                guidePaint.setStrokeWidth(2f);

                // خط عمودی راهنما
                canvas.drawLine(
                        selectedX,
                        top,
                        selectedX,
                        bottom,
                        guidePaint
                );

                // حلقه نقطه انتخاب‌شده
                canvas.drawCircle(
                        selectedX,
                        selectedY,
                        13f,
                        pointRingPaint
                );

                canvas.drawCircle(
                        selectedX,
                        selectedY,
                        8f,
                        pointPaint
                );

                // قیمت نقطه انتخاب‌شده
                textPaint.setColor(chartGoldDarkColor());
                textPaint.setTextSize(12f);
                textPaint.setTypeface(
                        Typeface.create(
                                Typeface.DEFAULT,
                                Typeface.BOLD
                        )
                );

                String selectedText =
                        formatPrice(selectedPrice);

                float selectedTextWidth =
                        textPaint.measureText(
                                selectedText
                        );

                float selectedTextX =
                        selectedX -
                        selectedTextWidth / 2f;

                if (selectedTextX < left) {
                    selectedTextX = left;
                }

                if (selectedTextX + selectedTextWidth > right) {
                    selectedTextX =
                            right - selectedTextWidth;
                }

                float selectedTextY =
                        selectedY - 14f;

                if (selectedTextY < 14f) {
                    selectedTextY =
                            selectedY + 24f;
                }

                // باکس اطلاعات نقطه انتخاب‌شده
                String boxTimeText =
                        formatTime(selected.getLong("time"));

                textPaint.setTextSize(30f);
                textPaint.setTypeface(
                        Typeface.create(
                                Typeface.DEFAULT,
                                Typeface.BOLD
                        )
                );

                float boxPadding = 36f;

                float boxTimeWidth =
                        textPaint.measureText(boxTimeText);

                float boxPriceWidth =
                        textPaint.measureText(selectedText);

                float boxWidth =
                        Math.max(boxTimeWidth, boxPriceWidth)
                        + boxPadding * 2f;

                float boxHeight = 82f;

                float boxX =
                        selectedX - boxWidth / 2f;

                float boxY =
                        selectedY - boxHeight - 18f;

                if (boxX < left) {
                    boxX = left;
                }

                if (boxX + boxWidth > right) {
                    boxX = right - boxWidth;
                }

                if (boxY < top) {
                    boxY = selectedY + 18f;
                }

                Paint boxPaint =
                        new Paint(Paint.ANTI_ALIAS_FLAG);

                boxPaint.setColor(chartBoxColor());
                boxPaint.setStyle(Paint.Style.FILL);

                canvas.drawRoundRect(
                        boxX,
                        boxY,
                        boxX + boxWidth,
                        boxY + boxHeight,
                        12f,
                        12f,
                        boxPaint
                );

                Paint boxStroke =
                        new Paint(Paint.ANTI_ALIAS_FLAG);

                boxStroke.setColor(chartGoldDarkColor());
                boxStroke.setStyle(Paint.Style.STROKE);
                boxStroke.setStrokeWidth(1.5f);

                canvas.drawRoundRect(
                        boxX,
                        boxY,
                        boxX + boxWidth,
                        boxY + boxHeight,
                        12f,
                        12f,
                        boxStroke
                );

                textPaint.setColor(chartGoldDarkColor());
                textPaint.setTextSize(30f);

                boxPriceWidth = textPaint.measureText(selectedText);

                float priceX =
                        boxX +
                        (boxWidth - boxPriceWidth) / 2f;

                canvas.drawText(
                        selectedText,
                        priceX,
                        boxY + 42f,
                        textPaint
                );

                textPaint.setColor(chartTextColor());
                textPaint.setTextSize(30f);

                boxTimeWidth = textPaint.measureText(boxTimeText);

                float boxTimeX =
                        boxX +
                        (boxWidth - boxTimeWidth) / 2f;

                canvas.drawText(
                        boxTimeText,
                        boxTimeX,
                        boxY + 68f,
                        textPaint
                );

                // زمان نقطه انتخاب‌شده
                long selectedTime =
                        selected.getLong("time");

                String selectedTimeText =
                        formatTime(selectedTime);

                textPaint.setColor(chartTextColor());
                textPaint.setTextSize(24f);
                textPaint.setTypeface(
                        Typeface.create(
                                Typeface.DEFAULT,
                                Typeface.NORMAL
                        )
                );

                float timeWidth =
                        textPaint.measureText(
                                selectedTimeText
                        );

                float timeX =
                        selectedX -
                        timeWidth / 2f;

                if (timeX < left) {
                    timeX = left;
                }

                if (timeX + timeWidth > right) {
                    timeX =
                            right - timeWidth;
                }

                canvas.drawText(
                        selectedTimeText,
                        timeX,
                        height - 14f,
                        textPaint
                );

            } catch (Exception ignored) {
            }
        }

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

            textPaint.setColor(chartGoldDarkColor());
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

            textPaint.setColor(chartTextColor());
            textPaint.setTextSize(14f);
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

            textPaint.setColor(chartTextColor());
            textPaint.setTextSize(14f);

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
