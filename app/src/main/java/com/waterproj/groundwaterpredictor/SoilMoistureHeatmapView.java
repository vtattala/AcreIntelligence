package com.waterproj.groundwaterpredictor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SoilMoistureHeatmapView extends View {
    private final Paint paint = new Paint();
    private final Paint gridPaint = new Paint();
    private final RectF cellRect = new RectF();
    private float[] values = new float[0];
    private int rows = 1;
    private int columns = 1;
    private boolean useFixedScale;
    private float fixedMin;
    private float fixedMax = 1f;
    private int colorSteps = 0;

    public SoilMoistureHeatmapView(Context context) {
        super(context);
        initPaints();
    }

    public SoilMoistureHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.FILL);
        gridPaint.setAntiAlias(false);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x33FFFFFF);
    }

    public void setHeatmap(float[] nextValues, int nextRows, int nextColumns) {
        values = nextValues == null ? new float[0] : nextValues.clone();
        rows = Math.max(1, nextRows);
        columns = Math.max(1, nextColumns);
        useFixedScale = false;
        invalidate();
    }

    public void setHeatmap(float[] nextValues, int nextRows, int nextColumns, float nextMin, float nextMax) {
        values = nextValues == null ? new float[0] : nextValues.clone();
        rows = Math.max(1, nextRows);
        columns = Math.max(1, nextColumns);
        useFixedScale = nextMax > nextMin;
        fixedMin = nextMin;
        fixedMax = nextMax;
        invalidate();
    }

    public void setColorSteps(int nextColorSteps) {
        colorSteps = Math.max(0, nextColorSteps);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values.length == 0) {
            paint.setColor(0xFFE8F2F0);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            return;
        }

        float min = fixedMin;
        float max = fixedMax;
        if (!useFixedScale) {
            min = Float.MAX_VALUE;
            max = -Float.MAX_VALUE;
            for (float value : values) {
                if (Float.isFinite(value)) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }

        if (min == Float.MAX_VALUE || max <= min) {
            min = 0f;
            max = 1f;
        }

        float cellWidth = getWidth() / (float) columns;
        float cellHeight = getHeight() / (float) rows;
        int valueCount = values.length;
        boolean drawGrid = cellWidth >= 7f && cellHeight >= 7f;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                float value = values[Math.min(index, valueCount - 1)];
                float normalized = Math.max(0f, Math.min(1f, (value - min) / (max - min)));
                if (colorSteps > 1) {
                    normalized = Math.round(normalized * (colorSteps - 1)) / (float) (colorSteps - 1);
                }
                paint.setColor(interpolateColor(normalized));
                cellRect.set(
                        (float) Math.floor(column * cellWidth),
                        (float) Math.floor(row * cellHeight),
                        (float) Math.ceil((column + 1) * cellWidth),
                        (float) Math.ceil((row + 1) * cellHeight)
                );
                canvas.drawRect(cellRect, paint);
                if (drawGrid) {
                    canvas.drawRect(cellRect, gridPaint);
                }
            }
        }
    }

    private int interpolateColor(float t) {
        int dryRed = 214;
        int dryGreen = 139;
        int dryBlue = 58;
        int wetRed = 18;
        int wetGreen = 122;
        int wetBlue = 112;

        int red = (int) (dryRed + ((wetRed - dryRed) * t));
        int green = (int) (dryGreen + ((wetGreen - dryGreen) * t));
        int blue = (int) (dryBlue + ((wetBlue - dryBlue) * t));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
