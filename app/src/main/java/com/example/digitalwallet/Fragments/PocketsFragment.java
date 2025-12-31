package com.example.digitalwallet.Fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PocketsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateContainer;

    private DatabaseReference mDatabase;
    private PocketAdapter adapter;
    private final List<Pocket> pocketList = new ArrayList<>();
    private double currentMainBalance = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pockets, container, false);

        // Firebase Init
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        }

        // Initialize Views
        recyclerView = view.findViewById(R.id.recyclerPockets);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        FloatingActionButton fab = view.findViewById(R.id.fabAddPocket);
        CardView cardCreateGeneral = view.findViewById(R.id.cardCreateGeneral);
        CardView cardCreateSavings = view.findViewById(R.id.cardCreateSavings);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PocketAdapter(pocketList);
        recyclerView.setAdapter(adapter);

        // Listeners for Empty State Cards
        cardCreateGeneral.setOnClickListener(v -> showAddPocketDialog("General"));
        cardCreateSavings.setOnClickListener(v -> showAddPocketDialog("Savings"));

        // FAB Listener
        fab.setOnClickListener(v -> {
            if (!pocketList.isEmpty()) {
                showAddPocketDialog(null); // Null means let user choose
            } else {
                Toast.makeText(getContext(), "Choose a pocket type above first.", Toast.LENGTH_SHORT).show();
            }
        });

        // Load Data
        listenToBalance();
        loadPockets();

        return view;
    }

    private void listenToBalance() {
        if (mDatabase == null) return;
        mDatabase.child("balance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    currentMainBalance = Double.parseDouble(Objects.requireNonNull(snapshot.getValue()).toString());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPockets() {
        if (mDatabase == null) return;
        mDatabase.child("pockets").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pocketList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Pocket p = data.getValue(Pocket.class);
                    pocketList.add(p);
                }
                adapter.notifyDataSetChanged();

                // --- TOGGLE LOGIC HERE ---
                if (pocketList.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateContainer.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Helper to show dialog. passing 'preSelectedType' skips the choice buttons if not null.
    private void showAddPocketDialog(@Nullable String preSelectedType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(preSelectedType != null ? "New " + preSelectedType + " Pocket" : "New Pocket");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        final EditText nameBox = new EditText(getContext());
        nameBox.setHint("Pocket Name (e.g. Summer Trip)");
        nameBox.setBackgroundResource(android.R.drawable.edit_text);
        layout.addView(nameBox);

        final EditText amountBox = new EditText(getContext());
        amountBox.setHint("Initial Amount");
        amountBox.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountBox.setBackgroundResource(android.R.drawable.edit_text);
        layout.addView(amountBox);

        builder.setView(layout);

        if (preSelectedType != null) {
            // Direct creation if clicked from empty state
            builder.setPositiveButton("Create", (dialog, which) ->
                    createPocket(nameBox.getText().toString(), amountBox.getText().toString(), preSelectedType)
            );
            builder.setNegativeButton("Cancel", null);
        } else {
            // Choice creation if clicked from FAB
            builder.setPositiveButton("General", (dialog, which) ->
                    createPocket(nameBox.getText().toString(), amountBox.getText().toString(), "General")
            );
            builder.setNeutralButton("Savings", (dialog, which) ->
                    createPocket(nameBox.getText().toString(), amountBox.getText().toString(), "Savings")
            );
            builder.setNegativeButton("Cancel", null);
        }

        builder.show();
    }

    private void createPocket(String name, String amountStr, String type) {
        if (name.isEmpty() || amountStr.isEmpty()) return;
        double amount = Double.parseDouble(amountStr);

        if (amount > currentMainBalance) {
            Toast.makeText(getContext(), "Insufficient Funds in Main Balance!", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = mDatabase.child("pockets").push().getKey();
        if (key == null) return;

        boolean isLocked = type.equals("Savings");
        Pocket pocket = new Pocket(key, name, amount, type, isLocked);

        // Transaction: Deduct from main, add to pocket
        mDatabase.child("balance").setValue(currentMainBalance - amount);
        mDatabase.child("pockets").child(key).setValue(pocket);
    }

    // --- ADAPTER ---
    private class PocketAdapter extends RecyclerView.Adapter<PocketAdapter.ViewHolder> {
        List<Pocket> list;
        PocketAdapter(List<Pocket> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Reuse the existing item_pocket.xml you have
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pocket, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Pocket p = list.get(position);
            holder.name.setText(p.name);
            holder.amount.setText(String.format("₪ %.2f", p.amount));
            holder.type.setText(p.type);

            if (p.isLocked) {
                holder.lockIcon.setImageResource(android.R.drawable.ic_lock_lock);
                holder.lockIcon.setColorFilter(Color.RED);
            } else {
                holder.lockIcon.setImageResource(android.R.drawable.ic_lock_idle_low_battery);
                holder.lockIcon.setColorFilter(Color.GREEN);
            }

            // Click to manage (Unlock/Delete)
            holder.itemView.setOnClickListener(v -> {
                if (p.isLocked) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Unlock Savings?")
                            .setMessage("Unlock this pocket to use funds?")
                            .setPositiveButton("Unlock", (d, w) -> {
                                mDatabase.child("pockets").child(p.id).child("isLocked").setValue(false);
                                mDatabase.child("pockets").child(p.id).child("type").setValue("General");
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Delete Pocket")
                            .setMessage("Delete '" + p.name + "' and return ₪" + p.amount + " to main balance?")
                            .setPositiveButton("Delete", (d, w) -> {
                                mDatabase.child("balance").setValue(currentMainBalance + p.amount);
                                mDatabase.child("pockets").child(p.id).removeValue();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, amount, type;
            ImageView lockIcon;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvPocketName);
                amount = v.findViewById(R.id.tvPocketAmount);
                type = v.findViewById(R.id.tvPocketType);
                lockIcon = v.findViewById(R.id.imgLockStatus);
            }
        }
    }
}