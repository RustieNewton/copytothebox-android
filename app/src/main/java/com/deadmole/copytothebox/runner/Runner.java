package com.deadmole.copytothebox.runner;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;

//app classes
import com.deadmole.copytothebox.util.Constants; //req for src dir and server url
import com.deadmole.copytothebox.util.SyncTarget;
import com.deadmole.copytothebox.util.Logger; 
import com.deadmole.copytothebox.util.SyncPreferences;
import com.deadmole.copytothebox.util.AuthPreferences;
import com.deadmole.copytothebox.util.NetworkUtils;

public class Runner {

    private static Context appContext; // needed here for sync prefs, auth prefs, logger
    private static final Boolean debugging = Constants.DEBUG_MODE;

    public static boolean run( Context context ) {

        // get context into an obj
        appContext = context.getApplicationContext();

        if(debugging) Logger.log( appContext, "Runner started.");

        // Initialize preferences ONCE here
        SyncPreferences.init(appContext);// usage SyncPreferences.getInstance().isFirstTime()
        AuthPreferences.init(appContext); 

        // Check if the app is enabled
        if (!SyncPreferences.getInstance().getSyncEnabled()) {
            if(debugging) Logger.log(appContext, "Sync disabled in settings. Bailing out");
            SyncPreferences.getInstance().updateRuntimes();
            return false;
        }
        if(debugging) Logger.log( appContext, "app is enabled, pressing on");

        // Check auth token
        String rsyncID = AuthPreferences.getInstance().getRsyncID();
        if (rsyncID == null) {
            if(debugging) Logger.log(appContext, "App not registered — missing auth key. Bailing out.");
            SyncPreferences.getInstance().updateRuntimes();
            return false;
        }
        if(debugging) Logger.log(appContext,"app has an rsyncID auth token, pressing on");

        //check server id
        String serverID = AuthPreferences.getInstance().getServerID();
        if (serverID == null) {
            if(debugging) Logger.log(appContext, "App not registered — missing server ID. bailing out");
            SyncPreferences.getInstance().updateRuntimes();
            return false;
        }
        if(debugging) Logger.log(appContext,"app has a server id, pressing on");

        // Check if it's time to run
        long currentTime = System.currentTimeMillis();
        long lastRun = SyncPreferences.getInstance().getLastRun(); // millis
        int syncInterval = SyncPreferences.getInstance().getSyncInterval(); // hours
        long nextRun = lastRun + syncInterval * 3600000L;
        if ( nextRun > currentTime ) {
            if(debugging) Logger.log(appContext, "we haven't reached the maximum interval since last run. Bailing out.");
            return false;
        }
        if(debugging) Logger.log(appContext,"it is time to run, pressing on");

        // connectivity
        if(debugging) Logger.log(appContext, "checking for connectivity");
        if( !resolveConnection() ) {
            if(debugging) Logger.log(appContext, "no connectivity, bailing out");
            SyncPreferences.getInstance().updateRuntimes();
            return false;
        }

        // get the available host
        if(debugging) Logger.log(appContext,"testing if local host (brainbox) or public host is available");
        String hostname = resolveHostname();
        if (hostname == null) {
            if(debugging) Logger.log( appContext, "No valid hostname found. Bailing out.");
            return false;
        }
        //report host
        if(debugging) Logger.log( appContext, "Hostname proved, the server is available. Using this one => " + hostname);
        
        // get binary
        if(debugging) Logger.log(appContext,"locating binary before running it");
        File rsyncBinary = new File(context.getApplicationInfo().nativeLibraryDir, Constants.RSYNC_BINARY);

        if (!rsyncBinary.exists()) {
            if(debugging) Logger.log(appContext, "Bundled rsync binary not found at " + rsyncBinary.getAbsolutePath());
            SyncPreferences.getInstance().updateRuntimes();
            return false;
        }

        String binaryPath = rsyncBinary.getAbsolutePath();
        for (SyncTarget target : Constants.SYNC_TARGETS) {

            // Ensure target.sourcePath exists, else continue
            // target.sourcePath should be in the form /storage/emulated/0/Music/Recordings etc
            File source = new File(target.sourcePath);
            if (!source.exists() || !source.isDirectory()) {
                if(debugging) Logger.log(appContext, "Skipping missing path or not a dir : " + target.sourcePath);
                continue;
            }

            //prepare rsync command str
            List<String> cmd = buildRsyncCommand(hostname, target, rsyncID, binaryPath);

            //run command
            if(debugging) Logger.log( appContext, "Running: " + cmd);
            if(debugging) Logger.log(appContext, "copying from ... " + target.sourcePath);

            boolean ranOK = runCommand( cmd, appContext);
            if( !ranOK ) {
                if(debugging) Logger.log(appContext, "this one FAILED : " + target.sourcePath);
                return false;
            }
            if(debugging) Logger.log(appContext, "module copied ok");
        }
        if(debugging) Logger.log(appContext,"finished pushing files, moving on");

        // Update nextRun and lastRun
        if(debugging) Logger.log(appContext, "updating run times");
        SyncPreferences.getInstance().updateRuntimes();

        //always log this ?
        if(debugging) Logger.log( appContext, "Runner finished.");
        return true;
    }

