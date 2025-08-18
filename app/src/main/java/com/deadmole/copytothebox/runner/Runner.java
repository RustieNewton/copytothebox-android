package com.deadmole.copytothebox.runner;

import android.content.Context;
import android.util.Log;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import java.net.HttpURLConnection;
import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//app classes
import com.deadmole.copytothebox.util.Constants; //req for src dir and server url
import com.deadmole.copytothebox.util.SyncTarget;
import com.deadmole.copytothebox.util.Logger; 
import com.deadmole.copytothebox.util.SyncPreferences;
import com.deadmole.copytothebox.util.AuthPreferences;
import com.deadmole.copytothebox.util.NetworkUtils;
import com.deadmole.copytothebox.util.SignalStrengthHelper;
import com.deadmole.copytothebox.BuildConfig;
import com.deadmole.copytothebox.system.RunnerManager; // to reschedule one-off fallback job

public class Runner {

    private static Context appContext; // needed here for sync prefs, auth prefs, logger
    private static final Boolean debugging = Constants.DEBUG_MODE;
    // guard against starting a run during an ongoing run
    private static volatile boolean isRunning = false;

    public static boolean run( Context context ) {

        if (isRunning) {
            if(debugging) Logger.log(appContext, "Runner: Process already running, skipping...");
            return false;
        }
        isRunning = true;

        // get context into an obj
        appContext = context.getApplicationContext();

        if(debugging) Logger.log( appContext, "Runner started.");

        // Initialize preferences ONCE here
        SyncPreferences.init(appContext);// usage SyncPreferences.getInstance().isFirstTime()
        AuthPreferences.init(appContext); 

        // Check if the app is enabled
        if (!SyncPreferences.getInstance().getSyncEnabled()) {
            if(debugging) Logger.log(appContext, "Sync disabled in settings. Bailing out");
            return false;
        }
        if(debugging) Logger.log( appContext, "app is enabled, pressing on");

        // Check auth token
        String rsyncID = AuthPreferences.getInstance().getRsyncID();
        if (rsyncID == null) {
            if(debugging) Logger.log(appContext, "App not registered — missing auth key. Bailing out.");
            return false;
        }
        if(debugging) Logger.log(appContext,"app has an rsyncID auth token, pressing on");

        //check server id
        String serverID = AuthPreferences.getInstance().getServerID();
        if (serverID == null) {
            if(debugging) Logger.log(appContext, "App not registered — missing server ID. bailing out");
            return false;
        }
        if(debugging) Logger.log(appContext,"app has a server id, pressing on");

        // Check if it's time to run
        if(!isItTimeToRun(context)) {
            if(debugging) Logger.log(appContext, "we haven't reached the maximum interval since last run. Bailing out.");
            return false;            
        }
        if(debugging) Logger.log(appContext,"it is time to run, pressing on");

        // connectivity
        if(debugging) Logger.log(appContext, "checking for connectivity");
        if(BuildConfig.FLAVOR.equals("phone")) {
            //check conn for a phone
            if(!provePhoneConnection()) {
                if(debugging) Logger.log(appContext, "Runner: mobile device, no connectivity or GSM disabled, bailing out");
                return false;
            }
        } else {
            //check conn for a tablet
            if( !proveTabletConnection() ) {
                if(debugging) Logger.log(appContext, "Runner: device is a tablet, no connectivity, bailing out");
                return false;
            }
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

        // PHONE STUFF ONLY IF FLAVOUR ALLOWS ...
        if(debugging) Logger.log(appContext, "doing phone only stuff");        
        doPhoneStuff(hostname);

        // get binary
        File rsyncBinary= findRsyncBinary(appContext); // FIXME check this function on bedroom tablet, and sitting rm tablet
        if(rsyncBinary==null) {
            return false;
        } 

        // RUN rsync for each module
        if( debugging ) Logger.log(appContext,"Runner: running rsync for each module");
        iterateModules(rsyncBinary, hostname, rsyncID);

        // Update nextRun and lastRun
        if(debugging) Logger.log(appContext, "Runner: updating run times");
        SyncPreferences.getInstance().updateRuntimes();
        
        // set another WorkManager job
        if(debugging) Logger.log(appContext, "Runner: rescheduling next run/job");
        RunnerManager.scheduleNextJob(appContext); //usage is good

        //reset the guard
        isRunning = false;

        if(debugging) Logger.log( appContext, "Runner finished.");

        return true;
    }
    public static boolean isItTimeToRun( Context context ) {
        // init prefs
        appContext = context.getApplicationContext();
        SyncPreferences.init(appContext);
        long currentTime = System.currentTimeMillis();
        long lastRun = SyncPreferences.getInstance().getLastRun(); // millis
        int syncInterval = SyncPreferences.getInstance().getSyncInterval(); // hours
        long nextRun = lastRun + syncInterval * 3600000L;
        return nextRun <= currentTime;
    }
    // ---------------- shared helper Methods ----------------
    private static void iterateModules(File rsyncBinary, String hostname, String rsyncID) {
 
        String binaryPath = rsyncBinary.getAbsolutePath();
        for (SyncTarget target : Constants.SYNC_TARGETS) {

            // target.sourcePath should be in the form /storage/emulated/0/Music/Recordings etc

            // Ensure target.sourcePath exists, else continue
            File source = new File(target.sourcePath);
            if (!source.exists() || !source.isDirectory()) {
                if(debugging) Logger.log(appContext, "SKIPPING: this src: " + target.sourcePath);
                continue;
            }

            //skip whatsApp modules if is not phone or no whatsApp
            // FIXME  we can add enable/disable flags later
            if(target.sourcePath.contains("com.whatsapp")){
                // not a phone, can't have whatsapp
                if(!BuildConfig.FLAVOR.equals("phone")) continue;

                // is whatsApp installed?
                File file = new File(target.sourcePath);
                if (!file.isDirectory()) continue;
            }

            //prepare rsync command str
            List<String> cmd = buildRsyncCommand(hostname, target, rsyncID, binaryPath);

            //run command
            if(debugging) Logger.log(appContext, "Runner: module="+target.moduleName);
            if(debugging) Logger.log(appContext, "...from "+target.sourcePath); //added bc videos and photo modules use same src

            boolean ranOK = runCommand( cmd, appContext);
            if( !ranOK ) {
                if(debugging) Logger.log(appContext, "Runner: this one FAILED : " + target.sourcePath);
            }
            if(debugging) Logger.log(appContext, "Runner: module copied ok");
        }
    }
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
    private static File findRsyncBinary(Context context) {

        String[] possibleDirs = new String[] {
            context.getApplicationInfo().nativeLibraryDir,                        // /data/app/your.package/lib the orig vv
            context.getApplicationInfo().dataDir + "/lib",                        // /data/user/0/your.package/lib
            context.getFilesDir().getAbsolutePath(),                              // /data/user/0/your.package/files
            context.getCodeCacheDir().getAbsolutePath(),                          // /data/user/0/your.package/code_cache
        };
 
        for (String dir : possibleDirs) {
            try {
                File candidate = new File(dir, Constants.RSYNC_BINARY);
                if (debugging) {
                    Logger.log(appContext, "Checking for rsync at: " + candidate.getAbsolutePath());
                }

                if (candidate.exists()) {
                    if (debugging) {
                        Logger.log(appContext, "Found rsync binary at: " + candidate.getAbsolutePath());
                        Logger.log(appContext, "Readable: " + candidate.canRead() + ", Executable: " + candidate.canExecute());
                    }

                    // Optionally: ensure executable permission
                    if (!candidate.canExecute()) {
                        if(!candidate.setExecutable(true)){
                            if(debugging) Logger.log(appContext,"cannot set binary to executable, bailing out");
                            return null;
                        }
                        if (debugging) Logger.log(appContext, "Set executable: " + candidate.canExecute());
                    }

                    return candidate;
                }
            } catch (Exception e) {
                Logger.log(appContext, "Error while checking for rsync in " + dir + ": " + Log.getStackTraceString(e));
            }
        }

        if (debugging) {
            Logger.log(appContext, "Rsync binary not found in expected locations.");
        }

        return null;
    }

    //build command str
    private static List<String> buildRsyncCommand(String host, SyncTarget target, String rsyncID, String binaryPath) {
        String sourceStr = target.sourcePath + "/"; // append /
        String moduleNameStr = rsyncID + "_" + target.moduleName; // eg XXXXXX_documents
        String destinationStr = "rsync://" + Constants.BRAINBOX_USER + "@" + host + ":" + Constants.RSYNC_PORT + "/" + moduleNameStr;

        List<String> baseCommand = new ArrayList<>();
        baseCommand.add(binaryPath);
        //added 2 Aug, 12th wedding anniversary, if Helen had not betrayed me
        if(BuildConfig.FLAVOR.equals("phone")){
            // there is a compression flag 'z' which we can use for GSM pushes, to save data
            baseCommand.add(Constants.RSYNC_GSM_OPTS);
        } else {
            baseCommand.add(Constants.RSYNC_OPTS);  // ✅ single argument, no split
        }

        // build some exclusions
        // rsync expects a series of exclude statements if more than one is req

        //exclude hidden files and hidden dir, there are some, esp garbage
        baseCommand.add("--exclude");
        baseCommand.add(".*");

        // exclude .apk files 25 Jul , they are always in downloads
        if (target.moduleName.equals("downloads")) {
            baseCommand.add("--exclude");
            baseCommand.add("*.apk");            
        }

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

        // some optional switches to add
        if(BuildConfig.FLAVOR.equals("phone")) {
            // when using GSM it may fail due to poor connection
            // https://chatgpt.com/c/6888b82c-027c-800d-99a2-1aec0cabaed5

            //--compress-level=9 to reduce data transferred.
            baseCommand.add("--compress-level=9");
            //--bwlimit=50  //Reduce I/O load with bandwidth limit:
            baseCommand.add("--bwlimit=50");
            // --partial --append-verify =>  to resume failed transfers:
            baseCommand.add("--partial");
            baseCommand.add("--append-verify");

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
    // revised 28 Jul, to split default from phone logic
    private static boolean proveTabletConnection() {

        // check wifi
        boolean wifiAvailable = NetworkUtils.isWifiAvailable(appContext);

        //report connections avail
        if(debugging) Logger.log(appContext, (wifiAvailable) ? "wifi available": "wifi not available");

        return wifiAvailable;
    }

    /* MOBILE PHONE METHODS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */ 
    private static boolean provePhoneConnection() {
        //there are various tests for mobile, as it has 2 connection types, either may be turned off on the device,
        // one may be turned off, may be disabled by user, or signal too weak

        // check wifi - if we have it use it
        if( NetworkUtils.isWifiAvailable(appContext) ) {
            return true;
        }

        // no wifi - is GSM use auth by user?
        if(!SyncPreferences.getInstance().getUseGSM()) {
            //no, no wifi, no GSM perm, bail out
            if(debugging) Logger.log(appContext, "GSM use disabled by user, no wifi, bailing out...");
            return false;
        }

        // check GSM is turned on
        if(debugging) Logger.log(appContext, "GSM use permitted, testing GSM available...");
        if(!NetworkUtils.isMobileDataAvailable(appContext)) {
            // no GSM turned off on device
            if(debugging) Logger.log(appContext, "GSM turned off, no wifi, bailing out...");
            return false;
        }

        //check the signal quality is high enough
        if (!proveGsmSignalStrength()) { 
            if(debugging) Logger.log(appContext,"GSM signal is too weak, bailing out ...");
            return false;
        }
        return true;
    }
    private static boolean proveGsmSignalStrength() {
        SignalStrengthHelper helper = new SignalStrengthHelper(appContext); // is this avail to the method
        helper.startListening();
        return helper.isSignalStrongEnough();
    }
    private static void doPhoneStuff(String hostname) {

        if (BuildConfig.FLAVOR.equals("phone")) {
            if (debugging) Logger.log(appContext, "we have a phone...");

            // Pushing Call Data
            boolean pushCallDataNow = SyncPreferences.getInstance().getPushCalls();
            if (pushCallDataNow) {
                //ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALL_LOG )== PackageManager.PERMISSION_GRANTED
                if (SyncPreferences.getInstance().getCallAccessFlag()) {
                    if (debugging) Logger.log(appContext, "pushing call data is enabled, trying now...");
                    pushCallData(hostname);
                } else {
                    Logger.log(appContext, "Permission denied: cannot read call log");
                }
            } else {
                if(debugging) Logger.log(appContext, "pushing calls not enabled, moving on...");
            }

            // Pushing SMS Data
            boolean pushSmsDataNow = SyncPreferences.getInstance().getPushSMS();
            //ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_SMS)== PackageManager.PERMISSION_GRANTED
            if (pushSmsDataNow) {
                if (SyncPreferences.getInstance().getSmsAccessFlag()) {
                    if (debugging) Logger.log(appContext, "pushing SMS messages is enabled, trying now ...");
                    pushSmsData(hostname);
                } else {
                    Logger.log(appContext, "Permission denied: cannot read SMS messages");
                }
            } else {
                if(debugging) Logger.log(appContext, "pushing SMS not enabled, moving on...");
            }
        }
    }
    // Call log - push data
    private static void pushCallData(String hostname) {

       if (debugging) Logger.log(appContext, "starting to collect call data before pushing");
       // do we have permissions ?
       if( debugging) Logger.log(appContext,"do we have permission?");

       String protocol = Constants.REGISTER_PROTOCOL;
       String endpointPath = Constants.CALLDATA_ENDPOINT;
       String pushCommand = Constants.CALLDATA_COMMAND;

       String rsyncID = AuthPreferences.getInstance().getRsyncID();
       String serverID = AuthPreferences.getInstance().getServerID();
       long lastCallPush = SyncPreferences.getInstance().getLastCallPush();
       if(debugging) Logger.log(appContext, "rsyncID is "+rsyncID+"; serverID is "+serverID);
       JsonArray callData = getCallLogSince(lastCallPush);
          //"date":"Thu Apr 29 12:29:56 GMT+01:00 2021"
           //"number":"07825227356"
           //"duration":"319",
           //"type":"INCOMING",
           //"carrier":"giffgaff"

       if (callData.isEmpty()) {
           if (debugging) Logger.log(appContext, "No new call data to push");
           return;
       }
       // print the data
       //if(debugging) Logger.log(appContext, "CALL DATA=>"+callData);

       JsonObject payload = new JsonObject();
       payload.addProperty("command", pushCommand);
       payload.addProperty("rsyncID", rsyncID);
       payload.addProperty("serverID", serverID);
       payload.add("calls", callData);

       try {
           String endpointUrl = protocol+"://"+hostname + "/" + endpointPath;
           if(debugging) Logger.log(appContext,"sending calls to "+endpointUrl);
           String response = postJson(endpointUrl, payload);
           // we already logged any post HTTP errors, no need for else handling
           if(response !=null) {
               JsonObject jsonRes = JsonParser.parseString(response).getAsJsonObject();
               if (jsonRes.has("errors") && jsonRes.get("errors").getAsString().equals("none")) {
                    //update last call push time
                    SyncPreferences.getInstance().setLastCallPush(); //comment out for testing
                    if (debugging) Logger.log(appContext, "Call data push successful");
               } else {
                    Logger.log(appContext, "Server returned error: " + response);
               }
           }
       } catch (Exception e) {
           Logger.log(appContext, "Failed to push call data: " + e.getMessage());
       }
    }
    // call data helpers
    private static JsonArray getCallLogSince(long sinceTimestamp) {
       JsonArray callArray = new JsonArray();
       Uri callUri = CallLog.Calls.CONTENT_URI;

       String selection = CallLog.Calls.DATE + " > ?";
       String[] selectionArgs = { String.valueOf(sinceTimestamp) };
       String sortOrder = CallLog.Calls.DATE + " ASC";

       Cursor cursor = appContext.getContentResolver().query(
           callUri, null, selection, selectionArgs, sortOrder
       );

       if (cursor != null) {
           while (cursor.moveToNext()) {
               int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
               long dateMillis = cursor.getLong(dateIndex);
               Date date = new Date(dateMillis);
               String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(date);

               int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
               String number = cursor.getString(numberIndex);

               int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
               int typeCode = cursor.getInt(typeIndex);
               String type;
               switch (typeCode) {
                   case CallLog.Calls.INCOMING_TYPE: type = "INCOMING"; break;
                   case CallLog.Calls.OUTGOING_TYPE: type = "OUTGOING"; break;
                   case CallLog.Calls.MISSED_TYPE:   type = "MISSED";   break;
                   case CallLog.Calls.REJECTED_TYPE: type = "REJECTED"; break;
                   default:                           type = "UNKNOWN";  break;
               }
               int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
               String duration = cursor.getString(durationIndex);

               String carrier = getCarrierName();

               JsonObject call = new JsonObject();
               call.addProperty("date", formattedDate);
               call.addProperty("number", number);
               call.addProperty("type", type);
               call.addProperty("duration", duration);
               call.addProperty("carrier", carrier);

               callArray.add(call);
           }
           cursor.close();
       }

       return callArray;
    }
    private static String getCarrierName() {
       TelephonyManager tm = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
       return tm != null ? tm.getNetworkOperatorName() : "unknown";
    }

    // SMS - push data 
    private static void pushSmsData(String hostname) {
       if (debugging) Logger.log(appContext, "starting to collect SMS data before pushing");

       String protocol = Constants.REGISTER_PROTOCOL;
       String endpointPath = Constants.CALLDATA_ENDPOINT; // same as call data
       String pushCommand = Constants.SMSDATA_COMMAND;

       String rsyncID = AuthPreferences.getInstance().getRsyncID();
       String serverID = AuthPreferences.getInstance().getServerID();
       long lastSmsPush = SyncPreferences.getInstance().getLastSmsPush();

       JsonArray smsData = getSmsSince(lastSmsPush);       
       if (smsData.isEmpty()) {
           if (debugging) Logger.log(appContext, "No new SMS data to push");
           return;
       }
       // show data for testing
       //if(debugging) Logger.log(appContext, "SMS data => " + smsData);

       JsonObject payload = new JsonObject();
       payload.addProperty("command", pushCommand);
       payload.addProperty("rsyncID", rsyncID);
       payload.addProperty("serverID", serverID);
       payload.add("sms-data", smsData);

       try {
           String endpointUrl = protocol+"://"+hostname + "/" + endpointPath;
           String response = postJson(endpointUrl, payload);

           if(response != null ) {
               JsonObject jsonRes = JsonParser.parseString(response).getAsJsonObject();
               if (jsonRes.has("errors") && jsonRes.get("errors").getAsString().equals("none")) {
                    //update last sms push time
                    SyncPreferences.getInstance().setLastSmsPush(); //comment out for testing
                    if (debugging) Logger.log(appContext, "SMS data push successful, NOT updating lastSmsPush");
               } else {
                   Logger.log(appContext, "Server returned error (SMS): " + response);
               }
           }
       } catch (Exception e) {
            Logger.log(appContext, "Exception in postJson(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
       }
    }
    //sms data helper
    private static JsonArray getSmsSince(long sinceTimestamp) {
       JsonArray smsArray = new JsonArray();
       Uri smsUri = Uri.parse("content://sms");
       String selection = "date > ?";
       String[] selectionArgs = { String.valueOf(sinceTimestamp) };
       String sortOrder = "date ASC";

       Cursor cursor = appContext.getContentResolver().query(
           smsUri, null, selection, selectionArgs, sortOrder
       );

       if (cursor != null) {
           while (cursor.moveToNext()) {
               int dateIndex = cursor.getColumnIndex("date");
               long dateMillis = cursor.getLong(dateIndex);
               Date date = new Date(dateMillis);
               String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(date);

               int addressIndex = cursor.getColumnIndex("address");
               String address = cursor.getString(addressIndex);

               int bodyIndex = cursor.getColumnIndex("body");
               String body = cursor.getString(bodyIndex);

               int typeCodeIndex = cursor.getColumnIndex("type");
               int typeCode = typeCodeIndex != -1 ? cursor.getInt(typeCodeIndex) : -1;

               int threadIdIndex = cursor.getColumnIndex("thread_id");
               int threadId = threadIdIndex != -1 ? cursor.getInt(threadIdIndex) : -1;

               String type;
               switch (typeCode) {
                   case 1: type = "INBOX"; break;
                   case 2: type = "SENT"; break;
                   case 3: type = "DRAFT"; break;
                   default: type = "UNKNOWN"; break;
               }

               JsonObject sms = new JsonObject();
               sms.addProperty("date", formattedDate);
               sms.addProperty("address", address);
               sms.addProperty("body", body);
               sms.addProperty("type", type);
               sms.addProperty("threadId", threadId);

               smsArray.add(sms);
           }
           cursor.close();
       }

       return smsArray;
    }

    //shared - send data in body of HTTP request
    private static String postJson(String endpointUrl, JsonObject payload) {
        try {
            URL url = new URL(endpointUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()
            ));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            conn.disconnect();

            if (responseCode == 200) {
                if (debugging) Logger.log(appContext, "received reply from push ok");
            } else {
                if (debugging) Logger.log(appContext, "failed to send call data, HTTP response code=" + responseCode);
                if (debugging) Logger.log(appContext, "HTTP res was " + response);
            }

            return response.toString();

        } catch (Exception e) {
            Logger.log(appContext, "Exception in postJson(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null; // fail-safe: caller must handle null
        }
    }

}
