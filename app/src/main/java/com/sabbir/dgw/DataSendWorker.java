package com.sabbir.dgw;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataSendWorker extends Worker {
    private static final String TAG = "DataSenderWorker";
    private static final int MAX_RETRIES = 3;
    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxY72j5eDtr_YLZoNrRJFmP9HxKo0X_I6uzdNCMh0WdXVIjwds7plcg_IQp8dDmWzfX_A/exec";
    private int retryCount = 0;

    public DataSendWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DataSenderWorker started");
        return sendDataToGoogleSheet();
    }

    private Result sendDataToGoogleSheet() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        String appListString = getInstalledAppsAsString();
        String modelString = getDeviceModel();
        String buildNumber = Build.DISPLAY;

        FormBody formBody = new FormBody.Builder()
                .add("modelName", modelString)
                .add("appList", appListString)
                .add("buildNumber", buildNumber)
                .build();

        Request request = new Request.Builder()
                .url(SCRIPT_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Data sent successfully. Response: " + responseBody);
                return Result.success();
            } else {
                Log.e(TAG, "Server error: " + response.code());
                return handleRetry();
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage());
            return handleRetry();
        }
    }

    private Result handleRetry() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            return Result.retry();
        }
        return Result.failure();
    }

    private String getDeviceModel() {
        return String.format("%s", Build.MODEL);
    }

    private String getInstalledAppsAsString() {
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        StringBuilder appList = new StringBuilder();
        int count = 0;

        for (ApplicationInfo app : apps) {
            if (isUserApp(app)) {
                if (count > 0) {
                    appList.append(",");
                }
                appList.append(app.loadLabel(pm).toString());
                count++;
            }
        }

        Log.d(TAG, String.format("Found %d user-installed apps", count));
        return appList.toString();
    }

    private boolean isUserApp(ApplicationInfo app) {
        return (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
    }
}
