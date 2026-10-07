package com.mersad.gold18;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class NavShimmerDrawable extends Drawable {

    private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progress = -1f;

    public NavShimmerDrawable() {
        paint.setStyle(Paint.Style.FILL);
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {

        int width = getBounds().width();
        int height = getBounds().height();

        if (width <= 0 || height <= 0) {
            return;
        }

        // زمینه طلایی
        LinearGradient baseGradient =
                new LinearGradient(
                        0,
                        0,
                        0,
                        height,
                        new int[]{
                                0xFFC99A2E,
                                0xFFF0CF63,
                                0xFFB27F14
                        },
                        new float[]{
                                0f,
                                0.5f,
                                1f
                        },
                        Shader.TileMode.CLAMP
                );

        paint.setShader(baseGradient);

        canvas.drawRoundRect(
                0,
                0,
                width,
                height,
                11f,
                11f,
                paint
        );

        // موج نور
        float center =
                progress * width;

        LinearGradient shimmer =
                new LinearGradient(
                        center - width * 0.35f,
                        0,
                        center + width * 0.35f,
                        0,
                        new int[]{
                                0x00FFF1A8,
                                0x20FFF1A8,
                                0x70FFF1A8,
                                0x20FFF1A8,
                                0x00FFF1A8
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

        paint.setShader(shimmer);

        canvas.drawRoundRect(
                0,
                0,
                width,
                height,
                11f,
                11f,
                paint
        );

        // حاشیه طلایی روشن
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(0xFFE8CC72);

        canvas.drawRoundRect(
                0.5f,
                0.5f,
                width - 0.5f,
                height - 0.5f,
                11f,
                11f,
                paint
        );

        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(
            android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }
}
