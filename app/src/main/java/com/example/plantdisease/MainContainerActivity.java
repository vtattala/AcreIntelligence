package com.example.plantdisease;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.waterproj.groundwaterpredictor.GroundwaterAppFragment;

public class MainContainerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private String userName, userEmail, userCountry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        // Get user data passed from WelcomeActivity
        userName = getIntent().getStringExtra("USER_NAME");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userCountry = getIntent().getStringExtra("USER_COUNTRY");

        viewPager = findViewById(R.id.viewPager);

        // Create adapter with swipe pages
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Start on home page (page 0)
        viewPager.setCurrentItem(0, false);
    }

    // Adapter to manage the swipe fragments/pages
    private class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                HomeFragment homeFragment = new HomeFragment();

                // Pass user data to fragment
                Bundle bundle = new Bundle();
                bundle.putString("USER_NAME", userName);
                bundle.putString("USER_EMAIL", userEmail);
                bundle.putString("USER_COUNTRY", userCountry);
                homeFragment.setArguments(bundle);

                return homeFragment;
            } else if (position == 1) {
                return new SpaceWeatherFragment();
            } else {
                return new GroundwaterAppFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
