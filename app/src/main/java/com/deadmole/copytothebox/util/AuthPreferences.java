package com.deadmole.copytothebox.util;
import android.content.SharedPreferences;
import android.content.Context;

public class AuthPreferences {
 
    private static AuthPreferences instance;
    private final SharedPreferences auth;

    //get the SharedPreferences data called auth
    private AuthPreferences(Context context) {
        this.auth = context.getApplicationContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new AuthPreferences(context);
        }
    }

    public static AuthPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AuthPreferences is not initialized. Call init(context) in onCreate class.");
        }
        return instance;
    }
    // methods
    public String getRsyncID() {
        return auth.getString("rsyncID", null);
    }

    public void setRsyncID(String rsyncID) {
        auth.edit().putString("rsyncID", rsyncID).apply();
    }

    public String getServerID() {
        return auth.getString("serverID", null);
    }

    public void setServerID(String serverID) {
        auth.edit().putString("serverID", serverID).apply();
    }

    public String getPublicDomain() {
        return auth.getString("publicDomain", null);
    }

    public void setPublicDomain(String publicDomain) {
        auth.edit().putString("publicDomain", publicDomain).apply();
    }
}