package com.waterproj.groundwaterpredictor;

public class RainfallLocationOption {
    public final String name;
    public final double lat;
    public final double lon;

    public RainfallLocationOption(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public String toString() {
        return name;
    }
}