    // ---------------- Helper Methods ----------------
    private static String resolveHostname() {

        String local = Constants.REGISTER_HOST; // = "brainbox"
        String publicHost = AuthPreferences.getInstance().getPublicDomain();

        // try brainbox host first
        if(debugging) Logger.log(appContext,"trying to find server on "+local);
        if (pingHost(local)) {
            return local;
        }
        //test for public domain
        if( publicHost == null ) {
            return null;
        }
        //try public domain
        if(debugging) Logger.log(appContext, "trying to find server on " + publicHost);
        if ( pingHost(publicHost) ) {
            return publicHost;
        }
        return null;
    }
    //ping host on local hostname or public domain
    private static boolean pingHost(String host) {
        try {
            Process proc = Runtime.getRuntime().exec("ping -c 1 " + host);
            int result = proc.waitFor();
            return result == 0;
        } catch (Exception e) {
            if(debugging) Logger.log( appContext, "Ping failed: " + Log.getStackTraceString(e)); // use this method, REQ Log import
            return false;
        }
    }
    //build command str
    private static List<String> buildRsyncCommand(String host, SyncTarget target, String rsyncID, String binaryPath) {
        String sourceStr = target.sourcePath + "/";
        String moduleNameStr = rsyncID + "_" + target.moduleName;
        String destinationStr = "rsync://" + Constants.BRAINBOX_USER + "@" + host + ":" + Constants.RSYNC_PORT + "/" + moduleNameStr;

        List<String> baseCommand = new ArrayList<>();
        baseCommand.add(binaryPath);
        baseCommand.add(Constants.RSYNC_OPTS);  // ✅ single argument, no split

        //exclude hidden files and hidden dir, there are some, esp garbage
        // rsync expects a series of exclude statements if more than one is req
        baseCommand.add("--exclude");
        baseCommand.add(".*");

        //strip out videos from Camera dir for photo upload
        if (target.moduleName.equals("photos")) {
            baseCommand.add("--exclude");
            baseCommand.add("*.mpeg");
        } 
        // strip out photos from Camera dir for video upload
        if (target.moduleName.equals("videos")) {
            baseCommand.add("--exclude");
            baseCommand.add("*.jpg");
        }

        baseCommand.add(sourceStr);
        baseCommand.add(destinationStr);

        return baseCommand;
    }
    //run command
    private static boolean runCommand(List<String> command, Context appContext) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if(debugging) Logger.log(appContext, "rsync: " + line);
                }
            }

            int exitCode = process.waitFor();
            //if(debugging) Logger.log(appContext, "Process exited with code: " + exitCode);
            return (exitCode == 0); 

        } catch (Exception e) {
            if(debugging) Logger.log(appContext, "Command failed: " + Log.getStackTraceString(e)); // use this method, REQ Log import
            return false;
        }
    }

    // new Fri 4 Jul, the old home and away logic, but much better
    private static boolean resolveConnection() {
        //we check whether we can push given where the device is and what connection is avail and what the user chose
        boolean wifiAvailable = false;
        boolean gsmAvailable = false;

        if (NetworkUtils.isWifiAvailable(appContext)) {
            // Connected via Wi-Fi
            wifiAvailable = true;
        }
        if (NetworkUtils.isMobileDataAvailable(appContext)) {
            // Connected via GSM/Mobile Data
            gsmAvailable = true;
        }
        boolean useGSM = SyncPreferences.getInstance().getUseGSM(); // user choice, use GSM data or not
        if(debugging) {
            //report results
            if(debugging) {
                if(debugging) Logger.log(appContext, (wifiAvailable) ? "wifi available": "wifi not available");
                if(debugging) Logger.log(appContext, (gsmAvailable) ? "GSM available": "GSM not available");
                if(debugging) Logger.log(appContext, (useGSM) ? "GSM use permitted":"GSM use forbidden" );
            }
        }

        //we assume ok and look for reasons NOT to run
        // 1 no connection at all
        if(!wifiAvailable && !gsmAvailable) {
            if(debugging) Logger.log(appContext, "no wifi or GSM available, bailing out"); //no block {} needed for one-liners
            return false;
        }
        // 2 wifi = false and useGSM = false bail out
        if (!wifiAvailable && !useGSM){
            if(debugging) if(debugging) Logger.log( appContext, "there is no wifi available, and GSM is forbidden, bailing out");
            return false;
        }
        return true;
    }
}
