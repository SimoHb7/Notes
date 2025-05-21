package com.example.notes;

import android.app.Application;
import android.util.Log;
import com.example.notes.api.RetrofitClient;

public class NotesApplication extends Application {
    private static final String TAG = "NotesApplication";

    @Override
    public void onCreate() {
        try {
            Log.d(TAG, "Initializing NotesApplication");
            super.onCreate();
            Log.d(TAG, "Calling RetrofitClient.init");
            RetrofitClient.init(this);
            Log.d(TAG, "NotesApplication initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing NotesApplication: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize NotesApplication", e);
        }
    }
} 