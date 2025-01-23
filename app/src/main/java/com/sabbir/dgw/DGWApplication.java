package com.sabbir.dgw;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.io.File;

public class DGWApplication extends Application {

    private static final String TAG = "DGWApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        initializeWorkManager();
    }

    private void initializeWorkManager() {
        try {
            // Create required directories
            createWorkManagerDirectories();

            // Initialize WorkManager with custom configuration
            Configuration config = new Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build();

            // Initialize WorkManager
            WorkManager.initialize(getApplicationContext(), config);

            Log.d(TAG, "WorkManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing WorkManager: " + e.getMessage());
        }
    }

    private void createWorkManagerDirectories() {
        File dataDir = new File(getApplicationInfo().dataDir);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        String[] directories = {
                "databases",
                "no_backup",
                "cache"
        };

        for (String dirName : directories) {
            File dir = new File(dataDir, dirName);
            if (!dir.exists()) {
                dir.mkdirs();
                Log.d(TAG, "Created directory: " + dir.getAbsolutePath());
            }
        }
    }
}

