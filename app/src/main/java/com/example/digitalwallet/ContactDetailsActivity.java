package com.example.digitalwallet;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Transfers.TransferAmountActivity;
import com.example.digitalwallet.Utils.ProfileUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContactDetailsActivity extends AppCompatActivity {

    private String contactUid, contactName, contactColor;
    private LinearLayout activityContainer;
    private DatabaseReference mDatabase;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish(); return;
        }

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        contactUid = getIntent().getStringExtra("contact_uid");
        contactName = getIntent().getStringExtra("contact_name");
        contactColor = getIntent().getStringExtra("contact_color");

        // UI Refs
        View profileContainer = findViewById(R.id.layoutProfileContainer);
        TextView tvName = findViewById(R.id.tvContactName);
        activityContainer = findViewById(R.id.activityContainer);

        // Set Header
        tvName.setText(contactName);
        ProfileUtils.setProfileInitial(profileContainer, contactName, contactColor);

        // Buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnSend).setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferAmountActivity.class);
            intent.putExtra("recipient_uid", contactUid);
            intent.putExtra("recipient_name", contactName);
            startActivity(intent);
        });

        findViewById(R.id.btnRequest).setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferAmountActivity.class);
            intent.putExtra("recipient_uid", contactUid);
            intent.putExtra("recipient_name", contactName);
            intent.putExtra("mode", "request"); 
            startActivity(intent);
        });

        loadTransactionHistory();
    }

    private void loadTransactionHistory() {
        mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(myUid).child("transactions");
        
        mDatabase.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activityContainer.removeAllViews();
                List<Transaction> list = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction tx = data.getValue(Transaction.class);
                    if (tx != null && contactUid.equals(tx.relatedUserUid)) {
                        list.add(tx);
                    }
                }
                Collections.reverse(list);

                if (list.isEmpty()) {
                    showEmptyState();
                } else {
                    for (Transaction tx : list) {
                        addTransactionView(tx);
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addTransactionView(Transaction tx) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_recent_activity, activityContainer, false);

        TextView name = view.findViewById(R.id.tvTxName);
        TextView details = view.findViewById(R.id.tvTxDate);
        TextView amount = view.findViewById(R.id.tvTxAmount);
        View profileContainer = view.findViewById(R.id.layoutProfileContainer);
        ImageView icon = view.findViewById(R.id.imgTxIcon);

        name.setText(tx.relatedUserName);

        SimpleDateFormat sdf = new SimpleDateFormat("d.M.yy", Locale.getDefault());
        String dateStr = sdf.format(new Date(tx.timestamp));
        String status = tx.status != null ? tx.status : "completed";
        if (status.length() > 0) {
            status = status.substring(0, 1).toUpperCase() + status.substring(1);
        }
        details.setText(dateStr + " • " + status);

        amount.setText("₪" + String.format("%.2f", tx.amount));
        amount.setTextColor(ContextCompat.getColor(this, R.color.amount_text));

        ProfileUtils.setProfileInitial(profileContainer, tx.relatedUserName, tx.relatedUserColor);

        // --- UNIFIED ICON LOGIC ---
        icon.setColorFilter(ContextCompat.getColor(this, R.color.tx_arrow_color));
        if ("sent".equals(tx.type)) {
            icon.setImageResource(R.drawable.ic_arrow_send);
            icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bg_tx_sent)));
        } else {
            icon.setImageResource(R.drawable.ic_arrow_request);
            icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bg_tx_received)));
        }

        activityContainer.addView(view);
        
        // --- THEME-AWARE SEPARATOR ---
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        line.setBackgroundColor(ContextCompat.getColor(this, R.color.separator));
        activityContainer.addView(line);
    }

    private void showEmptyState() {
        TextView empty = new TextView(this);
        empty.setText("No transactions with " + contactName);
        empty.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(0, 80, 0, 80);
        activityContainer.addView(empty);
    }
}