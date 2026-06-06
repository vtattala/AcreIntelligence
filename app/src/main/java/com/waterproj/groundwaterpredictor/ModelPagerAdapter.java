package com.waterproj.groundwaterpredictor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ModelPagerAdapter extends FragmentStateAdapter {

    public ModelPagerAdapter(@NonNull AppCompatActivity activity) {
        super(activity);
    }

    public ModelPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new GroundwaterPredictionFragment();
        }

        if (position == 1) {
            return new RainfallPredictionFragment();
        }

        return new SoilMoisturePredictionFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
