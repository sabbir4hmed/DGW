package com.sabbir.dgw;

import android.content.Context;

import java.io.File;

public class StorageUtils {
    public static void createWorkManagerDirectory(Context context) {
        File noBackupDir = new File(context.getApplicationInfo().dataDir, "no_backup");
        if (!noBackupDir.exists()) {
            noBackupDir.mkdirs();
        }
    }
}

