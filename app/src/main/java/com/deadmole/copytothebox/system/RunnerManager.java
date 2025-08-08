// File: com/deadmole/copytothebox/system/RunnerManager.java
package com.deadmole.copytothebox.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.Constants;
import com.deadmole.copytothebox.util.SyncPreferences;
/*
    this is run
    set new workManager job after
        1 enable in ui
        2 after a run when pressing run now button
        3 after succ run init by WorkManager
        4 after changing sync interval in ChoicesFragment
        5 after reboot in BootReceiver
*/
public class RunnerManager {

    private static final String UNIQUE_WORK_NAME = "daily-runner-work";
    private static final Boolean debugging = Constants.DEBUG_MODE;

    public static void enable(Context context) {

        Context appContext = context.getApplicationContext();
        if(debugging) Logger.log(appContext, "RunnerManager: enabling...");

        // init prefs
        SyncPreferences.init(appContext);

        // revised 2 aug after chatgpt realised that the old receiver DIED as soon as the UI was closed
        // revised 4 aug after chatgpt relised that screen on receivers won't really work
        // "So, a manifest-declared receiver won’t reliably receive SCREEN_ON/OFF anymore on most modern devices".
        //You can still listen for these events, but you must *dynamically* register the receiver while your app or a service is running:

        // requires additional declaration in manifest
        // 1) Enable manifest-declared ScreenOnReceiver component so it persists after UI close
        // if(debugging) Logger.log(appContext, "RunnerManager: enabling manifest ScreenOnReceiver component...");
        // PackageManager pm = appContext.getPackageManager();
        // ComponentName receiver = new ComponentName(appContext, ScreenOnReceiver.class);
        // pm.setComponentEnabledSetting(
        //         receiver,
        //         PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        //         PackageManager.DONT_KILL_APP
        // );

        // 2) Schedule WorkManager job
        if(debugging) Logger.log(appContext, "RunnerManager: scheduling fallback OneTimeWorkRequest ...");
        scheduleNextJob(appContext); //usage is good

        // update prefs for enabled status
        SyncPreferences.getInstance().setSyncEnabled(true);
        if(debugging) Logger.log(appContext, "RunnerManager: finished enabling");
    }

    public static void disable(Context context) {
        Context appContext = context.getApplicationContext();
        if(debugging) Logger.log(appContext, "RunnerManager: disabling...");

        //init prefs
        SyncPreferences.init(appContext);

        // Disable ScreenOnReceiver component in manifest
        // if(debugging) Logger.log(appContext, "RunnerManager: disabling manifest ScreenOnReceiver component...");
        // PackageManager pm = appContext.getPackageManager();
        // ComponentName receiver = new ComponentName(appContext, ScreenOnReceiver.class);
        // pm.setComponentEnabledSetting(
        //         receiver,
        //         PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        //         PackageManager.DONT_KILL_APP
        // );

        if(debugging) Logger.log(appContext, "RunnerManager: cancelling WorkManager job ...");
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_NAME);

        //update prefs
        SyncPreferences.getInstance().setSyncEnabled(false);

        if(debugging) Logger.log(appContext, "RunnerManager: finished disabling");
    }
    // new vv 4 Aug
    // called by ChoicesFragment and Runner.run
    public static void scheduleNextJob(Context context) {

        Context appContext = context.getApplicationContext();

        //init prefs
        SyncPreferences.init(appContext);

        //get sync interval
        long intervalHours = SyncPreferences.getInstance().getSyncInterval();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RunnerWorker.class)
            .setInitialDelay(intervalHours, TimeUnit.HOURS)
            .setConstraints(
                new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    // .setRequiresDeviceIdle(true)  // avoid unless mandatory
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30, TimeUnit.MINUTES
            )
            .build();

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE, // remove existing job, replace with this one
                request
            );

        if (debugging) Logger.log(appContext, "RunnerManager: scheduled next job in " + intervalHours + " hours");
    }

}