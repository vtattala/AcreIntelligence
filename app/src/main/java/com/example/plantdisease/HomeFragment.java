package com.example.plantdisease;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView welcomeText;
    private String userName, userEmail, userCountry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (getArguments() != null) {
            userName = getArguments().getString("USER_NAME");
            userEmail = getArguments().getString("USER_EMAIL");
            userCountry = getArguments().getString("USER_COUNTRY");
        }

        welcomeText = view.findViewById(R.id.welcomeText);
        View plantDiseaseBtn = view.findViewById(R.id.plantDiseaseBtn);
        View insectDetectionBtn = view.findViewById(R.id.insectDetectionBtn);
        View plantInfoBtn = view.findViewById(R.id.plantInfoBtn);
        View notepadBtn = view.findViewById(R.id.notepadBtn);
        View encyclopediaBtn = view.findViewById(R.id.encyclopediaBtn);
        View regionalGuideBtn = view.findViewById(R.id.regionalGuideBtn);
        View chatbotBtn = view.findViewById(R.id.chatbotBtn);
        View satelliteBtn = view.findViewById(R.id.satelliteBtn);
        View droneBtn = view.findViewById(R.id.droneBtn);

        welcomeText.setText(
                "Welcome back, " + userName +
                        "!\nCountry: " + userCountry +
                        "\nEmail: " + userEmail
        );

        plantDiseaseBtn.setOnClickListener(v -> {
            Intent plantIntent = new Intent(getActivity(), MainActivity.class);
            plantIntent.putExtra("USER_NAME", userName);
            plantIntent.putExtra("USER_EMAIL", userEmail);
            plantIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(plantIntent);
        });

        insectDetectionBtn.setOnClickListener(v -> {
            Intent insectIntent = new Intent(getActivity(), InsectActivity.class);
            insectIntent.putExtra("USER_NAME", userName);
            insectIntent.putExtra("USER_EMAIL", userEmail);
            insectIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(insectIntent);
        });

        encyclopediaBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EncyclopediaActivity.class));
        });

        regionalGuideBtn.setOnClickListener(v -> {
            Intent regionalIntent = new Intent(getActivity(), RegionalGuideActivity.class);
            regionalIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(regionalIntent);
        });

        plantInfoBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PlantInfoActivity.class));
        });

        notepadBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NotepadActivity.class));
        });

        chatbotBtn.setOnClickListener(v -> {
            Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
            chatIntent.putExtra("USER_NAME", userName);
            startActivity(chatIntent);
        });

        satelliteBtn.setOnClickListener(v -> {
            Intent satIntent = new Intent(getActivity(), SatelliteActivity.class);
            satIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(satIntent);
        });

        if (droneBtn != null) {
            droneBtn.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), DroneActivity.class));
            });
        }

        return view;
    }
}