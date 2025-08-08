// File: com/deadmole/copytothebox/system/RunnerWorker.java
package com.deadmole.copytothebox.system;

import android.content.Context;
//import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.deadmole.copytothebox.runner.Runner;
import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.Constants;

// this is the class that the system runs
public class RunnerWorker extends Worker {

    public RunnerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    // new vv 4 aug
    @NonNull
    @Override
    public Result doWork() {
        boolean debugging = Constants.DEBUG_MODE;
        Context appContext = getApplicationContext();
        boolean success = false;

        if(debugging) Logger.log(appContext, "RunnerWorker: started ... ");

        try {
            // Optional pre-check: skip if not due
            if (!Runner.isItTimeToRun(appContext)) {
                if(debugging) Logger.log(appContext, "RunnerWorker: sync not due, skipping execution.");
                RunnerManager.scheduleNextJob(appContext); //appContext is good
                return Result.success();
            }

            if(debugging) Logger.log(appContext, "RunnerWorker: calling Runner.run()");
            success = Runner.run(appContext);

        } catch (Exception e) {
            //RunnerWorker: runner call failed - SyncPreferences is not initialized. Call init(context) in Application class.

            if(debugging) Logger.log(appContext, "RunnerWorker: runner call failed - " + e.getMessage());
        }

        if(debugging) Logger.log(appContext, "RunnerWorker: finished with result: " + success);

        // Always schedule the next job to maintain the chain
        RunnerManager.scheduleNextJob(appContext); // good usage

        // Let WorkManager retry earlier if needed
        return success ? Result.success() : Result.retry();
    }
}
