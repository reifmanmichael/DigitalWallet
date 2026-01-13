package com.example.digitalwallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwallet.Model.Pocket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PocketDetailsActivity extends AppCompatActivity {

    private String pocketId;
    private DatabaseReference mUserRef;
    private Pocket currentPocket;
    private double mainBalance = 0;

    private TextView tvName, tvBalance;
    private ImageView imgIcon;
    private View btnWithdraw, btnDeposit, btnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pocket_details);

        pocketId = getIntent().getStringExtra("pocket_id");
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || pocketId == null) {
            finish();
            return;
        }

        mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        tvName = findViewById(R.id.tvPocketDetailName);
        tvBalance = findViewById(R.id.tvPocketDetailBalance);
        imgIcon = findViewById(R.id.imgPocketDetailIcon);
        
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnMore = findViewById(R.id.btnMore);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadPocketData();
        loadMainBalance();

        btnDeposit.setOnClickListener(v -> {
            Intent intent = new Intent(this, PocketTransferActivity.class);
            intent.putExtra("pocket_id", pocketId);
            intent.putExtra("pocket_name", currentPocket != null ? currentPocket.name : "");
            intent.putExtra("mode", "deposit");
            startActivity(intent);
        });

        btnWithdraw.setOnClickListener(v -> {
            if (currentPocket != null && currentPocket.isLocked) {
                Toast.makeText(this, "This pocket is locked!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, PocketTransferActivity.class);
            intent.putExtra("pocket_id", pocketId);
            intent.putExtra("pocket_name", currentPocket != null ? currentPocket.name : "");
            intent.putExtra("mode", "withdraw");
            startActivity(intent);
        });

        btnMore.setOnClickListener(this::showMoreMenu);
    }

    private void loadPocketData() {
        mUserRef.child("pockets").child(pocketId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentPocket = snapshot.getValue(Pocket.class);
                if (currentPocket != null) {
                    tvName.setText(currentPocket.name);
                    tvBalance.setText(String.format("₪%.2f", currentPocket.amount));
                    
                    if (currentPocket.iconName != null) {
                        int resId = getResources().getIdentifier(currentPocket.iconName, "drawable", getPackageName());
                        if (resId != 0) imgIcon.setImageResource(resId);
                    }

                    // --- HANDLE CLOSED STATE ---
                    updateUIForClosedState(currentPocket.isClosed);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUIForClosedState(boolean isClosed) {
        if (isClosed) {
            btnWithdraw.setEnabled(false);
            btnDeposit.setEnabled(false);
            btnMore.setEnabled(false);
            
            btnWithdraw.setAlpha(0.3f);
            btnDeposit.setAlpha(0.3f);
            btnMore.setAlpha(0.3f);
        } else {
            btnWithdraw.setEnabled(true);
            btnDeposit.setEnabled(true);
            btnMore.setEnabled(true);
            
            btnWithdraw.setAlpha(1.0f);
            btnDeposit.setAlpha(1.0f);
            btnMore.setAlpha(1.0f);
        }
    }

    private void loadMainBalance() {
        mUserRef.child("balance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mainBalance = Double.parseDouble(snapshot.getValue().toString());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showMoreMenu(View v) {
        if (currentPocket == null || currentPocket.isClosed) return;
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(currentPocket.isLocked ? "Unlock" : "Lock");
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Lock") || title.equals("Unlock")) {
                mUserRef.child("pockets").child(pocketId).child("isLocked").setValue(!currentPocket.isLocked);
            } else if (title.equals("Edit")) {
                // Future rename logic
            } else if (title.equals("Delete")) {
                confirmDelete();
            }
            return true;
        });
        popup.show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Pocket")
                .setMessage("Are you sure? All funds will be returned to your main balance.")
                .setPositiveButton("Delete", (d, w) -> {
                    mUserRef.child("balance").setValue(mainBalance + currentPocket.amount);
                    mUserRef.child("pockets").child(pocketId).child("isClosed").setValue(true);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}