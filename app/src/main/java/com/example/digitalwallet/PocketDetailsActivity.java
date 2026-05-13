package com.example.digitalwallet;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Utils.CustomPopup;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

public class PocketDetailsActivity extends AppCompatActivity {

    private String pocketId;
    private DatabaseReference mUserRef;
    private Pocket currentPocket;
    private double mainBalance = 0;

    private TextView tvName, tvBalance, tvWithdrawLabel, tvNoActivity;
    private EditText etName;
    private ImageButton btnSaveName;
    private ImageView imgIcon;
    private View btnWithdraw, btnDeposit, btnMore, layoutLockStatus;
    private ImageButton ibWithdraw, ibDeposit, ibMore;
    private RecyclerView recyclerActivity;
    private final List<Transaction> activityList = new ArrayList<>();
    private ActivityAdapter adapter;

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
        etName = findViewById(R.id.etPocketDetailName);
        btnSaveName = findViewById(R.id.btnSaveName);
        tvBalance = findViewById(R.id.tvPocketDetailBalance);
        imgIcon = findViewById(R.id.imgPocketDetailIcon);
        layoutLockStatus = findViewById(R.id.layoutLockStatus);
        tvNoActivity = findViewById(R.id.tvNoActivity);
        
        btnWithdraw = findViewById(R.id.btnWithdraw);
        tvWithdrawLabel = findViewById(R.id.tvWithdrawLabel);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnMore = findViewById(R.id.btnMore);

        ibWithdraw = findViewById(R.id.ibWithdraw);
        ibDeposit = findViewById(R.id.ibDeposit);
        ibMore = findViewById(R.id.ibMore);

        recyclerActivity = findViewById(R.id.recyclerPocketActivity);
        recyclerActivity.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityAdapter(activityList);
        recyclerActivity.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadPocketData();
        loadMainBalance();
        loadPocketActivity();

        btnDeposit.setOnClickListener(v -> {
            Intent intent = new Intent(this, PocketTransferActivity.class);
            intent.putExtra("pocket_id", pocketId);
            intent.putExtra("pocket_name", currentPocket != null ? currentPocket.name : "");
            intent.putExtra("mode", "deposit");
            startActivity(intent);
        });

        btnWithdraw.setOnClickListener(v -> {
            if (currentPocket != null && currentPocket.isLocked) {
                CustomPopup.show(this, "Locked", "This pocket is locked and cannot be withdrawn from.");
                return;
            }
            Intent intent = new Intent(this, PocketTransferActivity.class);
            intent.putExtra("pocket_id", pocketId);
            intent.putExtra("pocket_name", currentPocket != null ? currentPocket.name : "");
            intent.putExtra("mode", "withdraw");
            startActivity(intent);
        });

        btnMore.setOnClickListener(v -> showMoreMenu());
        
        btnSaveName.setOnClickListener(v -> saveNewName());

        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adjustTextSize(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showMoreMenu() {
        if (currentPocket == null || currentPocket.isClosed) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_pocket_more, null);
        dialog.setContentView(view);

        // Make background transparent to show rounded corners from XML
        int bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
        View bottomSheet = dialog.findViewById(bottomSheetId);

        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
        }

        TextView tvLock = view.findViewById(R.id.tvLockLabel);
        ImageView imgLock = view.findViewById(R.id.imgLockIcon);
        
        if (currentPocket.isLocked) {
            tvLock.setText("Unlock Pocket");
            imgLock.setColorFilter(Color.GRAY);
        } else {
            tvLock.setText("Lock Pocket");
            imgLock.setColorFilter(ContextCompat.getColor(this, R.color.primary_blue));
        }

        view.findViewById(R.id.btnMenuLock).setOnClickListener(v -> {
            mUserRef.child("pockets").child(pocketId).child("isLocked").setValue(!currentPocket.isLocked);
            dialog.dismiss();
        });

        view.findViewById(R.id.btnMenuEdit).setOnClickListener(v -> {
            dialog.dismiss();
            enableEditMode();
        });

        view.findViewById(R.id.btnMenuDelete).setOnClickListener(v -> {
            dialog.dismiss();
            showCustomDeleteDialog();
        });

