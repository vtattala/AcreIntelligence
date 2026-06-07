package com.waterproj.groundwaterpredictor;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plantdisease.R;
import com.google.android.material.button.MaterialButton;

public class WaterAgentFragment extends Fragment {
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_water_agent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        priorityText = view.findViewById(R.id.waterAgentPriorityText);
        confidenceText = view.findViewById(R.id.waterAgentConfidenceText);
        conditionText = view.findViewById(R.id.waterAgentConditionText);
        actionStatementText = view.findViewById(R.id.waterAgentActionStatementText);
        simpleExplanationText = view.findViewById(R.id.waterAgentSimpleExplanationText);
        diagnosisText = view.findViewById(R.id.waterAgentDiagnosisText);
        irrigationText = view.findViewById(R.id.waterAgentIrrigationText);
        rechargeText = view.findViewById(R.id.waterAgentRechargeText);
        watchText = view.findViewById(R.id.waterAgentWatchText);
        uncertaintyText = view.findViewById(R.id.waterAgentUncertaintyText);
        trendText = view.findViewById(R.id.waterAgentTrendText);
        observationsText = view.findViewById(R.id.waterAgentObservationsText);
        actionsText = view.findViewById(R.id.waterAgentActionsText);
        nextChecksText = view.findViewById(R.id.waterAgentNextChecksText);
        audienceText = view.findViewById(R.id.waterAgentAudienceText);
        qnaText = view.findViewById(R.id.waterAgentQnaText);
        extremeFlagsText = view.findViewById(R.id.waterAgentExtremeFlagsText);
        MaterialButton runButton = view.findViewById(R.id.runWaterAgentButton);

        runButton.setOnClickListener(v -> renderAdvice());
        renderAdvice();
    }

    private void renderAdvice() {
        WaterAgentRepository.WaterAgentAdvice advice =
                WaterAgentRepository.getInstance().buildAdvice();

        priorityText.setText(advice.integratedRisk);
        priorityText.setTextColor(colorForPriority(advice.priority));
        confidenceText.setText("Confidence: " + advice.confidence);
        conditionText.setText("Condition: " + advice.conditionLabel);
        actionStatementText.setText(advice.actionStatement);
        simpleExplanationText.setText(advice.simpleExplanation);
        diagnosisText.setText(advice.diagnosis);
        irrigationText.setText(advice.irrigationPlan);
        rechargeText.setText(advice.rechargeOutlook);
        watchText.setText(advice.watchWindow);
        uncertaintyText.setText(advice.uncertainty);
        trendText.setText(advice.trendComparison);
        observationsText.setText(formatList(advice.observations));
        actionsText.setText(formatList(advice.actions));
        nextChecksText.setText(formatList(advice.nextChecks));
        audienceText.setText(formatList(advice.audienceAdvice));
        qnaText.setText(formatList(advice.qna));
        extremeFlagsText.setText(formatList(advice.extremeFlags));
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
