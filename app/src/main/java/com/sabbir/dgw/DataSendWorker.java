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
    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxY72j5eDtr_YLZoNrRJFmP9HxKo0X_I6uzdNCMh0WdXVIjwds7plcg_IQp8dDmWzfX_A/exec";
    private static final int TIMEOUT = 180; // Increased timeout for better reliability

    public DataSendWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting data transmission - Device: " + Build.MODEL);

        // Add retry count check
        int runAttemptCount = getRunAttemptCount();
        if (runAttemptCount > 3) {
            Log.w(TAG, "Too many retry attempts: " + runAttemptCount);
            return Result.failure();
        }

        return sendDataToGoogleSheet();
    }

    private OkHttpClient createOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    Log.d(TAG, "Sending request to: " + request.url());

                    // Add connection pooling
                    Request newRequest = request.newBuilder()
                            .header("Connection", "keep-alive")
                            .build();

                    Response response = chain.proceed(newRequest);
                    Log.d(TAG, "Response code: " + response.code());
                    return response;
                })
                .build();
    }

    private FormBody createFormBody() {
        String appListString = getInstalledAppsAsString();
        String modelString = Build.MODEL.trim();
        String buildNumber = Build.DISPLAY;
        String androidVersion = Build.VERSION.RELEASE;
        String timestamp = String.valueOf(System.currentTimeMillis());

        return new FormBody.Builder()
                .add("modelName", modelString)
                .add("appList", appListString)
                .add("buildNumber", buildNumber)
                .add("androidVersion", androidVersion)
                .add("timestamp", timestamp)
                .build();
    }

    private String getInstalledAppsAsString() {
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> userApps = new ArrayList<>();

        try {
            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    String appName = app.loadLabel(pm).toString().trim();
                    String packageName = app.packageName;
                    if (!appName.isEmpty()) {
                        userApps.add(appName + " (" + packageName + ")");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app list", e);
        }

        return TextUtils.join(", ", userApps);
    }

    private Result handleResponse(Response response) throws IOException {
        if (response.body() == null) {
            return Result.retry();
        }

        String responseBody = response.body().string();
        int responseCode = response.code();

        if (responseCode >= 200 && responseCode < 300) {
            Log.d(TAG, "Data transmission successful");
            return Result.success();
        } else if (responseCode >= 500) {
            Log.e(TAG, "Server error: " + responseCode);
            return Result.retry();
        } else {
            Log.e(TAG, "Client error: " + responseCode);
            return Result.failure();
        }
    }

    private Result sendDataToGoogleSheet() {
        OkHttpClient client = createOkHttpClient();
        FormBody formBody = createFormBody();
        Request request = new Request.Builder()
                .url(SCRIPT_URL)
                .post(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            return handleResponse(response);
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}