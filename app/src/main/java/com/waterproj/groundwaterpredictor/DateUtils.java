package com.waterproj.groundwaterpredictor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {
    private static final SimpleDateFormat BACKEND_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DateUtils() {
    }

    public static String formatBackendDate(long selection) {
        return BACKEND_DATE_FORMAT.format(new Date(selection));
    }
}
