package com.example.plantdisease;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class AcreAgentActivity extends AppCompatActivity {
    private TextView riskText;
    private TextView confidenceText;
    private TextView diagnosisText;
    private TextView finalActionText;
    private TextView signalsText;
    private TextView actionsText;
    private TextView flagsText;
    private TextView nextChecksText;
    private TextView roleAdviceText;
    private TextView trendText;
    private TextView uncertaintyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acre_agent);

        riskText = findViewById(R.id.acreAgentRiskText);
        confidenceText = findViewById(R.id.acreAgentConfidenceText);
        diagnosisText = findViewById(R.id.acreAgentDiagnosisText);
        finalActionText = findViewById(R.id.acreAgentFinalActionText);
        signalsText = findViewById(R.id.acreAgentSignalsText);
        actionsText = findViewById(R.id.acreAgentActionsText);
        flagsText = findViewById(R.id.acreAgentFlagsText);
        nextChecksText = findViewById(R.id.acreAgentNextChecksText);
        roleAdviceText = findViewById(R.id.acreAgentRoleAdviceText);
        trendText = findViewById(R.id.acreAgentTrendText);
        uncertaintyText = findViewById(R.id.acreAgentUncertaintyText);
        MaterialButton refreshButton = findViewById(R.id.refreshAcreAgentButton);

        refreshButton.setOnClickListener(v -> renderAdvice());
        renderAdvice();
    }

    private void renderAdvice() {
        AcreAgentRepository.AcreAdvice advice = AcreAgentRepository.getInstance().buildAdvice();
        riskText.setText(advice.label);
        riskText.setTextColor(colorForPriority(advice.priority));
        confidenceText.setText("Confidence: " + advice.confidence);
        diagnosisText.setText(advice.diagnosis);
        finalActionText.setText(advice.finalAction);
        signalsText.setText(formatList(advice.signals));
        actionsText.setText(formatList(advice.actions));
        flagsText.setText(formatList(advice.flags));
        nextChecksText.setText(formatList(advice.nextChecks));
        roleAdviceText.setText(formatList(advice.roleAdvice));
        trendText.setText(advice.trend);
        uncertaintyText.setText(advice.uncertainty);
    }

    private int colorForPriority(String priority) {
        if ("Critical".equals(priority) || "High".equals(priority)) {
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
