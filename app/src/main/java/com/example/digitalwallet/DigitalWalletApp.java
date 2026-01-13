package com.example.digitalwallet;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DigitalWalletApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Apply user-specific theme if logged in
            SharedPreferences prefs = getSharedPreferences("theme_prefs_" + user.getUid(), MODE_PRIVATE);
            boolean isDarkMode = prefs.getBoolean("dark_mode", false);
            AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            // Default to light mode for login/register
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}