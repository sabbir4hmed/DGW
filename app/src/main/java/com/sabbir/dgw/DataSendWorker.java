package com.sabbir.dgw;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DataSendWorker extends Worker {

    private static final String TAG = "DataSenderWorker";
    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;

    public DataSendWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DataSenderWorker started");
        sendDataToGoogleSheet();
        return Result.success();
    }

    private void sendDataToGoogleSheet() {
        OkHttpClient client = new OkHttpClient();
        String url = "https://script.google.com/macros/s/AKfycbwJVtBf_6NyB311XytfvJvS4rzpZ2taP52mU6I44mqphuTDT6Fe8vvGoehPu6vtVYa1Fw/exec";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("modelName", Build.MODEL);
            jsonBody.put("appList", getInstalledAppsAsString());
            Log.d(TAG, "Prepared JSON body: " + jsonBody.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON body", e);
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Log.d(TAG, "Sending request to: " + url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed", e);
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    Log.d(TAG, "Retrying... Attempt " + retryCount);
                    sendDataToGoogleSheet();
                } else {
                    Log.e(TAG, "Max retries reached. Giving up.");
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response: " + responseBody);
                } else {
                    Log.e(TAG, "Unsuccessful response: " + response.code() + " " + response.message());
                    String responseBody = response.body().string();
                    Log.e(TAG, "Response body: " + responseBody);
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                        Log.d(TAG, "Retrying... Attempt " + retryCount);
                        sendDataToGoogleSheet();
                    } else {
                        Log.e(TAG, "Max retries reached. Giving up.");
                    }
                }
            }
        });

    }

    private String getInstalledAppsAsString() {
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        StringBuilder appList = new StringBuilder();

        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (appList.length() > 0) {
                    appList.append(",");
                }
                appList.append(app.loadLabel(pm).toString());
            }
        }

        Log.d(TAG, "Installed apps: " + appList.toString());
        return appList.toString();
    }
    }





