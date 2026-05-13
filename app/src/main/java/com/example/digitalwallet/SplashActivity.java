package com.example.digitalwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle System Splash
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView spinner = findViewById(R.id.imgSpinner);
        TextView text = findViewById(R.id.tvWalletLogo);

        // --- SETUP ---
        spinner.setAlpha(0f);
        spinner.setScaleX(1f);
        spinner.setScaleY(1f);

        text.setAlpha(0f);
        text.setScaleX(0f);
        text.setScaleY(0f);

        // --- PART 1: THE CONTINUOUS SPIN ---
        // We spin from 0 to 1440 degrees (4 full rotations) in one go.
        // The AccelerateInterpolator(2.5f) makes it start very slow and end extremely fast.
        ObjectAnimator continuousSpin = ObjectAnimator.ofFloat(spinner, "rotation", 0f, 1440f);
        continuousSpin.setDuration(2000); // Total spin time
        continuousSpin.setInterpolator(new AccelerateInterpolator(2.5f));

        // Fade in quickly at the start
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(spinner, "alpha", 0f, 1f);
        fadeIn.setDuration(400);

        // --- PART 2: THE IMPLOSION (Synced with end of spin) ---
        // We start shrinking when the spin is at its fastest (approx 1400ms in)
        ObjectAnimator shrinkX = ObjectAnimator.ofFloat(spinner, "scaleX", 1f, 0f);
        ObjectAnimator shrinkY = ObjectAnimator.ofFloat(spinner, "scaleY", 1f, 0f);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(spinner, "alpha", 1f, 0f);

        // Group the shrinking parts
        AnimatorSet implosion = new AnimatorSet();
        implosion.playTogether(shrinkX, shrinkY, fadeOut);
        implosion.setDuration(600); // Takes 600ms to disappear
        implosion.setStartDelay(1400); // Wait until the spin is fast before shrinking
        implosion.setInterpolator(new AccelerateInterpolator(2f)); // Snap inward

        // --- PART 3: THE LIQUID EXPLOSION ---
        // Text appears instantly as a dot (scale 0)
        ObjectAnimator textAlpha = ObjectAnimator.ofFloat(text, "alpha", 0f, 1f);
        textAlpha.setDuration(0);

        // Explode outward
        ObjectAnimator expandX = ObjectAnimator.ofFloat(text, "scaleX", 0f, 1f);
        ObjectAnimator expandY = ObjectAnimator.ofFloat(text, "scaleY", 0f, 1f);
        expandX.setDuration(800);
        expandY.setDuration(800);
        // Tension 5f = High liquid wobble
        expandX.setInterpolator(new OvershootInterpolator(5f));
        expandY.setInterpolator(new OvershootInterpolator(5f));

        AnimatorSet explosion = new AnimatorSet();
        explosion.playTogether(textAlpha, expandX, expandY);
        // Start exactly when the implosion finishes (1400 delay + 600 duration = 2000ms)
        explosion.setStartDelay(2000);

        // --- ORCHESTRATE ---
        AnimatorSet director = new AnimatorSet();
        // Play everything in parallel (using startDelays to sequence them)
        // This ensures the "Spin" keeps running smoothly while the "Implosion" starts overlapping it
        director.playTogether(fadeIn, continuousSpin, implosion, explosion);

        director.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                text.postDelayed(() -> navigateToNextScreen(), 400);
            }
        });

        director.start();
    }

    private void navigateToNextScreen() {
        Intent intent;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}