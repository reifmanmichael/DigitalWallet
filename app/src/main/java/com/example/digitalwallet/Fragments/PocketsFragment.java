package com.example.digitalwallet.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.CreatePocketNameActivity;
import com.example.digitalwallet.PocketDetailsActivity;
import com.example.digitalwallet.PocketTransferActivity;
import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PocketsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View tvWantAnother, btnClosedPockets;
    private View cardAllPurpose, cardSavings;

    private DatabaseReference mDatabase;
    private PocketAdapter adapter;
    private final List<Pocket> activePockets = new ArrayList<>();
    private final List<Pocket> closedPockets = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pockets, container, false);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        }

        recyclerView = view.findViewById(R.id.recyclerPockets);
        tvWantAnother = view.findViewById(R.id.tvWantAnother);
        btnClosedPockets = view.findViewById(R.id.btnClosedPockets);
        cardAllPurpose = view.findViewById(R.id.cardAllPurpose);
        cardSavings = view.findViewById(R.id.cardSavings);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PocketAdapter(activePockets);
        recyclerView.setAdapter(adapter);

        View.OnClickListener startAllPurposeFlow = v -> {
            Intent intent = new Intent(getActivity(), CreatePocketNameActivity.class);
            intent.putExtra("pocket_type", "All-purpose");
            startActivity(intent);
        };

        cardAllPurpose.setOnClickListener(startAllPurposeFlow);

        cardSavings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePocketNameActivity.class);
            intent.putExtra("pocket_type", "Savings");
            startActivity(intent);
        });

        btnClosedPockets.setOnClickListener(v -> showClosedPocketsMenu());

        loadPockets();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getIntent().getBooleanExtra("show_pocket_success", false)) {
            showSuccessPopup();
            getActivity().getIntent().removeExtra("show_pocket_success");
        }
    }

    private void loadPockets() {
        if (mDatabase == null) return;
        mDatabase.child("pockets").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                activePockets.clear();
                closedPockets.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Pocket p = data.getValue(Pocket.class);
                    if (p != null) {
                        if (p.isClosed) closedPockets.add(p);
                        else activePockets.add(p);
                    }
                }
                adapter.notifyDataSetChanged();

                if (activePockets.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvWantAnother.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvWantAnother.setVisibility(View.VISIBLE);
                }

                btnClosedPockets.setVisibility(closedPockets.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showClosedPocketsMenu() {
        if (getContext() == null || closedPockets.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.layout_closed_pockets_sheet, null);
        dialog.setContentView(view);

        LinearLayout container = view.findViewById(R.id.closedPocketsContainer);
        for (Pocket p : closedPockets) {
            View item = getLayoutInflater().inflate(R.layout.item_pocket_minimal, container, false);
            ((TextView) item.findViewById(R.id.tvPocketName)).setText(p.name);
            ImageView icon = item.findViewById(R.id.imgPocketIcon);
            if (p.iconName != null) {
                int resId = getResources().getIdentifier(p.iconName, "drawable", getContext().getPackageName());
                if (resId != 0) icon.setImageResource(resId);
            }
            
            item.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(getActivity(), PocketDetailsActivity.class);
                intent.putExtra("pocket_id", p.id);
                startActivity(intent);
            });
            container.addView(item);
        }

        dialog.show();
    }

    private void showSuccessPopup() {
        if (getContext() == null || activePockets.isEmpty()) return;
        
        Pocket latestPocket = activePockets.get(activePockets.size() - 1);
        
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.layout_pocket_success_sheet, null);
        bottomSheetDialog.setContentView(view);
        
        view.findViewById(R.id.btnAddMoney).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(getActivity(), PocketTransferActivity.class);
            intent.putExtra("pocket_id", latestPocket.id);
            intent.putExtra("pocket_name", latestPocket.name);
            intent.putExtra("mode", "deposit");
            startActivity(intent);
        });
        
        view.findViewById(R.id.btnViewPocket).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(getActivity(), PocketDetailsActivity.class);
            intent.putExtra("pocket_id", latestPocket.id);
            startActivity(intent);
        });
        
        bottomSheetDialog.show();
    }

    private class PocketAdapter extends RecyclerView.Adapter<PocketAdapter.ViewHolder> {
        final List<Pocket> list;
        PocketAdapter(List<Pocket> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pocket, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Pocket p = list.get(position);
            holder.name.setText(p.name);
            holder.amount.setText(String.format("₪ %.2f", p.amount));
            holder.type.setText(p.type + " pocket");

            if (p.iconName != null) {
                int resId = getResources().getIdentifier(p.iconName, "drawable", getContext().getPackageName());
                if (resId != 0) holder.icon.setImageResource(resId);
            }

            if ("Savings".equals(p.type)) {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.pocket_savings_tint));
            } else {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.white_card));
            }

            holder.imgLockBadge.setVisibility(p.isLocked ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), PocketDetailsActivity.class);
                intent.putExtra("pocket_id", p.id);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView amount;
            final TextView type;
            final ImageView icon;
            final ImageView imgLockBadge;
            final MaterialCardView cardView;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvPocketName);
                amount = v.findViewById(R.id.tvPocketAmount);
                type = v.findViewById(R.id.tvPocketType);
                icon = v.findViewById(R.id.imgPocketIcon);
                imgLockBadge = v.findViewById(R.id.imgLockBadge);
                cardView = v.findViewById(R.id.pocketCardView);
            }
        }
    }
}
