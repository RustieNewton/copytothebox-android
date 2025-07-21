// File: com/deadmole/copytothebox/system/RunnerManager.java
package com.deadmole.copytothebox.system;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.Constants;
import com.deadmole.copytothebox.util.SyncPreferences;

public class RunnerManager {

    private static final String UNIQUE_WORK_NAME = "daily-runner-work";
    private static ScreenOnReceiver screenOnReceiver;
    private static final Boolean debugging = Constants.DEBUG_MODE;

    public static void enable(Context context) {
        Context appContext = context.getApplicationContext();
        Logger.log(appContext, "RunnerManager: enabling...");

        // init prefs
        SyncPreferences.init(appContext);

        // 1) Register screen on receiver
        if (screenOnReceiver == null) {
            Logger.log(appContext, "RunnerManager: registering ScreenOnReceiver ...");
            screenOnReceiver = new ScreenOnReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            appContext.registerReceiver(screenOnReceiver, filter);
        }

        // 2) Schedule fallback OneTime WorkManager job
        Logger.log(appContext, "RunnerManager: scheduling fallback OneTimeWorkRequest ...");
        scheduleFallbackJob(context);

        // update prefs for enabled status
        SyncPreferences.getInstance().setSyncEnabled(true);
        Logger.log(appContext, "RunnerManager: finished enabling");
    }

    public static void disable(Context context) {
        Context appContext = context.getApplicationContext();
        Logger.log(appContext, "RunnerManager: disabling...");

        //init prefs
        SyncPreferences.init(appContext);

        if (screenOnReceiver != null) {
            Logger.log(appContext, "RunnerManager: tearing down ScreenOnReceiver...");
            try {
                appContext.unregisterReceiver(screenOnReceiver);
            } catch (IllegalArgumentException ignored) {}
            screenOnReceiver = null;
        }

        Logger.log(appContext, "RunnerManager: cancelling fallback WorkManager job ...");
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_NAME);

        //update prefs
        SyncPreferences.getInstance().setSyncEnabled(false);

        Logger.log(appContext, "RunnerManager: finished disabling");
    }

    //helper method, called by this class and RunnerWorker
    public static void scheduleFallbackJob(Context context) {
        Context appContext = context.getApplicationContext();
        // Logger.log(appContext, "RunnerManager: scheduling fallback OneTimeWorkRequest ...");
        
        //init prefs
        SyncPreferences.init(appContext);

        // use the user's pref for sync interval
        long intervalHours = SyncPreferences.getInstance().getSyncInterval(); 

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RunnerWorker.class)
            .setInitialDelay(intervalHours, TimeUnit.HOURS)
            .setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .build())
            .build();

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            );
    }

}
