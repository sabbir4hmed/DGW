package com.sabbir.dgw;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(android.R.layout.activity_main);

        BootReceiver.scheduleWork(this);

        // Request package query permission
        String[] permissions = {
                android.Manifest.permission.QUERY_ALL_PACKAGES
        };

        if (!hasPermissions(permissions)) {
            androidx.core.app.ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }

        // Schedule work to run every 15 minutes
        PeriodicWorkRequest dataSenderWork = new PeriodicWorkRequest.Builder(DataSendWorker.class,
                1, TimeUnit.DAYS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                finish();
            }
        }
    }
}
