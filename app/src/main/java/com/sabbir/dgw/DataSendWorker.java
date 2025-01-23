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
    private static final int TIMEOUT = 120;

    public DataSendWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting data transmission - Device: " + Build.MODEL);
        return sendDataToGoogleSheet();
    }

    private Result sendDataToGoogleSheet() {
        OkHttpClient client = createOkHttpClient();
        FormBody formBody = createFormBody();
        Request request = createRequest(formBody);

        try {
            Response response = executeRequest(client, request);
            return handleResponse(response);
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage(), e);
            return Result.retry();
        }
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
                    Response response = chain.proceed(request);
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

        Log.d(TAG, "Preparing data - Model: " + modelString +
                ", Build: " + buildNumber +
                ", Android: " + androidVersion +
                ", Apps Count: " + appListString.split(",").length);

        return new FormBody.Builder()
                .add("modelName", modelString)
                .add("appList", appListString)
                .add("buildNumber", buildNumber)
                .add("androidVersion", androidVersion)
                .build();
    }

    private Request createRequest(FormBody formBody) {
        return new Request.Builder()
                .url(SCRIPT_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "DataSenderWorker/" + Build.MODEL)
                .build();
    }

    private Response executeRequest(OkHttpClient client, Request request) throws IOException {
        Log.d(TAG, "Executing network request");
        return client.newCall(request).execute();
    }

    private Result handleResponse(Response response) throws IOException {
        String responseBody = response.body() != null ? response.body().string() : "";
        int responseCode = response.code();

        Log.d(TAG, "Response received - Code: " + responseCode + ", Body: " + responseBody);

        if (response.isSuccessful()) {
            Log.d(TAG, "Data transmission successful");
            return Result.success();
        } else {
            Log.e(TAG, "Server error: " + responseCode + " - " + responseBody);
            return Result.retry();
        }
    }

    private String getInstalledAppsAsString() {
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> userApps = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = app.loadLabel(pm).toString().trim();
                String packageName = app.packageName;
                if (!appName.isEmpty()) {
                    userApps.add(appName + " (" + packageName + ")");
                }
            }
        }

        Log.d(TAG, "Found " + userApps.size() + " user-installed applications");
        return TextUtils.join(", ", userApps);
    }
}