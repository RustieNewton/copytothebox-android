package com.deadmole.copytothebox.util; // includes Constants, no need to import sep

import android.content.Context;

public class PathUtils {
    // get registration url (can be public or private depending on circs, dev or prodn)
    public static String getRegisterUrl() {
        return  Constants.REGISTER_PROTOCOL +"://"+ Constants.REGISTER_HOST +"/" + Constants.REGISTER_PATHNAME;
    }

    // path to log 
    public static String getLogPath(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/" + Constants.LOG_FILENAME;
    }

}
