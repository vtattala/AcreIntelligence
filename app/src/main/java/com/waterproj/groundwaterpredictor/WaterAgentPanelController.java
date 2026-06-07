package com.waterproj.groundwaterpredictor;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.example.plantdisease.R;
import com.google.android.material.button.MaterialButton;

public class WaterAgentPanelController {
    private final View root;
    private View panel;
    private TextView priorityText;
    private TextView confidenceText;
    private TextView conditionText;
    private TextView actionStatementText;
    private TextView simpleExplanationText;
    private TextView diagnosisText;
    private TextView irrigationText;
    private TextView rechargeText;
    private TextView watchText;
    private TextView uncertaintyText;
    private TextView trendText;
    private TextView observationsText;
    private TextView actionsText;
    private TextView nextChecksText;
    private TextView audienceText;
    private TextView qnaText;
    private TextView extremeFlagsText;

    public WaterAgentPanelController(View root) {
        this.root = root;
    }

    public void bind() {
        panel = root.findViewById(R.id.waterAgentPanel);
        priorityText = root.findViewById(R.id.waterAgentPriorityText);
        confidenceText = root.findViewById(R.id.waterAgentConfidenceText);
        conditionText = root.findViewById(R.id.waterAgentConditionText);
        actionStatementText = root.findViewById(R.id.waterAgentActionStatementText);
        simpleExplanationText = root.findViewById(R.id.waterAgentSimpleExplanationText);
        diagnosisText = root.findViewById(R.id.waterAgentDiagnosisText);
        irrigationText = root.findViewById(R.id.waterAgentIrrigationText);
        rechargeText = root.findViewById(R.id.waterAgentRechargeText);
        watchText = root.findViewById(R.id.waterAgentWatchText);
        uncertaintyText = root.findViewById(R.id.waterAgentUncertaintyText);
        trendText = root.findViewById(R.id.waterAgentTrendText);
        observationsText = root.findViewById(R.id.waterAgentObservationsText);
        actionsText = root.findViewById(R.id.waterAgentActionsText);
        nextChecksText = root.findViewById(R.id.waterAgentNextChecksText);
        audienceText = root.findViewById(R.id.waterAgentAudienceText);
        qnaText = root.findViewById(R.id.waterAgentQnaText);
        extremeFlagsText = root.findViewById(R.id.waterAgentExtremeFlagsText);

        MaterialButton runButton = root.findViewById(R.id.runWaterAgentButton);
        if (panel != null) {
            panel.setVisibility(View.GONE);
        }
        if (runButton != null) {
            runButton.setOnClickListener(v -> {
                if (panel != null) {
                    panel.setVisibility(View.VISIBLE);
                }
                renderAdvice();
            });
        }
    }

    private void renderAdvice() {
        WaterAgentRepository.WaterAgentAdvice advice =
                WaterAgentRepository.getInstance().buildAdvice();

        setText(priorityText, advice.integratedRisk);
        if (priorityText != null) {
            priorityText.setTextColor(colorForPriority(advice.priority));
        }
        setText(confidenceText, "Confidence: " + advice.confidence);
        setText(conditionText, "Condition: " + advice.conditionLabel);
        setText(actionStatementText, advice.actionStatement);
        setText(simpleExplanationText, advice.simpleExplanation);
        setText(diagnosisText, advice.diagnosis);
        setText(irrigationText, advice.irrigationPlan);
        setText(rechargeText, advice.rechargeOutlook);
        setText(watchText, advice.watchWindow);
        setText(uncertaintyText, advice.uncertainty);
        setText(trendText, advice.trendComparison);
        setText(observationsText, formatList(advice.observations));
        setText(actionsText, formatList(advice.actions));
        setText(nextChecksText, formatList(advice.nextChecks));
        setText(audienceText, formatList(advice.audienceAdvice));
        setText(qnaText, formatList(advice.qna));
        setText(extremeFlagsText, formatList(advice.extremeFlags));
    }

    private void setText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }

    private int colorForPriority(String priority) {
        if ("Severe".equals(priority) || "High".equals(priority)) {
            return Color.parseColor("#B3261E");
        }
        if ("Medium".equals(priority)) {
            return Color.parseColor("#9A5B00");
        }
        return Color.parseColor("#0B5D5B");
    }

    private String formatList(Iterable<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("- ").append(item);
        }
        return builder.toString();
    }
}
