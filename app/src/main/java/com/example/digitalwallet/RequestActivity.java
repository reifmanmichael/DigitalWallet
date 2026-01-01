package com.example.digitalwallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.Transfers.TransferAmountActivity;
import com.example.digitalwallet.Transfers.TransferMobileActivity;
import com.example.digitalwallet.Utils.ProfileUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<User> savedContacts = new ArrayList<>();
    private List<User> filteredList = new ArrayList<>();
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish(); return;
        }
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etSearch = findViewById(R.id.etSearch);
        recyclerView = findViewById(R.id.recyclerContacts);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnMobileTransfer).setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferMobileActivity.class);
            intent.putExtra("mode", "request");
            startActivity(intent);
        });

        setupSearch();
        loadSavedContacts();
    }

    private void loadSavedContacts() {
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

        final int totalToFetch = ids.size();
        final Set<String> processedIds = new HashSet<>();

        for (String id : ids) {
            FirebaseDatabase.getInstance().getReference("Users").child(id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User u = snapshot.getValue(User.class);
                            if (u != null) {
                                if (u.uid == null) u.uid = snapshot.getKey();
                                
                                boolean alreadyExists = false;
                                for (User existing : savedContacts) {
                                    if (u.uid.equals(existing.uid)) {
                                        alreadyExists = true;
                                        break;
                                    }
                                }
                                if (!alreadyExists) {
                                    savedContacts.add(u);
                                }
                            }
                            
                            processedIds.add(id);
                            if (processedIds.size() >= totalToFetch) {
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
        Set<String> uniqueIds = new HashSet<>();
        
        String lower = query.toLowerCase().trim();
        for (User u : savedContacts) {
            if (uniqueIds.contains(u.uid)) continue;
            
            if (lower.isEmpty() || u.displayName.toLowerCase().contains(lower) || u.phone.contains(lower)) {
                filteredList.add(u);
                uniqueIds.add(u.uid);
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

            ProfileUtils.setProfileInitial(holder.profileContainer, u.displayName, u.profileColor);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(RequestActivity.this, TransferAmountActivity.class);
                intent.putExtra("recipient_uid", u.uid);
                intent.putExtra("recipient_name", u.displayName);
                intent.putExtra("mode", "request");
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            View profileContainer;
            TextView name, detail;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvContactName);
                detail = v.findViewById(R.id.tvContactDetail);
                profileContainer = v.findViewById(R.id.layoutProfileContainer);
            }
        }
    }
}