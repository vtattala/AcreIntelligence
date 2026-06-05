package com.waterproj.groundwaterpredictor;

public class RainfallRequest {
    private String region;
    private double lat;
    private double lon;
    private int horizon_hours;
    private String start_date;

    public RainfallRequest(String region, double lat, double lon, int horizon_hours, String start_date) {
        this.region = region;
        this.lat = lat;
        this.lon = lon;
        this.horizon_hours = horizon_hours;
        this.start_date = start_date;
    }

    public String getRegion() {
        return region;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int getHorizon_hours() {
        return horizon_hours;
    }

    public String getStart_date() {
        return start_date;
    }
}