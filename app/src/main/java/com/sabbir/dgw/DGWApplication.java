package com.sabbir.dgw;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class DGWApplication extends Application implements Configuration.Provider{

    private static final String TAG = "DGWApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest dataWork = new PeriodicWorkRequest.Builder(
                DataSendWorker.class,
                15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        10,
                        TimeUnit.MINUTES
                )
                .setInitialDelay(0, TimeUnit.SECONDS)
                .addTag("periodic_data_sender")
                .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "DataSenderWork",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        dataWork
                );
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}

