package com.example.digitalwallet;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class WithdrawActivity extends AppCompatActivity {

    private EditText etAmount;
    private TextView tvBalanceHint, btnWithdrawAll, btnContinue;
    private View cardErrorPopup;
    private DatabaseReference mUserRef;
    private double currentBalance = 0;
    private double currentBankBalance = 0;
    private boolean isExceeded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        etAmount = findViewById(R.id.etAmount);
        tvBalanceHint = findViewById(R.id.tvBalanceHint);
        btnWithdrawAll = findViewById(R.id.btnWithdrawAll);
        btnContinue = findViewById(R.id.btnContinue);
        cardErrorPopup = findViewById(R.id.cardErrorPopup);

        // Add Underline to Withdraw All
        btnWithdrawAll.setPaintFlags(btnWithdrawAll.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        
        btnContinue.setOnClickListener(v -> {
            if (!isExceeded) {
                performWithdrawal();
            }
        });

        btnWithdrawAll.setOnClickListener(v -> {
            etAmount.setText(String.format(Locale.getDefault(), "%.2f", currentBalance));
            btnWithdrawAll.setVisibility(View.GONE);
        });

        setupTextWatcher();
        loadBalance();
    }

    private void loadBalance() {
        mUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    currentBalance = user.balance;
                    currentBankBalance = user.bankBalance;
                    validateAmount(etAmount.getText().toString().trim());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupTextWatcher() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAmount(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void validateAmount(String input) {
        if (input.isEmpty() || input.equals(".")) {
            isExceeded = false;
            btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
            tvBalanceHint.setText(String.format(Locale.getDefault(), "You have ₪ %.2f in balance", currentBalance));
            btnWithdrawAll.setVisibility(View.VISIBLE);
            hideError();
            return;
        }

        try {
            double amount = Double.parseDouble(input);
            if (amount > currentBalance) {
                isExceeded = true;
                showError();
                btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
                tvBalanceHint.setText(String.format(Locale.getDefault(), "You'll have ₪ %.2f left in balance", currentBalance - amount));
                btnWithdrawAll.setVisibility(View.GONE);
            } else if (amount > 0) {
                isExceeded = false;
                hideError();
                btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_active));
                tvBalanceHint.setText(String.format(Locale.getDefault(), "You'll have ₪ %.2f left in balance", currentBalance - amount));
                if (amount >= currentBalance) btnWithdrawAll.setVisibility(View.GONE);
                else btnWithdrawAll.setVisibility(View.VISIBLE);
            } else {
                isExceeded = false;
                hideError();
                btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
                tvBalanceHint.setText(String.format(Locale.getDefault(), "You have ₪ %.2f in balance", currentBalance));
                btnWithdrawAll.setVisibility(View.VISIBLE);
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

    private void performWithdrawal() {
        String input = etAmount.getText().toString();
        if (TextUtils.isEmpty(input)) return;

        double amount = Double.parseDouble(input);
        if (amount <= 0) return;

        mUserRef.child("balance").setValue(currentBalance - amount);
        mUserRef.child("bankBalance").setValue(currentBankBalance + amount)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(WithdrawActivity.this, "Withdrawal successful", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}