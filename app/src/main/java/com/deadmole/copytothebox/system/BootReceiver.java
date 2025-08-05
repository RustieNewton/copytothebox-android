// File: com/deadmole/copytothebox/system/BootReceiver.java

package com.deadmole.copytothebox.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.util.Log;

//app modules
import com.deadmole.copytothebox.util.SyncPreferences;
import com.deadmole.copytothebox.util.Constants;
import com.deadmole.copytothebox.util.Logger;


public class BootReceiver extends BroadcastReceiver {

    //ensure that the ScreenOn and WorkManager are enabled ON REBOOT
    @Override
    public void onReceive(Context context, Intent intent) {
        
        Context appContext = context.getApplicationContext();
        boolean debugging = Constants.DEBUG_MODE;

        // bring the action
        String action = intent != null ? intent.getAction() : "null";
        if(debugging) Logger.log(appContext, "BootReceiver: received intent action: " + action);

        assert intent != null;
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            if(debugging) Logger.log(appContext, "BootReceiver: BOOT_COMPLETED signal received ...");

            //get app enabled state
            SyncPreferences.init(appContext);
            if (SyncPreferences.getInstance().getSyncEnabled()) {
                if(debugging) Logger.log(appContext, "App enabled.  Starting RunnerManager.enable() method");
                RunnerManager.enable(appContext);
            } else {
                if(debugging) Logger.log(appContext, "App disabled. Doing nothing.");
            }
        } 
        else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Logger.log(appContext, "BootReceiver: PACKAGE_REPLACED signal received ...");
            if (SyncPreferences.getInstance().getSyncEnabled()) {
                if(debugging) Logger.log(appContext, "App enabled after update. Starting RunnerManager.enable()");
                RunnerManager.enable(appContext);
            }
        }
        else {
            if(debugging) Logger.log(appContext, "BootReceiver: Unexpected intent, ignoring.");
        }
    }
}
