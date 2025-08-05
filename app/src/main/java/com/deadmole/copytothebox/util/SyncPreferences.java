package com.deadmole.copytothebox.util;
import android.content.SharedPreferences;
import android.content.Context;
import com.deadmole.copytothebox.BuildConfig;

public class SyncPreferences {

    private static SyncPreferences instance;
    protected final SharedPreferences settings; // protected allows access for MobileSyncPreferences sub class

    // default onboarding flags
    private static final String FILE_ACCESS_FLAG  = "fileAccessGranted";
    private static final String NOTIFICATION_FLAG = "notificationsGranted";
    private static final String REGISTERED_FLAG   = "isRegistered";

    private static final String KEY_DEFAULTS_SET = "defaults_set";  

    // PHONE perm flags
    private static final String CALL_ACCESS_FLAG = "callAccessGranted";
    private static final String SMS_ACCESS_FLAG = "smsAccessGranted";

    // PHONE settings
    private static final String MOBILE_DEFAULTS_SET = "mobile_defaults_set"; 
    
    public SyncPreferences(Context context) {
        this.settings = context.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        setDefaults();
        if( BuildConfig.FLAVOR.equals("phone")) {
             setMobileDefaults();      // sets phone-only defaults
        }
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

    //set defaults - sets first time only
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

            // Mark that defaults have been set
            editor.putBoolean(KEY_DEFAULTS_SET, true);
            editor.apply();

            //set mobile defaults if required
            setMobileDefaults();
        }
    }
    //set defaults
    private void setMobileDefaults() {
        if(!BuildConfig.FLAVOR.equals("phone")) return;

        if (!settings.getBoolean(MOBILE_DEFAULTS_SET, false)) {
            SharedPreferences.Editor editor = settings.edit();
            // set default perm
            editor.putBoolean(CALL_ACCESS_FLAG, false); // granted perm?
            editor.putBoolean(SMS_ACCESS_FLAG,false); //granted perm?

            //set other phone settings
            editor.putBoolean("useGSM", false); // use gms?
            editor.putBoolean("pushCalls",false); // push calls?
            editor.putLong("lastCallPush",0);
            editor.putBoolean("pushSMS",false); // push sms?
            editor.putLong("lastSmsPush",0);

            // Mark that defaults have been set
            editor.putBoolean(MOBILE_DEFAULTS_SET, true);
            editor.apply();
        }
    }
    /* Convenience: All onboarding steps completed */
    public boolean isSetupComplete() {
        if (BuildConfig.FLAVOR.equals("phone")) {
            // 5 tests
            return getRegisteredFlag()
                    && getNotificationFlag()
                    && getFileAccessFlag()
                    && getCallAccessFlag()
                    && getSmsAccessFlag();
        } else {
            // 3 tests
            return getRegisteredFlag()
                    && getNotificationFlag()
                    && getFileAccessFlag();
        }
    }


    // FILE ACCESS PERMISSION flag
    public void setFileAccessFlag(Boolean setThis) {
        settings.edit().putBoolean(FILE_ACCESS_FLAG, setThis).apply();
    }
    public boolean getFileAccessFlag() {
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
    //SYNC INTERVAL
    public void setSyncInterval(int hours) {
        settings.edit().putInt("syncInterval", hours).apply();
    }
    public int getSyncInterval() {
        return settings.getInt("syncInterval", 1);
    }
    
    /*  PERMISSIONS X 2   */ 
    public void setCallAccessFlag(Boolean setThis) {
        settings.edit().putBoolean(CALL_ACCESS_FLAG, setThis).apply();
    }
    public boolean getCallAccessFlag() {
        return settings.getBoolean(CALL_ACCESS_FLAG, false);
    }
    // sms
    public void setSmsAccessFlag(Boolean setThis) {
        settings.edit().putBoolean(SMS_ACCESS_FLAG, setThis).apply();
    }
    public boolean getSmsAccessFlag() {
        return settings.getBoolean(SMS_ACCESS_FLAG, false);
    }

    /*  CALL LOGS  */
    public void setPushCalls( boolean setThis) {
        settings.edit().putBoolean("pushCalls", setThis).apply();
    }
    public boolean getPushCalls() {
        return settings.getBoolean("pushCalls", false);
    }
    /* last call log push time */
    public long getLastCallPush() {
        return settings.getLong("lastCallPush", 0);
    }
    public void setLastCallPush() {
        long currentTime = System.currentTimeMillis();
        settings.edit().putLong("lastCallPush", currentTime).apply();
    }


    /*  SMS  messages  */
    public void setPushSMS( boolean setThis) {
        settings.edit().putBoolean("pushSMS", setThis).apply();
    }
    public boolean getPushSMS() {
        return settings.getBoolean("pushSMS", false);
    }
    /* last SMS push time */
    public long getLastSmsPush() {
        return settings.getLong("lastSmsPush", 0);
    }
    public void setLastSmsPush() {
        long currentTime = System.currentTimeMillis();
        settings.edit().putLong("lastSmsPush", currentTime).apply();
    }

    // use GSM 
    public void setUseGSM( boolean setThis) {
        settings.edit().putBoolean("useGSM", setThis).apply();
    }
    public boolean getUseGSM() {
        return settings.getBoolean("useGSM", false);
    }

}
