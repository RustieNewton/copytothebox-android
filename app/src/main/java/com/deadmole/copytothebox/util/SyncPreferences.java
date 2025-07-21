package com.deadmole.copytothebox.util;
import android.content.SharedPreferences;
import android.content.Context;

public class SyncPreferences {

    private static SyncPreferences instance;
    private final SharedPreferences settings;

    // onboarding flags
    private static final String REGISTERED_FLAG   = "isRegistered";
    private static final String FILE_ACCESS_FLAG  = "fileAccessGranted";
    private static final String NOTIFICATION_FLAG = "notificationsGranted";
    private static final String KEY_DEFAULTS_SET = "defaults_set";  

    private SyncPreferences(Context context) {
        this.settings = context.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        setDefaults();
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new SyncPreferences(context);
        }
    }

    public static SyncPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SyncPreferences is not initialized. Call init(context) in Application class.");
        }
        return instance;
    }
    //set defaults
    private void setDefaults() {
        if (!settings.getBoolean(KEY_DEFAULTS_SET, false)) {
            SharedPreferences.Editor editor = settings.edit();

            // onboarding 
            editor.putBoolean(REGISTERED_FLAG, false);
            editor.putBoolean(FILE_ACCESS_FLAG, false);
            editor.putBoolean(NOTIFICATION_FLAG, false);
            // app status
            editor.putBoolean("isEnabled",false); //MainActivity
            editor.putLong("nextRun", 0);
            editor.putLong("lastRun",0);
            editor.putInt("syncInterval", 1); 

            // comms status
            editor.putBoolean("useGSM", false); 

            // Mark that defaults have been set
            editor.putBoolean(KEY_DEFAULTS_SET, true);
            editor.apply();
        }
    }
    /**
     * Convenience: All onboarding steps completed?
     */
    public boolean isSetupComplete() {
        return getRegisteredFlag()
                && getNotificationFlag()
                && getFileAccessFlag();
    }
    // FILE ACCESS PERMISSION flag
    public void setFileAccessFlag(Boolean setThis) {
        settings.edit().putBoolean(FILE_ACCESS_FLAG, setThis).apply();
    }
    public boolean getFileAccessFlag() {
        //get constants name
        return settings.getBoolean(FILE_ACCESS_FLAG, false);
    }
    // NOTIFICATIONS PERMISSION flag
    public void setNotificationFlag(Boolean setThis) {
        settings.edit().putBoolean(NOTIFICATION_FLAG, setThis).apply();
    }
    public boolean getNotificationFlag() {
        return settings.getBoolean(NOTIFICATION_FLAG, false);
    }
    // REGISTRATION flag
    public void setRegisteredFlag() {
        settings.edit().putBoolean(REGISTERED_FLAG, true ).apply();
    }
    public boolean getRegisteredFlag() {
        return settings.getBoolean(REGISTERED_FLAG, false);
    }

    // APP ENABLED flag
    public void setSyncEnabled( boolean isEnabled) {
        settings.edit().putBoolean("isEnabled",isEnabled).apply();
    }
    public boolean getSyncEnabled() {
        return settings.getBoolean("isEnabled", false);
    }

    // RUN TIMES
    public long getNextRun() {
        return settings.getLong("nextRun", 0);
    }
    public void updateNextRun() {
        // the runner is called every time the screen is on
        // but that is too many times to run the sync
        // so if we set a std 1 hr as the next run, and check it, it will be called more often
        // probably but only fire after an hour
        long currentTime = System.currentTimeMillis();
        int minimumInterval = getSyncInterval();// Default to 1 hour if missing
        long nextTime = currentTime + minimumInterval * 3600000L; //millis
        settings.edit().putLong("nextRun", nextTime).apply();
    }
    public long getLastRun() {
        return settings.getLong("lastRun", 0);
    }
    public void updateLastRun() {
        long currentTime = System.currentTimeMillis();
        // Update lastRun as long
        settings.edit().putLong("lastRun", currentTime).apply();
    }
    public void updateRuntimes() {
        updateLastRun();
        updateNextRun();
    }

    // use GSM 
    public void setUseGSM( boolean setThis) {
        settings.edit().putBoolean("useGSM", setThis).apply();
    }
    public boolean getUseGSM() {
        return settings.getBoolean("useGSM", false);
    }

    //SYNC INTERVAL
    public void setSyncInterval(int hours) {
        settings.edit().putInt("syncInterval", hours).apply();
    }
    public int getSyncInterval() {
        return settings.getInt("syncInterval", 1);
    }


}
