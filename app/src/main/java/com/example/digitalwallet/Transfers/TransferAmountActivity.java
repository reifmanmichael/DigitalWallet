package com.example.digitalwallet.Transfers;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.MainActivity;
import com.example.digitalwallet.R;
import com.example.digitalwallet.Transfers.TransferReasonActivity;

public class TransferAmountActivity extends AppCompatActivity {

    private String recipientUid, recipientName;
    private EditText etAmount;
    private TextView btnContinue;
    private boolean isValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_amount);

        // Get Data
        recipientUid = getIntent().getStringExtra("recipient_uid");
        recipientName = getIntent().getStringExtra("recipient_name");

        // Set UI
        ((TextView) findViewById(R.id.tvRecipientName)).setText(recipientName);
        etAmount = findViewById(R.id.etAmount);
        btnContinue = findViewById(R.id.btnContinue);

        // Initial State
        setContinueActive(false);

        // Navigation
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Input Logic (Crash Safe)
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();

                try {
                    // Handle edge cases like "." or empty string
                    if (input.isEmpty() || input.equals(".")) {
                        setContinueActive(false);
                        return;
                    }

                    double value = Double.parseDouble(input);
                    if (value > 0) {
                        setContinueActive(true);
                    } else {
                        setContinueActive(false);
                    }
                } catch (NumberFormatException e) {
                    setContinueActive(false);
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // Button Click
        btnContinue.setOnClickListener(v -> {
            if (isValid) {
                Intent intent = new Intent(this, TransferReasonActivity.class);
                intent.putExtra("recipient_uid", recipientUid);
                intent.putExtra("recipient_name", recipientName);
                intent.putExtra("amount", etAmount.getText().toString());
                startActivity(intent);
            }
        });
    }

    private void setContinueActive(boolean active) {
        isValid = active;
        if (active) {
            btnContinue.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_black_active)));
            btnContinue.setAlpha(1f);
        } else {
            btnContinue.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_black_inactive)));
            btnContinue.setAlpha(1f); // Ensure visibility
        }
    }
}