package com.waterproj.groundwaterpredictor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plantdisease.R;

public class SoilMoisturePredictionFragment extends Fragment {
    private SoilMoisturePanelController panelController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_soil_moisture_prediction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        panelController = new SoilMoisturePanelController(this);
        panelController.bind(view);
    }

    @Override
    public void onDestroyView() {
        if (panelController != null) {
            panelController.close();
            panelController = null;
        }
        super.onDestroyView();
    }
}
