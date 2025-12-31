package com.example.digitalwallet.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.LoginActivity;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private ImageView imgProfileBackground; // Note the name change to match XML
    private TextView tvName, tvDetail, btnSignOut;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        imgProfileBackground = view.findViewById(R.id.imgProfileBackground);
        tvName = view.findViewById(R.id.tvProfileName);
        tvDetail = view.findViewById(R.id.tvProfileDetail);
        btnSignOut = view.findViewById(R.id.btnSignOut);

        loadUserProfile();

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        return view;
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || getContext() == null) return;

                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            tvName.setText(user.displayName);
                            tvDetail.setText(user.email);

                            // --- APPLY PROFILE COLOR TO THE BACKGROUND CIRCLE ---
                            String colorCode = user.profileColor != null ? user.profileColor : "#E5E5EA";
                            try {
                                // This tints the background circle shape
                                imgProfileBackground.setColorFilter(Color.parseColor(colorCode));
                            } catch (IllegalArgumentException e) {
                                imgProfileBackground.setColorFilter(Color.GRAY);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}