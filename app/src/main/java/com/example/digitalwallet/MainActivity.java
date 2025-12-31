package com.example.digitalwallet;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.Fragments.ActivityFragment;
import com.example.digitalwallet.Fragments.HomeFragment;
import com.example.digitalwallet.Fragments.PocketsFragment;
import com.example.digitalwallet.Fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_activity) selectedFragment = new ActivityFragment();
            else if (id == R.id.nav_pockets) selectedFragment = new PocketsFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Default Load
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }
}