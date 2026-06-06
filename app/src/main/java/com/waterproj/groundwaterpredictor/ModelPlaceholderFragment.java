package com.waterproj.groundwaterpredictor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plantdisease.R;

public class ModelPlaceholderFragment extends Fragment {
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";

    public static ModelPlaceholderFragment newInstance(String title, String message) {
        ModelPlaceholderFragment fragment = new ModelPlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_model_placeholder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        TextView title = view.findViewById(R.id.placeholderTitle);
        TextView message = view.findViewById(R.id.placeholderMessage);

        if (args != null) {
            title.setText(args.getString(ARG_TITLE, ""));
            message.setText(args.getString(ARG_MESSAGE, ""));
        }
    }
}
