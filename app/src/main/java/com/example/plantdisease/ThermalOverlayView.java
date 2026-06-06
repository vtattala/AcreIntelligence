package com.example.plantdisease;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ThermalOverlayView extends View {

    private float[][] thermalData;
    private float minTemp = 20f;
    private float maxTemp = 40f;
    private Paint paint = new Paint();
    private Paint labelPaint = new Paint();
    private Paint bgPaint = new Paint();

    // Crop stress thresholds — customizable
    private float optimalLow  = 20f;
    private float optimalHigh = 30f;
    private float stressHigh  = 35f;

    public ThermalOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlpha(0.65f);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(24f);
        labelPaint.setAntiAlias(true);
        bgPaint.setColor(Color.argb(160, 0, 0, 0));
        bgPaint.setStyle(Paint.Style.FILL);
    }

    public void setOptimalRange(float low, float high) {
        this.optimalLow  = low;
        this.optimalHigh = high;
        this.stressHigh  = high + 5f;
        invalidate();
    }

    public void updateThermal(float[][] data, float min, float max) {
        this.thermalData = data;
        this.minTemp = min;
        this.maxTemp = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (thermalData == null) return;

        int rows = 24, cols = 32;
        float viewW = getWidth();
        float viewH = getHeight();

        float imageAspect = 640f / 480f;
        float viewAspect = viewW / viewH;

        float imageW, imageH, offsetX, offsetY;
        if (viewAspect > imageAspect) {
            imageH = viewH;
            imageW = viewH * imageAspect;
            offsetX = (viewW - imageW) / 2f;
            offsetY = 0;
        } else {
            imageW = viewW;
            imageH = viewW / imageAspect;
            offsetX = 0;
            offsetY = (viewH - imageH) / 2f;
        }

        float cellW = imageW / cols;
        float cellH = imageH / rows;

        int healthy = 0, mild = 0, severe = 0, cold = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float temp = thermalData[r][c];
                paint.setColor(tempToRiskColor(temp));
                float left = offsetX + c * cellW;
                float top = offsetY + r * cellH;
                canvas.drawRect(left, top, left + cellW + 1, top + cellH + 1, paint);

                if (temp < optimalLow) cold++;
                else if (temp <= optimalHigh) healthy++;
                else if (temp <= stressHigh) mild++;
                else severe++;
            }
        }

        int total = rows * cols;
        int severePercent  = (severe  * 100) / total;
        int mildPercent    = (mild    * 100) / total;
        int healthyPercent = (healthy * 100) / total;
        int coldPercent    = (cold    * 100) / total;

        canvas.drawRect(offsetX, offsetY + imageH - 90,
                offsetX + imageW, offsetY + imageH, bgPaint);

        labelPaint.setTextSize(18f);
        labelPaint.setColor(Color.WHITE);
        canvas.drawText(
                String.format("🔵 Cold: %d%%  🟢 Healthy: %d%%  🟡 Mild: %d%%  🔴 Severe: %d%%",
                        coldPercent, healthyPercent, mildPercent, severePercent),
                offsetX + 8, offsetY + imageH - 58, labelPaint);

        canvas.drawText(
                String.format("Optimal range: %.0f°C – %.0f°C", optimalLow, optimalHigh),
                offsetX + 8, offsetY + imageH - 35, labelPaint);

        if (severePercent > 20) {
            labelPaint.setColor(Color.RED);
            canvas.drawText("⚠ HIGH STRESS — IMMEDIATE ACTION REQUIRED",
                    offsetX + 8, offsetY + imageH - 12, labelPaint);
        } else if (mildPercent > 30) {
            labelPaint.setColor(Color.YELLOW);
            canvas.drawText("⚡ Mild stress detected — monitor closely",
                    offsetX + 8, offsetY + imageH - 12, labelPaint);
        } else {
            labelPaint.setColor(Color.GREEN);
            canvas.drawText("✓ Crops healthy — no action required",
                    offsetX + 8, offsetY + imageH - 12, labelPaint);
        }
    }

    private int tempToRiskColor(float temp) {
        if (temp < optimalLow) {
            return Color.argb(200, 0, 100, 255);
        } else if (temp <= optimalHigh) {
            return Color.argb(200, 0, 200, 50);
        } else if (temp <= stressHigh) {
            float t = (temp - optimalHigh) / (stressHigh - optimalHigh);
            return Color.argb(200, (int)(255 * t), (int)(200 * (1 - t) + 55), 0);
        } else {
            return Color.argb(200, 255, 0, 0);
        }
    }
}