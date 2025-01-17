package com.sabbir.dgw;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        if (action != null && (
                action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                        action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                        action.equals("android.intent.action.QUICKBOOT_POWERON") ||
                        action.equals(Intent.ACTION_REBOOT))) {

            Log.d(TAG, "Boot completed or package replaced, scheduling DataSenderWorker");
            scheduleWork(context);

            // Start immediate work
            startImmediateWork(context);
        }
    }

    public static void scheduleWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest dataSenderWork = new PeriodicWorkRequest.Builder(
                DataSendWorker.class,
                1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DataSenderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                dataSenderWork);
    }

    private void startImmediateWork(Context context) {
        Intent serviceIntent = new Intent(context, DataSendWorker.class);
        context.startService(serviceIntent);
    }
}