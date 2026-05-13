package com.example.digitalwallet.Transfers;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.MainActivity;
import com.example.digitalwallet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TransferAmountActivity extends AppCompatActivity {

    private String recipientUid, recipientName, mode;
    private EditText etAmount;
    private TextView btnContinue, tvErrorTitle, tvErrorDetail;
    private View cardErrorPopup;
    private double mainBalance = 0;
    private boolean isExceeded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_amount);

        recipientUid = getIntent().getStringExtra("recipient_uid");
        recipientName = getIntent().getStringExtra("recipient_name");
        mode = getIntent().getStringExtra("mode");

        ((TextView) findViewById(R.id.tvRecipientName)).setText(recipientName);
        etAmount = findViewById(R.id.etAmount);
        btnContinue = findViewById(R.id.btnContinue);
        cardErrorPopup = findViewById(R.id.cardErrorPopup);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorDetail = findViewById(R.id.tvErrorDetail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        loadBalance();

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAmount(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnContinue.setOnClickListener(v -> {
            if (!isExceeded) {
                Intent intent = new Intent(this, TransferReasonActivity.class);
                intent.putExtra("recipient_uid", recipientUid);
                intent.putExtra("recipient_name", recipientName);
                intent.putExtra("amount", etAmount.getText().toString());
                intent.putExtra("mode", mode);
                startActivity(intent);
            }
        });
    }

    private void loadBalance() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("balance")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) mainBalance = Double.parseDouble(snapshot.getValue().toString());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void validateAmount(String input) {
        if (input.isEmpty() || input.equals(".")) {
            isExceeded = false;
            btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
            hideError();
            return;
        }

        try {
            double amount = Double.parseDouble(input);
            // Only validate against balance if we are SENDING money
            if (!"request".equals(mode) && amount > mainBalance) {
                isExceeded = true;
                showError();
                btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
            } else if (amount > 0) {
                isExceeded = false;
                hideError();
                btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_active));
            } else {
                isExceeded = false;
                hideError();
                btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
            }
        } catch (NumberFormatException e) {
            isExceeded = false;
            hideError();
        }
    }

    private void showError() {
        if (cardErrorPopup.getVisibility() == View.VISIBLE) return;
        cardErrorPopup.setVisibility(View.VISIBLE);
        cardErrorPopup.setAlpha(0f);
        cardErrorPopup.setTranslationY(100f);
        cardErrorPopup.animate().alpha(1f).translationY(0f).setDuration(300).start();
    }

    private void hideError() {
        if (cardErrorPopup.getVisibility() == View.GONE) return;
        cardErrorPopup.animate().alpha(0f).translationY(100f).setDuration(200).withEndAction(() -> cardErrorPopup.setVisibility(View.GONE)).start();
    }
}