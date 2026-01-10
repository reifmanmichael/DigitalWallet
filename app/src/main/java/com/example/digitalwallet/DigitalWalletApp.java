package com.example.digitalwallet;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class DigitalWalletApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}