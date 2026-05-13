package com.example.digitalwallet.Utils;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.example.digitalwallet.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class CustomPopup {

    public interface OnPopupDismissListener {
        void onDismiss();
    }

    public static void show(Context context, String title, String message, OnPopupDismissListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = View.inflate(context, R.layout.layout_custom_popup, null);
        dialog.setContentView(view);

        int bottomSheetId = context.getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
        View bottomSheet = dialog.findViewById(bottomSheetId);

        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tvPopupTitle);
        TextView tvMessage = view.findViewById(R.id.tvPopupMessage);
        View btnOk = view.findViewById(R.id.btnPopupOk);

        tvTitle.setText(title);
        tvMessage.setText(message);

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onDismiss();
        });

        dialog.show();
    }
    
    public static void show(Context context, String title, String message) {
        show(context, title, message, null);
    }

}