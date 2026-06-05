package com.waterproj.groundwaterpredictor;

import java.util.List;

public final class GroundwaterResponse {
    private String region;
    private String groundwater_level_status;
    private String trend_summary;
    private List<List<Double>> heatmap;

    public String getRegion() {
        return region == null ? "" : region;
    }

    public String getGroundwater_level_status() {
        return groundwater_level_status == null ? "Unavailable" : groundwater_level_status;
    }

    public String getTrend_summary() {
        return trend_summary == null ? "No trend summary available." : trend_summary;
    }

    public List<List<Double>> getHeatmap() {
        return heatmap;
    }
}
