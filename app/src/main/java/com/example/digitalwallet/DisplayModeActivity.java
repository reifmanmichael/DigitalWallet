package com.example.digitalwallet;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.card.MaterialCardView;

public class DisplayModeActivity extends AppCompatActivity {

    private MaterialCardView cardLight, cardDark;
    private RadioButton radioLight, radioDark;
    private SharedPreferences prefs;
    
    private static Bitmap mSnapshot = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_mode);

        prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        cardLight = findViewById(R.id.cardLight);
        cardDark = findViewById(R.id.cardDark);
        radioLight = findViewById(R.id.radioLight);
        radioDark = findViewById(R.id.radioDark);

        updateUI(isDarkMode);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnLightMode).setOnClickListener(v -> setThemeMode(false));
        findViewById(R.id.btnDarkMode).setOnClickListener(v -> setThemeMode(true));

        if (mSnapshot != null) {
            showTransitionOverlay();
        }
    }

    private void showTransitionOverlay() {
        final ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        final ImageView overlay = new ImageView(this);
        overlay.setImageBitmap(mSnapshot);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        root.addView(overlay);
        mSnapshot = null;

        // Transition made faster (400ms instead of 600ms)
        overlay.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> root.removeView(overlay))
                .start();
    }

    private void setThemeMode(boolean darkMode) {
        boolean currentMode = prefs.getBoolean("dark_mode", false);
        if (currentMode == darkMode) return;

        View root = findViewById(android.R.id.content);
        mSnapshot = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mSnapshot);
        root.draw(canvas);

        prefs.edit().putBoolean("dark_mode", darkMode).apply();

        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        overridePendingTransition(0, 0);
    }

    private void updateUI(boolean isDarkMode) {
        radioLight.setChecked(!isDarkMode);
        radioDark.setChecked(isDarkMode);

        if (isDarkMode) {
            cardDark.setStrokeWidth(dpToPx(2));
            cardLight.setStrokeWidth(0);
        } else {
            cardLight.setStrokeWidth(dpToPx(2));
            cardDark.setStrokeWidth(0);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}