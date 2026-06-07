package com.example.plantdisease;

import com.waterproj.groundwaterpredictor.WaterAgentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AcreAgentRepository {
    private static final AcreAgentRepository INSTANCE = new AcreAgentRepository();

    private PlantState plantState;
    private InsectState insectState;
    private SatelliteState satelliteState;
    private RegionalState regionalState;
    private SpaceWeatherState spaceWeatherState;
    private AcreAdvice previousAdvice;

    private AcreAgentRepository() {
    }

    public static AcreAgentRepository getInstance() {
        return INSTANCE;
    }

    public synchronized void updatePlant(PlantState state) {
        plantState = state;
    }

    public synchronized void updateInsect(InsectState state) {
        insectState = state;
    }

    public synchronized void updateSatellite(SatelliteState state) {
        satelliteState = state;
    }

    public synchronized void updateRegional(RegionalState state) {
        regionalState = state;
    }

    public synchronized void updateSpaceWeather(SpaceWeatherState state) {
        spaceWeatherState = state;
    }

    public synchronized AcreAdvice buildAdvice() {
        WaterAgentRepository.WaterAgentAdvice waterAdvice =
                WaterAgentRepository.getInstance().buildAdvice();

        List<String> signals = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        List<String> nextChecks = new ArrayList<>();
        List<String> roleAdvice = new ArrayList<>();
        List<String> flags = new ArrayList<>();
        int riskScore = 0;
        int signalCount = 0;

        if (plantState == null) {
            signals.add("Plant disease scan has not been run yet.");
            nextChecks.add("Run Disease Detection if you see leaf spots, yellowing, wilting, or rot.");
        } else {
            signalCount++;
            signals.add(String.format(Locale.US, "Plant health: %s at %.1f%% confidence.",
                    plantState.label, plantState.confidence * 100.0));
            if (plantState.confidence >= 0.70f && isPlantProblem(plantState.label)) {
                riskScore += 3;
                flags.add("Plant disease risk is active.");
                actions.add("Isolate affected plants or leaves and avoid spreading water splash across foliage.");
            } else if (plantState.confidence >= 0.45f) {
                riskScore += 1;
                actions.add("Re-scan the plant in better light or from another leaf angle before acting aggressively.");
            }
        }

        if (insectState == null) {
            signals.add("Insect scan has not been run yet.");
            nextChecks.add("Run Insect ID if you see chewing, sticky residue, holes, or insects under leaves.");
        } else {
            signalCount++;
            signals.add(String.format(Locale.US, "Pest signal: %s at %.1f%% confidence.",
                    insectState.label, insectState.confidence * 100.0));
            if (insectState.confidence >= 0.65f) {
                riskScore += 2;
                flags.add("Pest pressure may be affecting crop health.");
                actions.add("Inspect leaf undersides and field edges before choosing treatment.");
            }
        }

        if (satelliteState == null) {
            signals.add("Agricultural data has not been fetched yet.");
            nextChecks.add("Fetch Agricultural Data for vegetation health, top-soil moisture, and evapotranspiration.");
        } else {
            signalCount++;
            signals.add(String.format(Locale.US,
                    "Field data: vegetation %.3f, top-soil %.3f, evapotranspiration %.2f.",
                    satelliteState.vegetationHealth,
                    satelliteState.soilMoisture,
                    satelliteState.evapotranspiration));
            if (satelliteState.vegetationHealth < 0.30) {
                riskScore += 3;
                flags.add("Vegetation stress is visible in field data.");
            } else if (satelliteState.vegetationHealth < 0.60) {
                riskScore += 1;
            }
            if (satelliteState.soilMoisture < 0.15 || satelliteState.evapotranspiration > 5.0) {
                riskScore += 2;
                actions.add("Check irrigation timing because field water demand looks elevated.");
            }
        }

        if (regionalState == null) {
            signals.add("Regional guide has not been searched yet.");
            nextChecks.add("Search Regional Guide so the advisor can adjust advice to climate and crop season.");
        } else {
            signalCount++;
            signals.add("Region: " + regionalState.location + ", " + regionalState.climateZone
                    + ", " + regionalState.currentAdvice);
            if (regionalState.temperature > 30.0 || regionalState.currentAdvice.toLowerCase(Locale.US).contains("increase irrigation")) {
                riskScore += 2;
                actions.add("Plan field work and watering around heat stress.");
            }
            if (regionalState.temperature < 10.0) {
                riskScore += 2;
                actions.add("Delay planting or protect sensitive crops from cold stress.");
            }
        }

        if (spaceWeatherState == null) {
            signals.add("Space weather has not loaded yet.");
            nextChecks.add("Refresh Space Weather before relying heavily on satellite vegetation or soil readings.");
        } else {
            signalCount++;
            signals.add(String.format(Locale.US,
                    "Space weather: %s risk, Kp %.1f, solar wind %.0f km/s, F10.7 %.0f.",
                    spaceWeatherState.risk,
                    spaceWeatherState.kpIndex,
                    spaceWeatherState.solarWindSpeed,
                    spaceWeatherState.f107Flux));
            if ("High".equals(spaceWeatherState.risk)) {
                riskScore += 3;
                flags.add("Space weather may reduce satellite/NDVI reliability.");
                actions.add("Treat satellite readings as less reliable today and confirm stress with field observation.");
            } else if ("Medium".equals(spaceWeatherState.risk)) {
                riskScore += 1;
                actions.add("Use satellite field data with caution if vegetation readings look unusual.");
            }
        }

        if (!"No model data".equals(waterAdvice.confidence)) {
            signalCount++;
            signals.add(waterAdvice.integratedRisk + "; " + waterAdvice.conditionLabel + ".");
            if ("Severe".equals(waterAdvice.priority)) {
                riskScore += 4;
                flags.add("Water risk is severe.");
            } else if ("High".equals(waterAdvice.priority)) {
                riskScore += 3;
            } else if ("Medium".equals(waterAdvice.priority)) {
                riskScore += 1;
            }
            actions.add(waterAdvice.actionStatement);
        } else {
            signals.add("Water advisor has not received enough model signals yet.");
            nextChecks.add("Run Groundwater, Soil Moisture, and Rainfall before relying on the water plan.");
        }

        String priority = riskScore >= 10 ? "Critical" : riskScore >= 7 ? "High" : riskScore >= 4 ? "Medium" : "Low";
        String confidence = signalCount >= 5 ? "High" : signalCount >= 3 ? "Moderate" : signalCount >= 1 ? "Low" : "No model data";
        String label = buildLabel(priority);
        String diagnosis = buildDiagnosis(waterAdvice);
        String finalAction = buildFinalAction(priority);
        String trend = buildTrend(priority);
        String uncertainty = "This is a decision-support advisor, not a guarantee. Confidence depends on how many scans and data tools have been run.";

        if (actions.isEmpty()) {
            actions.add("Run at least one detection or data tool to generate a farm-specific plan.");
        }
        if (nextChecks.isEmpty()) {
            nextChecks.add("Re-run this advisor after new scans, weather changes, irrigation, or field observations.");
        }
        if (flags.isEmpty()) {
            flags.add("No extreme farm-wide flag from available signals.");
        }

        roleAdvice.add("Grower: " + buildGrowerAdvice(priority));
        roleAdvice.add("Scout: prioritize the highest-risk signal first, then re-scan after action.");
        roleAdvice.add("Water manager: " + waterAdvice.rechargeOutlook);
        roleAdvice.add("Planner: compare this report with local weather, restrictions, and field records.");

        AcreAdvice advice = new AcreAdvice(
                priority,
                confidence,
                label,
                diagnosis,
                finalAction,
                trend,
                uncertainty,
                signals,
                actions,
                nextChecks,
                roleAdvice,
                flags
        );
        previousAdvice = advice;
        return advice;
    }

    private boolean isPlantProblem(String label) {
        String lower = label == null ? "" : label.toLowerCase(Locale.US);
        return !(lower.contains("healthy") || lower.contains("normal"));
    }

    private String buildLabel(String priority) {
        if ("Critical".equals(priority)) return "Integrated Farm Risk: Critical";
        if ("High".equals(priority)) return "Integrated Farm Risk: High";
        if ("Medium".equals(priority)) return "Integrated Farm Risk: Medium";
        return "Integrated Farm Risk: Low";
    }

    private String buildDiagnosis(WaterAgentRepository.WaterAgentAdvice waterAdvice) {
        boolean plantRisk = plantState != null && plantState.confidence >= 0.70f && isPlantProblem(plantState.label);
        boolean insectRisk = insectState != null && insectState.confidence >= 0.65f;
        boolean fieldStress = satelliteState != null && satelliteState.vegetationHealth < 0.60;
        boolean waterRisk = "Severe".equals(waterAdvice.priority) || "High".equals(waterAdvice.priority);
        boolean spaceRisk = spaceWeatherState != null && ("High".equals(spaceWeatherState.risk) || "Medium".equals(spaceWeatherState.risk));

        if (fieldStress && spaceRisk) {
            return "Field data may show stress, but space weather can add noise, so confirm satellite signals in the field.";
        }
        if (plantRisk && waterRisk) {
            return "Crop health and water stress are both active, so treat water management and plant protection together.";
        }
        if (plantRisk && insectRisk) {
            return "Disease and pest signals overlap; inspect before treatment so you do not solve the wrong problem.";
        }
        if (fieldStress && waterRisk) {
            return "Field-level stress agrees with the water advisor, so irrigation/recharge decisions should be prioritized.";
        }
        if (plantRisk) {
            return "The strongest current signal is plant health risk.";
        }
        if (waterRisk) {
            return "The strongest current signal is water risk.";
        }
        return "Available signals do not show a severe farm-wide issue, but keep monitoring as conditions change.";
    }

    private String buildFinalAction(String priority) {
        if ("Critical".equals(priority) || "High".equals(priority)) {
            return "Recommended Action: act today on the highest-risk signal, document what changed, and re-run the advisor.";
        }
        if ("Medium".equals(priority)) {
            return "Recommended Action: monitor closely and adjust water or crop protection within the next 24-48 hours.";
        }
        return "Recommended Action: continue normal operations and recheck after new scans or weather changes.";
    }

    private String buildTrend(String priority) {
        if (previousAdvice == null) {
            return "No previous Acre Agent report is available in this app session yet.";
        }
        int current = rank(priority);
        int previous = rank(previousAdvice.priority);
        if (current > previous) return "Trend: worsening compared with the previous Acre Agent report.";
        if (current < previous) return "Trend: improving compared with the previous Acre Agent report.";
        return "Trend: broadly stable compared with the previous Acre Agent report.";
    }

    private int rank(String priority) {
        if ("Critical".equals(priority)) return 4;
        if ("High".equals(priority)) return 3;
        if ("Medium".equals(priority)) return 2;
        if ("Low".equals(priority)) return 1;
        return 0;
    }

    private String buildGrowerAdvice(String priority) {
        if ("Critical".equals(priority) || "High".equals(priority)) {
            return "walk the field today, protect stressed crops first, and avoid broad treatment until the cause is clear.";
        }
        if ("Medium".equals(priority)) {
            return "schedule a targeted field check and re-run any uncertain model output.";
        }
        return "continue routine scouting and keep recent predictions updated.";
    }

    public static final class PlantState {
        public final String label;
        public final float confidence;

        public PlantState(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }

    public static final class InsectState {
        public final String label;
        public final float confidence;
        public final String fact1;
        public final String fact2;

        public InsectState(String label, float confidence, String fact1, String fact2) {
            this.label = label;
            this.confidence = confidence;
            this.fact1 = fact1;
            this.fact2 = fact2;
        }
    }

    public static final class SatelliteState {
        public final String region;
        public final double vegetationHealth;
        public final double soilMoisture;
        public final double evapotranspiration;

        public SatelliteState(String region, double vegetationHealth, double soilMoisture, double evapotranspiration) {
            this.region = region;
            this.vegetationHealth = vegetationHealth;
            this.soilMoisture = soilMoisture;
            this.evapotranspiration = evapotranspiration;
        }
    }

    public static final class RegionalState {
        public final String location;
        public final String climateZone;
        public final double temperature;
        public final int humidity;
        public final String currentAdvice;

        public RegionalState(String location, String climateZone, double temperature, int humidity, String currentAdvice) {
            this.location = location;
            this.climateZone = climateZone;
            this.temperature = temperature;
            this.humidity = humidity;
            this.currentAdvice = currentAdvice;
        }
    }

    public static final class SpaceWeatherState {
        public final String risk;
        public final double kpIndex;
        public final double solarWindSpeed;
        public final double f107Flux;
        public final String cropImpact;
        public final String satelliteImpact;

        public SpaceWeatherState(String risk, double kpIndex, double solarWindSpeed, double f107Flux,
                                 String cropImpact, String satelliteImpact) {
            this.risk = risk;
            this.kpIndex = kpIndex;
            this.solarWindSpeed = solarWindSpeed;
            this.f107Flux = f107Flux;
            this.cropImpact = cropImpact;
            this.satelliteImpact = satelliteImpact;
        }
    }

    public static final class AcreAdvice {
        public final String priority;
        public final String confidence;
        public final String label;
        public final String diagnosis;
        public final String finalAction;
        public final String trend;
        public final String uncertainty;
        public final List<String> signals;
        public final List<String> actions;
        public final List<String> nextChecks;
        public final List<String> roleAdvice;
        public final List<String> flags;

        public AcreAdvice(String priority, String confidence, String label, String diagnosis,
                          String finalAction, String trend, String uncertainty,
                          List<String> signals, List<String> actions, List<String> nextChecks,
                          List<String> roleAdvice, List<String> flags) {
            this.priority = priority;
            this.confidence = confidence;
            this.label = label;
            this.diagnosis = diagnosis;
            this.finalAction = finalAction;
            this.trend = trend;
            this.uncertainty = uncertainty;
            this.signals = signals;
            this.actions = actions;
            this.nextChecks = nextChecks;
            this.roleAdvice = roleAdvice;
            this.flags = flags;
        }
    }
}
