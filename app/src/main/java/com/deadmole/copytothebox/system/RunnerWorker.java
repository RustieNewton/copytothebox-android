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

public class RunnerWorker extends Worker {

    public RunnerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        
        boolean debugging = Constants.DEBUG_MODE;
        Context appContext = getApplicationContext();
        boolean success = false;

        try {
            Logger.log(appContext, "RunnerWorker: calling Runner.run()");
            success = Runner.run(appContext);
        } catch (Exception e) {
            Logger.log(appContext, "RunnerWorker: runner call failed");
        }

        Logger.log(appContext, "RunnerWorker: finished with result: " + success);

        if (success) {
            RunnerManager.scheduleFallbackJob(appContext); // schedule next fallback
            return Result.success();
        } else {
            return Result.retry(); // Let WorkManager retry on its own schedule
        }
    }
}
