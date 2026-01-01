package com.example.digitalwallet.Utils;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.digitalwallet.R;

public class ProfileUtils {

    public static void setProfileInitial(View container, String name, String colorHex) {
        ImageView bg = container.findViewById(R.id.imgProfileBg);
        TextView initial = container.findViewById(R.id.tvProfileInitial);

        if (bg == null || initial == null) return;

        // 1. Set Background Color
        int color = Color.parseColor(colorHex != null ? colorHex : "#E5E5EA");
        bg.setColorFilter(color, PorterDuff.Mode.SRC_IN);

        // 2. Set Initial
        if (name != null && !name.isEmpty()) {
            initial.setText(String.valueOf(name.trim().charAt(0)).toUpperCase());
        } else {
            initial.setText("?");
        }

        // 3. Contrast Logic (White vs Black)
        // Calculating brightness (Luma)
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        
        // Special case: Orange background (#FF9500 or similar) should use WHITE initial
        // We can detect "Orange" by high Red and Green, low Blue.
        boolean isOrange = Color.red(color) > 200 && Color.green(color) > 100 && Color.blue(color) < 50;

        if (darkness < 0.35 && !isOrange) {
            initial.setTextColor(Color.BLACK); // Bright background
        } else {
            initial.setTextColor(Color.WHITE); // Dark or Orange background
        }
    }
}