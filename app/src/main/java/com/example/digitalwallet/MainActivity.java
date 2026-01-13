package com.example.digitalwallet;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.digitalwallet.Fragments.ActivityFragment;
import com.example.digitalwallet.Fragments.HomeFragment;
import com.example.digitalwallet.Fragments.PocketsFragment;
import com.example.digitalwallet.Fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private int currentTabIndex = 0;
    private final Map<Integer, Integer> tabIndices = new HashMap<>();
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabIndices.put(R.id.nav_home, 0);
        tabIndices.put(R.id.nav_activity, 1);
        tabIndices.put(R.id.nav_pockets, 2);
        tabIndices.put(R.id.nav_profile, 3);

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState != null) {
            currentTabIndex = savedInstanceState.getInt("current_tab_index", 0);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Integer indexObj = tabIndices.get(id);
            if (indexObj == null) return false;
            
            int newIndex = indexObj;
            if (newIndex == currentTabIndex && getSupportFragmentManager().findFragmentById(R.id.fragment_container) != null) {
                return true;
            }

            Fragment selectedFragment = null;
            if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_activity) selectedFragment = new ActivityFragment();
            else if (id == R.id.nav_pockets) selectedFragment = new PocketsFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                
                if (newIndex > currentTabIndex) {
                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (newIndex < currentTabIndex) {
                    ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
                }
                
                ft.replace(R.id.fragment_container, selectedFragment);
                ft.commit();
                
                currentTabIndex = newIndex;
            }
            return true;
        });

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("show_pocket_success", false)) {
                // Redirect back to the pockets fragment
                loadFragment(R.id.nav_pockets);
            } else {
                loadFragment(R.id.nav_home);
            }
        }
    }

    private void loadFragment(int menuId) {
        Fragment selectedFragment = null;
        if (menuId == R.id.nav_home) selectedFragment = new HomeFragment();
        else if (menuId == R.id.nav_activity) selectedFragment = new ActivityFragment();
        else if (menuId == R.id.nav_pockets) selectedFragment = new PocketsFragment();
        else if (menuId == R.id.nav_profile) selectedFragment = new ProfileFragment();

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            currentTabIndex = tabIndices.get(menuId);
            bottomNav.setSelectedItemId(menuId);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_tab_index", currentTabIndex);
    }
}