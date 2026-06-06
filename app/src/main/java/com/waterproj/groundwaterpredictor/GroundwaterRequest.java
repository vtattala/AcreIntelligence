package com.waterproj.groundwaterpredictor;

public final class GroundwaterRequest {
    private final String region;
    private final String time_range;
    private final String start_date;
    private final String end_date;

    public GroundwaterRequest(String region, String timeRange, String startDate, String endDate) {
        this.region = region;
        this.time_range = timeRange;
        this.start_date = startDate;
        this.end_date = endDate;
    }

    public String getRegion() {
        return region;
    }

    public String getTime_range() {
        return time_range;
    }

    public String getStart_date() {
        return start_date;
    }

    public String getEnd_date() {
        return end_date;
    }
}
