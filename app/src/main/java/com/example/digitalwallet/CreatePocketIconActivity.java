package com.example.digitalwallet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.Model.Pocket;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePocketIconActivity extends AppCompatActivity {

    private String pocketName, pocketType;
    private double initialAmount;
    private double interestRate;
    private long lockEndDate;
    private int selectedIconResId = -1;
    private IconAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pocket_icon);

        pocketName = getIntent().getStringExtra("pocket_name");
        pocketType = getIntent().getStringExtra("pocket_type");
        initialAmount = getIntent().getDoubleExtra("initial_amount", 0.0);
        interestRate = getIntent().getDoubleExtra("interest_rate", 0.0);
        lockEndDate = getIntent().getLongExtra("lock_end_date", 0L);

        RecyclerView recyclerView = findViewById(R.id.recyclerIcons);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<Integer> icons = new ArrayList<>();
        icons.add(R.drawable.ic_pocket_goal);
        icons.add(R.drawable.ic_pocket_vacation);
        icons.add(R.drawable.ic_pocket_shopping);
        icons.add(R.drawable.ic_pocket_business);
        icons.add(R.drawable.ic_pocket_car);
        icons.add(R.drawable.ic_pocket_gift);
        icons.add(R.drawable.ic_pocket_home);
        icons.add(R.drawable.ic_pocket_book);
        icons.add(R.drawable.ic_pocket_heart);

        adapter = new IconAdapter(icons);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.btnCreatePocket).setOnClickListener(v -> {
            if (selectedIconResId != -1) {
                finalizePocketCreation();
            } else {
                Toast.makeText(this, "Please select an icon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void finalizePocketCreation() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        
        mUserRef.child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double currentBalance = 0;
                if (snapshot.exists()) currentBalance = Double.parseDouble(snapshot.getValue().toString());

                if (initialAmount > currentBalance) {
                    Toast.makeText(CreatePocketIconActivity.this, "Insufficient balance for initial deposit", Toast.LENGTH_SHORT).show();
                    return;
                }

                String key = mUserRef.child("pockets").push().getKey();
                if (key == null) return;

                String iconName = getResources().getResourceEntryName(selectedIconResId);
                
                Pocket pocket = new Pocket(key, pocketName, initialAmount, pocketType, "Savings".equals(pocketType));
                pocket.iconName = iconName;
                pocket.interestRate = interestRate;
                pocket.lockEndDate = lockEndDate;
                pocket.initialDeposit = initialAmount;

                Map<String, Object> updates = new HashMap<>();
                updates.put("balance", currentBalance - initialAmount);
                updates.put("pockets/" + key, pocket);

                mUserRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(CreatePocketIconActivity.this, MainActivity.class);
                        intent.putExtra("show_pocket_success", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        List<Integer> list;
        int selectedPos = -1;

        IconAdapter(List<Integer> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pocket_icon_choice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int resId = list.get(position);
            holder.imgIcon.setImageResource(resId);
            
            if (selectedPos == position) {
                holder.cardBg.setStrokeColor(ContextCompat.getColor(CreatePocketIconActivity.this, R.color.primary_blue));
                holder.cardBg.setStrokeWidth(dpToPx(4));
                holder.cardBg.setAlpha(1.0f);
            } else {
                holder.cardBg.setStrokeColor(Color.BLACK);
                holder.cardBg.setStrokeWidth(dpToPx(1));
                holder.cardBg.setAlpha(0.7f);
            }

            holder.itemView.setOnClickListener(v -> {
                selectedPos = holder.getAdapterPosition();
                selectedIconResId = resId;
                notifyDataSetChanged();
            });
        }

        private int dpToPx(int dp) {
            float density = getResources().getDisplayMetrics().density;
            return Math.round((float) dp * density);
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgIcon;
            MaterialCardView cardBg;
            ViewHolder(View v) {
                super(v);
                imgIcon = v.findViewById(R.id.imgIconChoice);
                cardBg = v.findViewById(R.id.cardIconBg);
            }
        }
    }
}