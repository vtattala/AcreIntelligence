package com.waterproj.groundwaterpredictor;

public class RainfallResponse {
    private int prediction_id;
    private double rain_probability_24h;
    private double rainfall_mm_p10_24h;
    private double rainfall_mm_p50_24h;
    private double rainfall_mm_p90_24h;
    private double extreme_rain_risk;
    private double extreme_threshold_mm;
    private String model_version;

    public int getPrediction_id() { return prediction_id; }
    public double getRain_probability_24h() { return rain_probability_24h; }
    public double getRainfall_mm_p10_24h() { return rainfall_mm_p10_24h; }
    public double getRainfall_mm_p50_24h() { return rainfall_mm_p50_24h; }
    public double getRainfall_mm_p90_24h() { return rainfall_mm_p90_24h; }
    public double getExtreme_rain_risk() { return extreme_rain_risk; }
    public double getExtreme_threshold_mm() { return extreme_threshold_mm; }
    public String getModel_version() { return model_version; }
}