        dialog.show();
    }

    private void loadPocketActivity() {
        mUserRef.child("pockets").child(pocketId).child("activity")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        activityList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Transaction tx = data.getValue(Transaction.class);
                            if (tx != null) activityList.add(tx);
                        }
                        Collections.reverse(activityList);
                        adapter.notifyDataSetChanged();
                        
                        if (activityList.isEmpty()) {
                            tvNoActivity.setVisibility(View.VISIBLE);
                            recyclerActivity.setVisibility(View.GONE);
                        } else {
                            tvNoActivity.setVisibility(View.GONE);
                            recyclerActivity.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void enableEditMode() {
        tvName.setVisibility(View.GONE);
        etName.setVisibility(View.VISIBLE);
        btnSaveName.setVisibility(View.VISIBLE);
        etName.setText(tvName.getText().toString());
        etName.requestFocus();
        etName.setSelection(etName.getText().length());
    }

    private void saveNewName() {
        String newName = etName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        mUserRef.child("pockets").child(pocketId).child("name").setValue(newName).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                tvName.setText(newName);
                tvName.setVisibility(View.VISIBLE);
                etName.setVisibility(View.GONE);
                btnSaveName.setVisibility(View.GONE);
                CustomPopup.show(this, "Success", "Pocket renamed to " + newName);
            }
        });
    }

    private void adjustTextSize(String s) {
        if (s.length() > 8) {
            etName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        } else {
            etName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        }
    }

    private void showCustomDeleteDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_confirm_delete, null);
        dialog.setContentView(view);

        int bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
        View bottomSheet = dialog.findViewById(bottomSheetId);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
            dialog.dismiss();
            performDelete();
        });

        view.findViewById(R.id.btnCancelDelete).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performDelete() {
        mUserRef.child("balance").setValue(mainBalance + currentPocket.amount);
        mUserRef.child("pockets").child(pocketId).child("isClosed").setValue(true);
        
        MainActivity.pendingSuccessMessage = "Pocket closed and funds returned.";
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MainActivity.pendingSuccessMessage != null) {
            String message = MainActivity.pendingSuccessMessage;
            MainActivity.pendingSuccessMessage = null;
            new Handler().postDelayed(() -> {
                CustomPopup.show(this, "Transaction Successful", message);
            }, 300);
        }
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

                    boolean isLocked = currentPocket.isLocked;
                    layoutLockStatus.setVisibility(isLocked ? View.VISIBLE : View.GONE);

                    updateUIStates();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUIStates() {
        if (currentPocket == null) return;

        if (currentPocket.isClosed) {
            btnWithdraw.setEnabled(false);
            btnDeposit.setEnabled(false);
            btnMore.setEnabled(false);
            ibMore.setEnabled(false);

            int bgColor = ContextCompat.getColor(this, R.color.separator);
            int iconColor = ContextCompat.getColor(this, R.color.text_gray);
            ColorStateList bgTint = ColorStateList.valueOf(bgColor);
            ColorStateList iconTint = ColorStateList.valueOf(iconColor);
            
            ibWithdraw.setBackgroundTintList(bgTint);
            ibWithdraw.setImageTintList(iconTint);
            
            ibDeposit.setBackgroundTintList(bgTint);
            ibDeposit.setImageTintList(iconTint);
            
            ibMore.setBackgroundTintList(bgTint);
            ibMore.setImageTintList(iconTint);
            
            btnWithdraw.setAlpha(0.5f);
            btnDeposit.setAlpha(0.5f);
            btnMore.setAlpha(0.5f);
        } else if (currentPocket.isLocked) {
            // Locked State
            btnWithdraw.setAlpha(0.5f);
            tvWithdrawLabel.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            ibWithdraw.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.separator)));
            ibWithdraw.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_gray)));
            
            // Deposit remains active
            btnDeposit.setAlpha(1.0f);
            ibDeposit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.action_deposit)));
            ibDeposit.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            
            btnMore.setAlpha(1.0f);
            ibMore.setEnabled(true);
        } else {
            // Normal Active State
            btnWithdraw.setEnabled(true);
            btnDeposit.setEnabled(true);
            btnMore.setEnabled(true);
            ibMore.setEnabled(true);

            ibWithdraw.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_blue)));
            ibWithdraw.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            tvWithdrawLabel.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            
            ibDeposit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.action_deposit)));
            ibDeposit.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            
            ibMore.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.separator)));
            ibMore.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_black)));
            
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

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        final List<Transaction> list;
        ActivityAdapter(List<Transaction> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction tx = list.get(position);
            holder.name.setText(tx.type.equals("sent") ? "Deposit" : "Withdrawal");
            
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());
            holder.date.setText(sdf.format(new Date(tx.timestamp)));
            
            holder.amount.setText("₪" + String.format("%.2f", tx.amount));
            holder.amount.setTextColor(ContextCompat.getColor(PocketDetailsActivity.this, R.color.amount_text));

            holder.profileContainer.setVisibility(View.GONE);
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setColorFilter(Color.GRAY);
            
            if (tx.type.equals("sent")) {
                holder.icon.setImageResource(R.drawable.ic_plus_deposit);
                holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5F9E5")));
            } else {
                holder.icon.setImageResource(R.drawable.ic_arrow_send);
                holder.icon.setRotation(180);
                holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE5E5")));
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView date;
            final TextView amount;
            final ImageView icon;
            final View profileContainer;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvTxName);
                date = v.findViewById(R.id.tvTxDate);
                amount = v.findViewById(R.id.tvTxAmount);
                icon = v.findViewById(R.id.imgTxIcon);
                profileContainer = v.findViewById(R.id.layoutProfileContainer);
            }
        }
    }
}
