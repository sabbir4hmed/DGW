package com.sabbir.dgw;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataSendWorker extends Worker {
    private static final String TAG = "DataSenderWorker";
    private static final int MAX_RETRIES = 3;
    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzSHgcqlYJmcDMOc6bwE8XVxH2_OsrojJ357VjEbBLmIU74h9P9WSqo-7ax0-dVbP90nw/exec";

    public DataSendWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DataSenderWorker started");

        // Add delay to ensure network is ready
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        return sendDataToGoogleSheet();
    }

    private Result sendDataToGoogleSheet() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        String appListString = getInstalledAppsAsString();
        String modelString = getDeviceModel();
        String buildNumber = Build.DISPLAY;

        // Log the data being sent
        Log.d(TAG, "Sending data - Model: " + modelString);
        Log.d(TAG, "Build Number: " + buildNumber);
        Log.d(TAG, "Apps: " + appListString);

        FormBody formBody = new FormBody.Builder()
                .add("modelName", modelString)
                .add("appList", appListString)
                .add("buildNumber", buildNumber)
                .build();

        Request request = new Request.Builder()
                .url(SCRIPT_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .build();

        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            Log.d(TAG, "Response code: " + response.code());
            Log.d(TAG, "Response body: " + responseBody);

            if (response.isSuccessful()) {
                return Result.success();
            } else {
                Log.e(TAG, "Server error: " + response.code() + " - " + responseBody);
                return handleRetry();
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage(), e);
            return handleRetry();
        }
    }

    private Result handleRetry() {
        return Result.retry();
    }

    private String getDeviceModel() {
        return Build.MODEL.trim();
    }

    private String getInstalledAppsAsString() {
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> userApps = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            if (isUserApp(app)) {
                String appName = app.loadLabel(pm).toString().trim();
                if (!appName.isEmpty()) {
                    userApps.add(appName);
                }
            }
        }

        Log.d(TAG, "Found " + userApps.size() + " user apps");
        return TextUtils.join(",", userApps);
    }

    private boolean isUserApp(ApplicationInfo app) {
        return (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
    }
}