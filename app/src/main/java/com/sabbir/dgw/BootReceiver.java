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
            scheduleWork(context);
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
            OneTimeWorkRequest initWork = new OneTimeWorkRequest.Builder(DataSendWorker.class)
                    .setInitialDelay(2, TimeUnit.MINUTES)  // Changed to 2 minutes
                    .addTag("init_work")
                    .build();

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                            "init_data_sender",
                            ExistingWorkPolicy.REPLACE,
                            initWork
                    );
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling work", e);
        }
    }
}