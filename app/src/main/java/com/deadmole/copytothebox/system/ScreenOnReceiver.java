// File: com/deadmole/copytothebox/system/ScreenOnReceiver.java
package com.deadmole.copytothebox.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.util.Log; may need for err logging

import com.deadmole.copytothebox.runner.Runner;
import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.Constants;

public class ScreenOnReceiver extends BroadcastReceiver {
    private static final Boolean debugging = Constants.DEBUG_MODE;

    @Override
    public void onReceive(Context context, Intent intent) {
        // get context into an obj
        Context appContext = context.getApplicationContext();
        // FIXME add some more output here ... 
        if(debugging) Logger.log(appContext, "ScreenOnReceiver: Screen turned on, testing for intent ..."+Intent.ACTION_SCREEN_ON);

        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            if(debugging) Logger.log(appContext,"calling Runner from ScreenOnReceiver");
            Runner.run(appContext);
        }
    }
}
