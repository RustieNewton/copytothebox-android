package com.deadmole.copytothebox.util; // includes PathUtils and Constants 

import android.util.Log; //for err
import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat; // Date offers no formatting
import java.util.Locale;

public class Logger {

    public static void log(Context context, String msg) {
        
            writeToFile(context, msg);
       
    }

    private static void writeToFile(Context context, String msg) {
        try {
            String LOG_PATH = PathUtils.getLogPath(context);
            File logFile = new File( LOG_PATH );
            FileWriter writer = new FileWriter(logFile, true);

            //simplify date format
            Date now = new Date();
            // Use Locale to avoid unexpected formatting on different devices
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm (dd MMM)", Locale.getDefault());// simplify for testing
            String formattedDate = formatter.format(now);

            writer.append(formattedDate)
                  .append(" - ")
                  .append(msg)
                  .append("\n");
            writer.close();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Failed to write log");
        }
    }
}
