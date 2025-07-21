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

    //ensure that the ScreenOn and BootReceiver are enabled ON REBOOT
    @Override
    public void onReceive(Context context, Intent intent) {
        
        Context appContext = context.getApplicationContext();
        boolean debugging = Constants.DEBUG_MODE;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Logger.log(appContext, "BootReceiver: BOOT_COMPLETED signal received ...");

            //get app enabled state
            SyncPreferences.init(appContext);
            if (SyncPreferences.getInstance().getSyncEnabled()) {
                Logger.log(appContext, "App enabled.  Starting RunnerManager.enable() method");
                RunnerManager.enable(appContext);
            } else {
                Logger.log(appContext, "App disabled. Doing nothing.");
            }
        }
    }
}
