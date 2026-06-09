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
    private Paint clusterPaint = new Paint();
    private Paint clusterLabelBgPaint = new Paint();

    // Crop stress thresholds — customizable
    private float optimalLow  = 20f;
    private float optimalHigh = 30f;
    private float stressHigh  = 35f;
    private float airTemperature = 25f;
    private float mildDelta = 2f;
    private float severeDelta = 5f;

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
        clusterPaint.setColor(Color.YELLOW);
        clusterPaint.setStyle(Paint.Style.STROKE);
        clusterPaint.setStrokeWidth(5f);
        clusterPaint.setAntiAlias(true);
        clusterLabelBgPaint.setColor(Color.argb(190, 0, 0, 0));
        clusterLabelBgPaint.setStyle(Paint.Style.FILL);
    }

    public void setOptimalRange(float low, float high) {
        this.optimalLow  = low;
        this.optimalHigh = high;
        this.stressHigh  = high + 5f;
        invalidate();
    }

    public void setAirTemperature(float airTemperature) {
        this.airTemperature = airTemperature;
        invalidate();
    }

    public void updateThermal(float[][] data, float min, float max) {
        this.thermalData = data;
        this.minTemp = min;
        this.maxTemp = max;
        invalidate();
    }

    public void clearThermal() {
        this.thermalData = null;
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
        boolean[][] severeMask = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float temp = thermalData[r][c];
                paint.setColor(tempToRiskColor(temp));
                float left = offsetX + c * cellW;
                float top = offsetY + r * cellH;
                canvas.drawRect(left, top, left + cellW + 1, top + cellH + 1, paint);

                int stressLevel = getStressLevel(temp);
                if (stressLevel == 0) cold++;
                else if (stressLevel == 1) healthy++;
                else if (stressLevel == 2) mild++;
                else {
                    severe++;
                    severeMask[r][c] = true;
                }
            }
        }

        StressCluster cluster = findMoranStressCluster(severeMask, rows, cols);
        if (cluster != null) {
            drawStressClusterBox(canvas, cluster, offsetX, offsetY, cellW, cellH);
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
                String.format("Air: %.0fC  Leaf-air stress: +%.0fC mild, +%.0fC severe",
                        airTemperature, mildDelta, severeDelta),
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

    private void drawStressClusterBox(Canvas canvas, StressCluster cluster,
                                      float offsetX, float offsetY, float cellW, float cellH) {
        float left = offsetX + cluster.minColumn * cellW;
        float top = offsetY + cluster.minRow * cellH;
        float right = offsetX + (cluster.maxColumn + 1) * cellW;
        float bottom = offsetY + (cluster.maxRow + 1) * cellH;
        float pad = 4f;
        canvas.drawRect(left - pad, top - pad, right + pad, bottom + pad, clusterPaint);

        String label = String.format("Moran cluster I=%.2f  n=%d", cluster.moranI, cluster.cellCount);
        labelPaint.setTextSize(17f);
        labelPaint.setColor(Color.YELLOW);
        float labelWidth = labelPaint.measureText(label);
        float labelLeft = Math.max(offsetX, left - pad);
        float labelTop = Math.max(offsetY, top - 28f);
        canvas.drawRect(labelLeft, labelTop, labelLeft + labelWidth + 14f, labelTop + 24f, clusterLabelBgPaint);
        canvas.drawText(label, labelLeft + 7f, labelTop + 17f, labelPaint);
    }

    private StressCluster findMoranStressCluster(boolean[][] severeMask, int rows, int cols) {
        int severeCount = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (severeMask[row][col]) {
                    severeCount++;
                }
            }
        }

        int total = rows * cols;
        if (severeCount < 4 || severeCount == total) {
            return null;
        }

        double moranI = calculateBinaryMoranI(severeMask, rows, cols, severeCount);
        if (moranI < 0.12) {
            return null;
        }

        boolean[][] visited = new boolean[rows][cols];
        StressCluster best = null;
        int minClusterCells = Math.max(5, Math.round(total * 0.008f));
        int[] queueRows = new int[total];
        int[] queueCols = new int[total];

        for (int startRow = 0; startRow < rows; startRow++) {
            for (int startCol = 0; startCol < cols; startCol++) {
                if (!severeMask[startRow][startCol] || visited[startRow][startCol]) {
                    continue;
                }

                int head = 0;
                int tail = 0;
                queueRows[tail] = startRow;
                queueCols[tail] = startCol;
                tail++;
                visited[startRow][startCol] = true;

                int minRow = startRow;
                int maxRow = startRow;
                int minCol = startCol;
                int maxCol = startCol;
                int count = 0;
                int neighborLinks = 0;

                while (head < tail) {
                    int row = queueRows[head];
                    int col = queueCols[head];
                    head++;
                    count++;
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);

                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) {
                                continue;
                            }
                            int nr = row + dr;
                            int nc = col + dc;
                            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                                continue;
                            }
                            if (severeMask[nr][nc]) {
                                neighborLinks++;
                                if (!visited[nr][nc]) {
                                    visited[nr][nc] = true;
                                    queueRows[tail] = nr;
                                    queueCols[tail] = nc;
                                    tail++;
                                }
                            }
                        }
                    }
                }

                boolean compactEnough = neighborLinks >= count * 2;
                if (count >= minClusterCells && compactEnough
                        && (best == null || count > best.cellCount)) {
                    best = new StressCluster(minRow, maxRow, minCol, maxCol, count, moranI);
                }
            }
        }

        return best;
    }

    private double calculateBinaryMoranI(boolean[][] severeMask, int rows, int cols, int severeCount) {
        int total = rows * cols;
        double mean = severeCount / (double) total;
        double denominator = 0.0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double centered = (severeMask[row][col] ? 1.0 : 0.0) - mean;
                denominator += centered * centered;
            }
        }
        if (denominator == 0.0) {
            return 0.0;
        }

        double numerator = 0.0;
        int weightSum = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double current = (severeMask[row][col] ? 1.0 : 0.0) - mean;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }
                        int nr = row + dr;
                        int nc = col + dc;
                        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                            continue;
                        }
                        double neighbor = (severeMask[nr][nc] ? 1.0 : 0.0) - mean;
                        numerator += current * neighbor;
                        weightSum++;
                    }
                }
            }
        }

        if (weightSum == 0) {
            return 0.0;
        }
        return (total / (double) weightSum) * (numerator / denominator);
    }

    private static final class StressCluster {
        final int minRow;
        final int maxRow;
        final int minColumn;
        final int maxColumn;
        final int cellCount;
        final double moranI;

        StressCluster(int minRow, int maxRow, int minColumn, int maxColumn,
                      int cellCount, double moranI) {
            this.minRow = minRow;
            this.maxRow = maxRow;
            this.minColumn = minColumn;
            this.maxColumn = maxColumn;
            this.cellCount = cellCount;
            this.moranI = moranI;
        }
    }

    private int tempToRiskColor(float temp) {
        int stressLevel = getStressLevel(temp);
        if (stressLevel == 0) {
            return Color.argb(200, 0, 100, 255);
        } else if (stressLevel == 1) {
            return Color.argb(200, 0, 200, 50);
        } else if (stressLevel == 2) {
            float delta = Math.max(0f, temp - airTemperature);
            float t = Math.max(0f, Math.min(1f, (delta - mildDelta) / (severeDelta - mildDelta)));
            return Color.argb(200, (int)(255 * t), (int)(200 * (1 - t) + 55), 0);
        } else {
            return Color.argb(200, 255, 0, 0);
        }
    }

    private int getStressLevel(float leafTemp) {
        float leafAirDelta = leafTemp - airTemperature;
        if (leafTemp < optimalLow) {
            return 0;
        }
        if (leafAirDelta > severeDelta || leafTemp > stressHigh) {
            return 3;
        }
        if (leafAirDelta > mildDelta || leafTemp > optimalHigh) {
            return 2;
        }
        return 1;
    }
}
