package com.deadmole.copytothebox.util;

public final class Constants {
    
    private Constants() {
        // Prevent instantiation
    }
    //debug flag
    public static final boolean DEBUG_MODE = false;



    // register url
    public static final String REGISTER_PROTOCOL = "http"; // poss set to http if possible
    public static final String REGISTER_HOST  = "brainbox"; // simulate newton.house here if nec and whitelist it as well /res/xml
    public static final String REGISTER_PATHNAME = "cgi/register.php";

    // Logging
    public static final String LOG_FILENAME = "copytothebox.log";
    public static final String LOG_TAG = "CopyToTheBox";  //used once in Logger, as failsafe
    public static final int LOG_LINES_TO_READ = 100;  // example, adjust as needed

    // rsync binary name
    public static final String RSYNC_BINARY = "rsync.so"; // adding .so to trick android studio into packaging binary from jniLibs into /lib

    //rsync command components
    public static final String BRAINBOX_USER = "bbx";
    public static final String RSYNC_PORT = "873" ;
    public static final String RSYNC_OPTS = "-rtpu"; // add vvv for verbose IN DEV

    // Android user-facing source directories
    // there are 11 modules per user 
    // finding external SD cards is in the next phase
    public static final SyncTarget[] SYNC_TARGETS = {
        new SyncTarget("/storage/emulated/0/Documents", "documents"),
        new SyncTarget("/storage/emulated/0/Download", "downloads"),
        new SyncTarget("/storage/emulated/0/DCIM/Camera", "photos"),
        new SyncTarget("/storage/emulated/0/DCIM/Pictures", "pictures"),
        new SyncTarget("/storage/emulated/0/DCIM/Screenshots", "screenshots"),
        new SyncTarget("/storage/emulated/0/DCIM/Camera", "videos"),
        new SyncTarget("/storage/emulated/0/Audiobooks", "audiobooks"),
        new SyncTarget("/storage/emulated/0/Podcasts", "podcasts"),
        new SyncTarget("/storage/emulated/0/Recordings", "voicenotes"), //3 sources of audio recordings
        new SyncTarget("/storage/emulated/0/Music/Recordings", "voicenotes"),
        new SyncTarget("/storage/emulated/0/DCIM/Recordings", "voicenotes"),
        new SyncTarget("/storage/emulated/0/Music", "music"),
        new SyncTarget("/storage/emulated/0/Movies", "movies")
    };

}
