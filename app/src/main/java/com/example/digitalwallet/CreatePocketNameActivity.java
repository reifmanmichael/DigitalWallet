package com.example.digitalwallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class CreatePocketNameActivity extends AppCompatActivity {

    private EditText etPocketName;
    private TextView btnContinue, tvHeaderTitle;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pocket_name);

        type = getIntent().getStringExtra("pocket_type");

        etPocketName = findViewById(R.id.etPocketName);
        btnContinue = findViewById(R.id.btnContinue);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

        if ("Savings".equals(type)) {
            tvHeaderTitle.setText("Open a savings pocket");
        } else {
            tvHeaderTitle.setText("Open an all-purpose pocket");
        }

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        etPocketName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(CreatePocketNameActivity.this, R.color.btn_black_active));
                    btnContinue.setEnabled(true);
                } else {
                    btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(CreatePocketNameActivity.this, R.color.btn_black_inactive));
                    btnContinue.setEnabled(false);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnContinue.setOnClickListener(v -> {
            Intent nextIntent;
            if ("Savings".equals(type)) {
                nextIntent = new Intent(this, CreateSavingsPlanActivity.class);
            } else {
                nextIntent = new Intent(this, CreatePocketIconActivity.class);
            }
            nextIntent.putExtra("pocket_type", type);
            nextIntent.putExtra("pocket_name", etPocketName.getText().toString().trim());
            startActivity(nextIntent);
        });
    }
}