package com.deadmole.copytothebox.util;

public final class Constants {
    
    private Constants() {
        // Prevent instantiation
    }
    //debug flag
    public static final boolean DEBUG_MODE = true;

    // register url
    public static final String REGISTER_PROTOCOL = "http"; // poss set to http if possible
    public static final String REGISTER_HOST  = "newton.house"; // simulate newton.house here if nec and whitelist it as well /res/xml
    public static final String REGISTER_PATHNAME = "cgi/register.php";

    // PHONE ONLY
    public static final String CALLDATA_ENDPOINT = "cgi/copyToTheBox/mobilecalls.php";
    public static final String CALLDATA_COMMAND = "save-calls";
    public static final String SMSDATA_COMMAND = "save-sms";

    //network constants
    public static final int dbThreshold = -95;
    
    // Logging
    public static final String LOG_FILENAME = "copytothebox.log";
    public static final String LOG_TAG = "CopyToTheBox";  //used once in Logger, as failsafe
    public static final int LOG_LINES_TO_READ = 100;  // example, adjust as needed

    // rsync binary name
    public static final String RSYNC_BINARY = "rsync.so"; // adding .so to trick android studio into packaging binary from jniLibs into /lib

    //rsync command components
    public static final String BRAINBOX_USER = "bbx";
    public static final String RSYNC_PORT = "873" ;
    /*
        rsync flags
            z compress locally and decompress remotely on the server
            r drill down to subdir (recursive)
            t preserve time 
            p preserve permissions - DON'T do this, mobile devices default to 0700, which fucks up serverside
            u new or updated only (preserve newer on remote)
            v verbose, vv more, vvv even more
            there is also even more verbose logging flags, not nec for here, see bin notes
    */
    public static final String RSYNC_OPTS = "-rtu";
    //PHONE ONLY (GSM optimisation)
    public static final String RSYNC_GSM_OPTS = "-rtuz"; // add compression flag

    // Android user-facing source directories
    // there are 11 core modules
    // there are 3 additional whatsApp modules mapped to some of those 11 

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
        new SyncTarget("/storage/emulated/0/Movies", "movies"),
        // new whatsApp options
        // no need to esc the whitespace https://chatgpt.com/c/688a0358-2d00-800d-bb1d-54065e25fa06
        new SyncTarget("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents", "documents"),
        new SyncTarget("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images", "pictures"),
        new SyncTarget("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video", "videos")
    };

}
