package com.example.digitalwallet;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.Utils.CustomPopup;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

    private TextView tvName, tvBalance, tvWithdrawLabel;
    private EditText etName;
    private ImageButton btnSaveName;
    private ImageView imgIcon;
    private View btnWithdraw, btnDeposit, btnMore, layoutMoreExpanded, layoutLockStatus;
    private ImageButton ibWithdraw, ibDeposit, ibMore, btnActionLock, btnActionEdit;
    private View btnActionDelete;

    private boolean isMoreExpanded = false;

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
        
        btnWithdraw = findViewById(R.id.btnWithdraw);
        tvWithdrawLabel = findViewById(R.id.tvWithdrawLabel);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnMore = findViewById(R.id.btnMore);
        layoutMoreExpanded = findViewById(R.id.layoutMoreExpanded);

        ibWithdraw = findViewById(R.id.ibWithdraw);
        ibDeposit = findViewById(R.id.ibDeposit);
        ibMore = findViewById(R.id.ibMore);
        
        btnActionLock = findViewById(R.id.btnActionLock);
        btnActionEdit = findViewById(R.id.btnActionEdit);
        btnActionDelete = findViewById(R.id.btnActionDelete);

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
                CustomPopup.show(this, "Locked", "This pocket is locked and cannot be withdrawn from.");
                return;
            }
            Intent intent = new Intent(this, PocketTransferActivity.class);
            intent.putExtra("pocket_id", pocketId);
            intent.putExtra("pocket_name", currentPocket != null ? currentPocket.name : "");
            intent.putExtra("mode", "withdraw");
            startActivity(intent);
        });

        ibMore.setOnClickListener(v -> toggleMoreMenu());
        
        btnActionLock.setOnClickListener(v -> {
            if (currentPocket != null) {
                mUserRef.child("pockets").child(pocketId).child("isLocked").setValue(!currentPocket.isLocked);
                toggleMoreMenu();
            }
        });
        
        btnActionEdit.setOnClickListener(v -> {
            toggleMoreMenu();
            enableEditMode();
        });
        
        btnActionDelete.setOnClickListener(v -> {
            toggleMoreMenu();
            showCustomDeleteDialog();
        });

        btnSaveName.setOnClickListener(v -> saveNewName());

        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adjustTextSize(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void enableEditMode() {
        tvName.setVisibility(View.GONE);
        etName.setVisibility(View.VISIBLE);
        btnSaveName.setVisibility(View.VISIBLE);
        btnSaveName.setImageResource(R.drawable.ic_check);
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

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (isMoreExpanded) {
                int[] location = new int[2];
                layoutMoreExpanded.getLocationOnScreen(location);
                float x = ev.getRawX();
                float y = ev.getRawY();

                if (x < location[0] || x > location[0] + layoutMoreExpanded.getWidth() ||
                    y < location[1] || y > location[1] + layoutMoreExpanded.getHeight()) {
                    toggleMoreMenu();
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void toggleMoreMenu() {
        if (currentPocket != null && currentPocket.isClosed) return;
        
        isMoreExpanded = !isMoreExpanded;
        
        int targetHeight = isMoreExpanded ? dpToPx(200) : dpToPx(64);
        int startHeight = layoutMoreExpanded.getHeight();
        
        ValueAnimator animator = ValueAnimator.ofInt(startHeight, targetHeight);
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = layoutMoreExpanded.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            layoutMoreExpanded.setLayoutParams(params);
        });
        
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        float targetAlpha = isMoreExpanded ? 1f : 0f;
        float dotsAlpha = isMoreExpanded ? 0f : 1f;

        ibMore.animate().alpha(dotsAlpha).setDuration(200).start();
        btnActionLock.animate().alpha(targetAlpha).setDuration(300).start();
        btnActionEdit.animate().alpha(targetAlpha).setDuration(300).start();
        btnActionDelete.animate().alpha(targetAlpha).setDuration(300).start();

        ibMore.setClickable(!isMoreExpanded);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
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
                    btnActionLock.setImageTintList(ColorStateList.valueOf(isLocked ? 
                            ContextCompat.getColor(PocketDetailsActivity.this, R.color.primary_blue) : 
                            ContextCompat.getColor(PocketDetailsActivity.this, R.color.text_black)));

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
}
