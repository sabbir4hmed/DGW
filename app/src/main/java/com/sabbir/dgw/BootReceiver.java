package com.sabbir.dgw;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, scheduling DataSenderWorker");
            OneTimeWorkRequest dataSenderWork = new OneTimeWorkRequest.Builder(DataSendWorker.class)
                    .build();
            WorkManager.getInstance(context).enqueue(dataSenderWork);
        }
    }
}
