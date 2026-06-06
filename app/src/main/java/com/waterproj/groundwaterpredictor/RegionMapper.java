package com.waterproj.groundwaterpredictor;

public final class RegionMapper {
    private static final String[] SUPPORTED_REGIONS = new String[]{
            "California_North",
            "California_South",
            "Michigan_Upper",
            "Michigan_Lower"
    };

    private RegionMapper() {
    }

    public static String[] getSupportedRegions() {
        return SUPPORTED_REGIONS.clone();
    }
}
