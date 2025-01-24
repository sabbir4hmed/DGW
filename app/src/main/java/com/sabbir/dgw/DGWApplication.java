package com.sabbir.dgw;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.io.File;

public class DGWApplication extends Application implements Configuration.Provider{

    private static final String TAG = "DGWApplication";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}