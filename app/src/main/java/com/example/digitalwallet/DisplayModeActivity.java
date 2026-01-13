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
import com.google.firebase.auth.FirebaseAuth;

public class DisplayModeActivity extends AppCompatActivity {

    private MaterialCardView cardLight, cardDark;
    private RadioButton radioLight, radioDark;
    private SharedPreferences prefs;
    
    private static Bitmap mSnapshot = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_mode);

        // Use User-Specific Preferences
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "default";
        prefs = getSharedPreferences("theme_prefs_" + uid, MODE_PRIVATE);
        
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        cardLight = findViewById(R.id.cardLight);
        cardDark = findViewById(R.id.cardDark);
        radioLight = findViewById(R.id.radioLight);
        radioDark = findViewById(R.id.radioDark);

        // Force UI update from saved state
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

        // Transition made even faster (300ms) for a snappier feel
        overlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> root.removeView(overlay))
                .start();
    }

    private void setThemeMode(boolean darkMode) {
        boolean currentMode = prefs.getBoolean("dark_mode", false);
        if (currentMode == darkMode) return;

        // 1. Capture current screen state for the fade transition
        View root = findViewById(android.R.id.content);
        try {
            mSnapshot = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mSnapshot);
            root.draw(canvas);
        } catch (Exception e) {
            mSnapshot = null;
        }

        // 2. Save Preference
        prefs.edit().putBoolean("dark_mode", darkMode).apply();

        // 3. Switch theme
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        overridePendingTransition(0, 0);
    }

    private void updateUI(boolean isDarkMode) {
        // Explicitly set both to ensure they don't conflict
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