package com.example.digitalwallet.Fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.DisplayModeActivity;
import com.example.digitalwallet.LoginActivity;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.example.digitalwallet.Utils.ProfileUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private View profileContainer;
    private TextView tvName, tvDetail;
    private View btnSignOut, btnBankAccounts, btnPersonalDetails, btnDisplayMode;
    private FirebaseAuth mAuth;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        profileContainer = view.findViewById(R.id.layoutProfileContainer);
        tvName = view.findViewById(R.id.tvProfileName);
        tvDetail = view.findViewById(R.id.tvProfileDetail);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnBankAccounts = view.findViewById(R.id.btnBankAccounts);
        btnPersonalDetails = view.findViewById(R.id.btnPersonalDetails);
        btnDisplayMode = view.findViewById(R.id.btnDisplayMode);

        loadUserProfile();

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        btnBankAccounts.setOnClickListener(v -> showBankAccountsDialog());
        btnPersonalDetails.setOnClickListener(v -> showPersonalDetailsDialog());
        
        btnDisplayMode.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DisplayModeActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || getContext() == null) return;

                        currentUser = snapshot.getValue(User.class);
                        if (currentUser != null) {
                            tvName.setText(currentUser.displayName);
                            tvDetail.setText(currentUser.email);
                            // Set larger initials for the profile fragment (36sp)
                            ProfileUtils.setProfileInitial(profileContainer, currentUser.displayName, currentUser.profileColor, 36f);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showBankAccountsDialog() {
        if (currentUser == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_bank_accounts);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvBankBalanceValue = dialog.findViewById(R.id.tvBankBalanceValue);
        tvBankBalanceValue.setText(String.format(Locale.getDefault(), "₪ %.2f", currentUser.bankBalance));

        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showPersonalDetailsDialog() {
        if (currentUser == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personal_details);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ((TextView) dialog.findViewById(R.id.tvDetailName)).setText(currentUser.displayName);
        ((TextView) dialog.findViewById(R.id.tvDetailEmail)).setText(currentUser.email);
        ((TextView) dialog.findViewById(R.id.tvDetailPhone)).setText(currentUser.phone != null ? currentUser.phone : "Not set");

        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}