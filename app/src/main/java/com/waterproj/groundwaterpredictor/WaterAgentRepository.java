package com.waterproj.groundwaterpredictor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WaterAgentRepository {
    private static final WaterAgentRepository INSTANCE = new WaterAgentRepository();

    private GroundwaterState groundwaterState;
    private SoilState soilState;
    private RainfallState rainfallState;
    private WaterAgentAdvice previousAdvice;

    private WaterAgentRepository() {
    }

    public static WaterAgentRepository getInstance() {
        return INSTANCE;
    }

    public synchronized void updateGroundwater(GroundwaterState state) {
        groundwaterState = state;
    }

    public synchronized void updateSoil(SoilState state) {
        soilState = state;
    }

    public synchronized void updateRainfall(RainfallState state) {
        rainfallState = state;
    }

    public synchronized WaterAgentAdvice buildAdvice() {
        List<String> observations = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        List<String> nextChecks = new ArrayList<>();
        List<String> audienceAdvice = new ArrayList<>();
        List<String> qna = new ArrayList<>();
        List<String> extremeFlags = new ArrayList<>();
        int riskScore = 0;
        int signalCount = 0;

        if (groundwaterState == null) {
            observations.add("Groundwater forecast has not been run yet.");
            nextChecks.add("Run Groundwater to get aquifer status and heatmap context.");
        } else {
            signalCount++;
            observations.add("Aquifer layer: " + groundwaterState.status + " for " + groundwaterState.region
                    + " during " + groundwaterState.forecastPeriod + ".");
            String status = groundwaterState.status.toLowerCase(Locale.US);
            if (status.contains("low")) {
                riskScore += 3;
                actions.add("Reduce discretionary irrigation until aquifer status improves.");
                extremeFlags.add("Groundwater depletion risk is elevated.");
            } else if (status.contains("high")) {
                riskScore += 1;
                actions.add("Watch low-lying fields for saturation and drainage issues.");
            } else {
                riskScore += 1;
            }
        }

        if (soilState == null) {
            observations.add("Surface soil moisture has not been run yet.");
            nextChecks.add("Run Soil Moisture in the Groundwater tab to capture root-zone stress.");
        } else {
            signalCount++;
            observations.add("Surface layer: " + soilState.status
                    + String.format(Locale.US, " with average index %.3f.", soilState.average));
            if ("Dry".equalsIgnoreCase(soilState.status)) {
                riskScore += 3;
                actions.add("Prioritize shallow-root crops and irrigate early in the morning.");
                extremeFlags.add("Short-term drought stress is possible in the root zone.");
            } else if ("Wet".equalsIgnoreCase(soilState.status)) {
                riskScore += 2;
                actions.add("Delay irrigation and check drainage-sensitive areas.");
            } else {
                riskScore += 1;
            }
        }

        if (rainfallState == null) {
            observations.add("Rainfall forecast has not been run yet.");
            nextChecks.add("Run Rainfall so the agent can decide whether to irrigate now or wait.");
        } else {
            signalCount++;
            observations.add(String.format(
                    Locale.US,
                    "Rain layer: %s, %.0f%% rain chance, %.1f mm likely rainfall.",
                    rainfallState.location,
                    rainfallState.rainProbability * 100.0,
                    rainfallState.medianRainMm
            ));
            if (rainfallState.rainProbability < 0.35 && rainfallState.medianRainMm < 2.0) {
                riskScore += 2;
                actions.add("Do not wait for rain if soil moisture is already low.");
            } else if (rainfallState.rainProbability > 0.70 || rainfallState.medianRainMm >= 5.0) {
                riskScore -= 1;
                actions.add("Hold irrigation until after the forecast rainfall window.");
            }

            if (rainfallState.extremeRainRisk > 0.35) {
                riskScore += 2;
                actions.add("Prepare runoff controls before heavy rain reaches exposed soil.");
                extremeFlags.add("Extreme rainfall/runoff risk is elevated.");
            }
        }

        String diagnosis = buildDiagnosis();
        String priority = riskScore >= 8 ? "Severe" : riskScore >= 6 ? "High" : riskScore >= 3 ? "Medium" : "Low";
        String confidence = signalCount == 3 ? "High" : signalCount == 2 ? "Medium" : signalCount == 1 ? "Low" : "No model data";
        String irrigationPlan = buildIrrigationPlan();
        String rechargeOutlook = buildRechargeOutlook();
        String watchWindow = buildWatchWindow();
        String integratedRisk = "Integrated Water Risk: " + priority;
        String actionStatement = "Recommended Action: " + buildActionStatement(priority);
        String uncertainty = buildUncertainty(signalCount);
        String trendComparison = buildTrendComparison(priority);
        String conditionLabel = buildConditionLabel();
        String simpleExplanation = buildSimpleExplanation(conditionLabel);

        audienceAdvice.add("Farmers: " + buildFarmerAdvice());
        audienceAdvice.add("Households: " + buildHouseholdAdvice(priority));
        audienceAdvice.add("City planners: " + buildPlannerAdvice(priority));
        audienceAdvice.add("Water agencies: " + buildAgencyAdvice(priority));

        qna.add("Why is risk high? Risk rises when dry soil, low groundwater, low rainfall, or heavy-rain runoff risk appear together.");
        qna.add("What does low groundwater mean? It means the aquifer signal is weaker and water use should be more conservative.");
        qna.add("Why can soil be wet while groundwater is low? Soil responds quickly to rain or irrigation, while aquifers recharge more slowly.");
        qna.add("Should farmers irrigate less? If soil is wet or rain is likely, yes. If soil is dry and rain is unlikely, irrigate carefully and efficiently.");

        if (actions.isEmpty()) {
            actions.add("Run groundwater, soil moisture, and rainfall predictions to unlock a complete plan.");
        }

        if (nextChecks.isEmpty()) {
            nextChecks.add("All water signals are available. Re-run after new rain, irrigation, or field observations.");
        }

        nextChecks.add("Check local well levels, drought maps, reservoir status, and water restrictions before major decisions.");

        if (extremeFlags.isEmpty()) {
            extremeFlags.add("No extreme water-risk flag from the available model signals.");
        }

        actions.add("Re-run the Water Agent after new model results or field observations.");
        WaterAgentAdvice advice = new WaterAgentAdvice(
                priority,
                confidence,
                integratedRisk,
                actionStatement,
                conditionLabel,
                simpleExplanation,
                diagnosis,
                irrigationPlan,
                rechargeOutlook,
                watchWindow,
                uncertainty,
                trendComparison,
                observations,
                actions,
                nextChecks,
                audienceAdvice,
                qna,
                extremeFlags
        );
        previousAdvice = advice;
        return advice;
    }

    private String buildDiagnosis() {
        if (groundwaterState == null && soilState == null && rainfallState == null) {
            return "Waiting for water model signals.";
        }

        boolean drySoil = soilState != null && "Dry".equalsIgnoreCase(soilState.status);
        boolean wetSoil = soilState != null && "Wet".equalsIgnoreCase(soilState.status);
        boolean lowGroundwater = groundwaterState != null
                && groundwaterState.status.toLowerCase(Locale.US).contains("low");
        boolean rainLikely = rainfallState != null
                && (rainfallState.rainProbability > 0.70 || rainfallState.medianRainMm >= 5.0);
        boolean rainUnlikely = rainfallState != null
                && rainfallState.rainProbability < 0.35 && rainfallState.medianRainMm < 2.0;

        if (drySoil && lowGroundwater && rainUnlikely) {
            return "Water stress is likely at both the surface and aquifer layers.";
        }
        if (drySoil && !lowGroundwater) {
            return "Surface stress is stronger than aquifer stress; manage irrigation for the root zone.";
        }
        if (wetSoil && rainLikely) {
            return "Near-surface saturation risk is elevated; recharge may improve, but runoff risk rises too.";
        }
        if (lowGroundwater && rainLikely) {
            return "Rain may help recharge, but aquifer recovery can lag behind surface wetting.";
        }
        return "Water conditions are mixed; compare surface moisture, aquifer status, and rainfall timing.";
    }

    private String buildConditionLabel() {
        boolean drySoil = soilState != null && "Dry".equalsIgnoreCase(soilState.status);
        boolean wetSoil = soilState != null && "Wet".equalsIgnoreCase(soilState.status);
        boolean lowGroundwater = groundwaterState != null
                && groundwaterState.status.toLowerCase(Locale.US).contains("low");
        boolean highGroundwater = groundwaterState != null
                && groundwaterState.status.toLowerCase(Locale.US).contains("high");
        boolean rainLikely = rainfallState != null
                && (rainfallState.rainProbability > 0.70 || rainfallState.medianRainMm >= 5.0);

        if (drySoil && lowGroundwater) {
            return "Severe water stress";
        }
        if (drySoil) {
            return "Short-term drought stress";
        }
        if (lowGroundwater) {
            return "Groundwater depletion risk";
        }
        if (wetSoil && rainLikely) {
            return "Recharge-favorable conditions";
        }
        if (highGroundwater || wetSoil) {
            return "High water availability";
        }
        return "Stable water conditions";
    }

    private String buildSimpleExplanation(String conditionLabel) {
        return conditionLabel + " means the advisor is combining aquifer status, root-zone moisture, and rainfall timing into one decision-support label.";
    }

    private String buildIrrigationPlan() {
        if (soilState == null || rainfallState == null) {
            return "Run soil moisture and rainfall first; irrigation timing depends on root-zone moisture plus near-term rain.";
        }

        boolean drySoil = "Dry".equalsIgnoreCase(soilState.status);
        boolean wetSoil = "Wet".equalsIgnoreCase(soilState.status);
        boolean rainLikely = rainfallState.rainProbability > 0.70 || rainfallState.medianRainMm >= 5.0;
        boolean rainUnlikely = rainfallState.rainProbability < 0.35 && rainfallState.medianRainMm < 2.0;

        if (drySoil && rainUnlikely) {
            return "Irrigate soon, preferably morning. Keep it targeted rather than heavy if groundwater is low.";
        }
        if (drySoil && rainLikely) {
            return "Wait for the forecast rain window if crops are not wilting; use only a small bridge irrigation if needed.";
        }
        if (wetSoil) {
            return "Do not irrigate now. Focus on drainage, runoff control, and checking saturated low spots.";
        }
        if (rainLikely) {
            return "Hold irrigation until after rainfall, then re-run soil moisture before watering.";
        }
        return "Maintain normal irrigation, but recheck soil moisture before increasing water volume.";
    }

    private String buildRechargeOutlook() {
        if (soilState == null || rainfallState == null) {
            return "Recharge outlook needs both surface moisture and rainfall.";
        }

        boolean wetSoil = "Wet".equalsIgnoreCase(soilState.status);
        boolean drySoil = "Dry".equalsIgnoreCase(soilState.status);
        boolean rainLikely = rainfallState.rainProbability > 0.70 || rainfallState.medianRainMm >= 5.0;
        boolean heavyRisk = rainfallState.extremeRainRisk > 0.35;

        if (wetSoil && rainLikely && !heavyRisk) {
            return "Recharge potential is favorable because the surface layer is already wet and more rain is likely.";
        }
        if (wetSoil && heavyRisk) {
            return "Recharge may improve, but runoff/erosion risk is high, so slow water movement across bare soil.";
        }
        if (drySoil && rainLikely) {
            return "First rainfall may refill the root zone before reaching groundwater; aquifer response can lag.";
        }
        if (drySoil) {
            return "Recharge potential is weak until rainfall or irrigation moves water below the root zone.";
        }
        return "Recharge outlook is moderate; compare the next rainfall event with updated aquifer heatmap results.";
    }

    private String buildWatchWindow() {
        if (rainfallState != null && rainfallState.extremeRainRisk > 0.35) {
            return "Next 24-72 hours: monitor runoff, ponding, and erosion-prone rows.";
        }
        if (soilState != null && "Dry".equalsIgnoreCase(soilState.status)) {
            return "Next 24-48 hours: check wilting, leaf curl, and shallow-root crops before midday heat.";
        }
        if (groundwaterState != null
                && groundwaterState.status.toLowerCase(Locale.US).contains("low")) {
            return "Next week: keep irrigation conservative and watch for repeated dry soil readings.";
        }
        return "Next 2-3 days: re-run after rain, irrigation, or major weather changes.";
    }

    private String buildActionStatement(String priority) {
        if ("Severe".equals(priority) || "High".equals(priority)) {
            return "reduce water use, monitor groundwater conditions, and prepare a drought or runoff response.";
        }
        if ("Medium".equals(priority)) {
            return "adjust irrigation based on soil moisture and rainfall timing.";
        }
        return "continue normal usage, but recheck after weather or irrigation changes.";
    }

    private String buildUncertainty(int signalCount) {
        String base = "Predictions are decision-support tools, not guarantees.";
        if (signalCount == 3) {
            return base + " Confidence is high because aquifer, soil, and rainfall signals are available.";
        }
        if (signalCount == 2) {
            return base + " Confidence is moderate because one water signal is missing.";
        }
        return base + " Confidence is low until more model outputs are run.";
    }

    private String buildTrendComparison(String priority) {
        if (previousAdvice == null) {
            return "No previous Water Agent report is available in this app session yet.";
        }

        int currentRank = riskRank(priority);
        int previousRank = riskRank(previousAdvice.priority);
        if (currentRank > previousRank) {
            return "Trend: worsening since the previous Water Agent report.";
        }
        if (currentRank < previousRank) {
            return "Trend: improving since the previous Water Agent report.";
        }
        return "Trend: broadly stable compared with the previous Water Agent report.";
    }

    private int riskRank(String priority) {
        if ("Severe".equals(priority)) return 4;
        if ("High".equals(priority)) return 3;
        if ("Medium".equals(priority)) return 2;
        if ("Low".equals(priority)) return 1;
        return 0;
    }

    private String buildFarmerAdvice() {
        if (soilState != null && "Dry".equalsIgnoreCase(soilState.status)) {
            return "protect shallow-root crops first and irrigate efficiently if rain is unlikely.";
        }
        if (soilState != null && "Wet".equalsIgnoreCase(soilState.status)) {
            return "pause irrigation and inspect drainage-sensitive rows.";
        }
        return "continue normal crop water checks and re-run after field conditions change.";
    }

    private String buildHouseholdAdvice(String priority) {
        if ("Severe".equals(priority) || "High".equals(priority)) {
            return "reduce outdoor water use and watch for local restriction notices.";
        }
        return "continue normal use while avoiding unnecessary outdoor watering.";
    }

    private String buildPlannerAdvice(String priority) {
        if ("Severe".equals(priority) || "High".equals(priority)) {
            return "review demand forecasts, vulnerable neighborhoods, and emergency water messaging.";
        }
        return "track forecast updates and compare with local infrastructure reports.";
    }

    private String buildAgencyAdvice(String priority) {
        if ("Severe".equals(priority) || "High".equals(priority)) {
            return "check wells, drought maps, reservoir levels, and restriction thresholds.";
        }
        return "continue monitoring wells and compare model output with observed station data.";
    }

    public static final class GroundwaterState {
        public final String region;
        public final String status;
        public final String trendSummary;
        public final String forecastPeriod;
        public final float heatmapAverage;

        public GroundwaterState(String region, String status, String trendSummary,
                                String forecastPeriod, float heatmapAverage) {
            this.region = region;
            this.status = status;
            this.trendSummary = trendSummary;
            this.forecastPeriod = forecastPeriod;
            this.heatmapAverage = heatmapAverage;
        }
    }

    public static final class SoilState {
        public final String region;
        public final String status;
        public final float average;
        public final float min;
        public final float max;

        public SoilState(String region, String status, float average, float min, float max) {
            this.region = region;
            this.status = status;
            this.average = average;
            this.min = min;
            this.max = max;
        }
    }

    public static final class RainfallState {
        public final String location;
        public final double rainProbability;
        public final double medianRainMm;
        public final double upperRainMm;
        public final double extremeRainRisk;

        public RainfallState(String location, double rainProbability, double medianRainMm,
                             double upperRainMm, double extremeRainRisk) {
            this.location = location;
            this.rainProbability = rainProbability;
            this.medianRainMm = medianRainMm;
            this.upperRainMm = upperRainMm;
            this.extremeRainRisk = extremeRainRisk;
        }
    }

    public static final class WaterAgentAdvice {
        public final String priority;
        public final String confidence;
        public final String integratedRisk;
        public final String actionStatement;
        public final String conditionLabel;
        public final String simpleExplanation;
        public final String diagnosis;
        public final String irrigationPlan;
        public final String rechargeOutlook;
        public final String watchWindow;
        public final String uncertainty;
        public final String trendComparison;
        public final List<String> observations;
        public final List<String> actions;
        public final List<String> nextChecks;
        public final List<String> audienceAdvice;
        public final List<String> qna;
        public final List<String> extremeFlags;

        public WaterAgentAdvice(String priority, String confidence, String integratedRisk,
                                String actionStatement, String conditionLabel, String simpleExplanation,
                                String diagnosis,
                                String irrigationPlan, String rechargeOutlook, String watchWindow,
                                String uncertainty, String trendComparison,
                                List<String> observations, List<String> actions, List<String> nextChecks,
                                List<String> audienceAdvice, List<String> qna, List<String> extremeFlags) {
            this.priority = priority;
            this.confidence = confidence;
            this.integratedRisk = integratedRisk;
            this.actionStatement = actionStatement;
            this.conditionLabel = conditionLabel;
            this.simpleExplanation = simpleExplanation;
            this.diagnosis = diagnosis;
            this.irrigationPlan = irrigationPlan;
            this.rechargeOutlook = rechargeOutlook;
            this.watchWindow = watchWindow;
            this.uncertainty = uncertainty;
            this.trendComparison = trendComparison;
            this.observations = observations;
            this.actions = actions;
            this.nextChecks = nextChecks;
            this.audienceAdvice = audienceAdvice;
            this.qna = qna;
            this.extremeFlags = extremeFlags;
        }
    }
}
