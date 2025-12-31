package com.example.digitalwallet;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.Transfers.TransferAmountActivity;
import com.example.digitalwallet.Transfers.TransferMobileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SendActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<User> savedContacts = new ArrayList<>();
    private List<User> filteredList = new ArrayList<>();
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send); // Ensure XML matches previous step

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etSearch = findViewById(R.id.etSearch);
        recyclerView = findViewById(R.id.recyclerContacts);

        // Hide "New Contact" since we removed it conceptually
        findViewById(R.id.btnNewContact).setVisibility(View.GONE);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnMobileTransfer).setOnClickListener(v -> {
            startActivity(new Intent(SendActivity.this, TransferMobileActivity.class));
        });

        setupSearch();
        loadSavedContacts();
    }

    private void showPhoneLookupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Mobile Number");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);

        builder.setPositiveButton("Next", (dialog, which) -> {
            String phone = input.getText().toString().trim();
            if (!phone.isEmpty()) searchUserByPhone(phone);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void searchUserByPhone(String phone) {
        FirebaseDatabase.getInstance().getReference("Users")
                .orderByChild("phone")
                .equalTo(phone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                User recipient = child.getValue(User.class);
                                if (recipient != null) {
                                    // FOUND USER -> Go to Transfer Amount Screen
                                    Intent intent = new Intent(SendActivity.this, TransferAmountActivity.class);
                                    intent.putExtra("recipient_uid", recipient.uid);
                                    intent.putExtra("recipient_name", recipient.displayName);
                                    startActivity(intent);
                                    return; // Stop after finding one
                                }
                            }
                        } else {
                            Toast.makeText(SendActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadSavedContacts() {
        // 1. Get List of IDs from "saved_contacts"
        FirebaseDatabase.getInstance().getReference("Users").child(myUid).child("saved_contacts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        savedContacts.clear();
                        List<String> idsToFetch = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            idsToFetch.add(data.getKey());
                        }
                        fetchContactDetails(idsToFetch);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchContactDetails(List<String> ids) {
        if (ids.isEmpty()) {
            filter("");
            return;
        }

        // This is a simple implementation. For production, you'd want batched queries or storing minimal details in saved_contacts.
        for (String id : ids) {
            FirebaseDatabase.getInstance().getReference("Users").child(id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User u = snapshot.getValue(User.class);
                            if (u != null) {
                                savedContacts.add(u);
                                filter(etSearch.getText().toString());
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(savedContacts);
        } else {
            String lower = query.toLowerCase();
            for (User u : savedContacts) {
                if (u.displayName.toLowerCase().contains(lower) || u.phone.contains(lower)) {
                    filteredList.add(u);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
        List<User> list;
        ContactAdapter(List<User> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User u = list.get(position);
            holder.name.setText(u.displayName);
            holder.detail.setText(u.phone);

            // --- COLOR LOGIC ---
            String color = u.profileColor != null ? u.profileColor : "#E5E5EA";
            try {
                holder.bg.setColorFilter(android.graphics.Color.parseColor(color));
            } catch (Exception e) {
                holder.bg.setColorFilter(android.graphics.Color.GRAY);
            }
            // -------------------

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SendActivity.this, TransferAmountActivity.class);
                intent.putExtra("recipient_uid", u.uid);
                intent.putExtra("recipient_name", u.displayName);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView bg;
            TextView name, detail;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvContactName);
                detail = v.findViewById(R.id.tvContactDetail);
                bg = v.findViewById(R.id.imgContactBg); // Match XML ID
            }
        }
    }
}