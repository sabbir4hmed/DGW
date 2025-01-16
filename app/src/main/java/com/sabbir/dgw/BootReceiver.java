package com.sabbir.dgw;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
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

        // Handle boot events efficiently
        if (action != null && isValidBootAction(action)) {
            Log.d(TAG, "Boot completed, initializing DataSenderWorker");
            initializeWorkManager(context);
            startDataService(context);
        }
    }

    private boolean isValidBootAction(String action) {
        return action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                action.equals("android.intent.action.QUICKBOOT_POWERON") ||
                action.equals(Intent.ACTION_REBOOT);
    }

    public static void initializeWorkManager(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest dataSenderWork = new PeriodicWorkRequest.Builder(
                DataSendWorker.class,
                15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)  // Add initial delay for better boot performance
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DataSenderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                dataSenderWork);
    }

    private void startDataService(Context context) {
        OneTimeWorkRequest immediateWork = new OneTimeWorkRequest.Builder(DataSendWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(immediateWork);
    }
}

