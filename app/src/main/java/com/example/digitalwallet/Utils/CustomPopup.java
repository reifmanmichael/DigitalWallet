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
        
        // Ensure background of the dialog is transparent so the rounded corners of the layout show
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
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
    
    public static void show(Context context, String message) {
        show(context, "Notification", message, null);
    }
}