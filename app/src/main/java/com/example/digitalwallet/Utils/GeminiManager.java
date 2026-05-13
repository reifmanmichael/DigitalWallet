package com.example.digitalwallet.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.content.ContextCompat;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import java.util.concurrent.Executor;


public class GeminiManager {
    private static GeminiManager instance;

    private static final String modelVersion = "gemini-2.5-flash-lite";
    private static final String TAG = "GeminiManager";
    private GeminiManager()
    {
    }

    public static GeminiManager getInstance()
    {
        if (null == instance) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public void sendText(String promptStr, Context context, GeminiCallback callback)
    {
        Log.d(TAG, "sendText: start");
        send(promptStr,null,null,null,context,callback);
        Log.d(TAG, "sendText: done");
    }

    private void send(String promptStr, Bitmap bitmap,byte[] bytes, String mimeType, Context context, GeminiCallback callback) {
        Log.d(TAG, "send: start");
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(modelVersion);
        GenerativeModelFutures model = GenerativeModelFutures.from(ai);

        Content.Builder builder = new Content.Builder();
        if(bitmap != null)
            builder.addImage(bitmap);
        if(bytes != null)
            builder.addInlineData(bytes, mimeType);

        Content prompt = builder.addText(promptStr).build();

        Executor executor = ContextCompat.getMainExecutor(context);
        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse result) {
                Log.d(TAG, "onSuccess: text: " + result.getText());
                callback.onSuccess(result.getText());
            }
            @Override public void onFailure(Throwable t) {
                Log.d(TAG, "onFailure: error: " + t.getMessage());
                callback.onError();
            }
        }, executor);
    }


    public interface GeminiCallback {
        public void onSuccess(String result);
        public void onError();

    }

}
