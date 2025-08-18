package com.deadmole.copytothebox.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.deadmole.copytothebox.R;
import com.deadmole.copytothebox.BuildConfig;
import com.deadmole.copytothebox.util.SyncPreferences;
import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.Constants;

public class SetupFragment extends Fragment {

    private TextView fileStatusIcon;
    private TextView notificationStatusIcon;
    private TextView registrationStatusIcon;

    private TextView setupCompleteMsg;

    //buttons
    private Button fileButton;
    private Button notificationButton;
    // system file access perm flag
    private static final int REQUEST_CODE_FILE = 1001;
    private static final int REQUEST_CODE_NOTIFICATIONS= 1002;
    // PHONE ONLY
    private static final int REQUEST_CODE_CALL_LOG = 1003;
    private static final int REQUEST_CODE_SMS = 1004;

    // perm status flags
    private boolean checkFileAccessOnResume = false;
    private boolean checkNotificationsOnResume = false;

    //mobile icons
    private TextView callsStatusIcon;
    private TextView smsStatusIcon;
    // mobile only buttons
    private Button callsButton;
    private Button smsButton;
    //mobile only
    private boolean checkCallAccessOnResume = false;
    private boolean checkSmsAccessOnResume = false;

    private static final Boolean debugging = Constants.DEBUG_MODE;

    private SyncPreferences prefs;
    private Context appContext; //def but don't instantiate yet

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        appContext = requireContext().getApplicationContext(); //this is safe inside a method, nb diff to MainActivity handling

        //this is good
        prefs = SyncPreferences.getInstance();

        // status elements
        fileStatusIcon = view.findViewById(R.id.file_status_icon);
        notificationStatusIcon = view.findViewById(R.id.notifications_status_icon);
        registrationStatusIcon = view.findViewById(R.id.registration_status_icon);
        setupCompleteMsg = view.findViewById(R.id.setup_complete_msg);
        callsStatusIcon = view.findViewById(R.id.calls_status_icon);
        smsStatusIcon   = view.findViewById(R.id.sms_status_icon);

        if (BuildConfig.FLAVOR.equals("phone")) {
            //phone only section defaults to GONE
            view.findViewById(R.id.phone_only_section).setVisibility(View.VISIBLE);
        }

        // button elements
        fileButton = view.findViewById(R.id.btn_file_permission);
        notificationButton = view.findViewById(R.id.btn_notifications_permission);
        // button elements (phone ONLY)
        callsButton = view.findViewById(R.id.btn_calls_permission);
        smsButton   = view.findViewById(R.id.btn_sms_permission);

        //button listeners
        fileButton.setOnClickListener(v -> requestFilePermission());
        notificationButton.setOnClickListener(v -> requestNotificationPermission());
        // fixme add listeners for phone call db and sms db
        callsButton.setOnClickListener(v -> requestCallLogPermission());
        smsButton.setOnClickListener(v -> requestSmsPermission());

        // update in case user is nav back after a break
        updateUI();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onResume() {
        super.onResume();

        appContext = requireContext().getApplicationContext();
        //file access perm
        if (checkFileAccessOnResume) {
            boolean fileGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? Environment.isExternalStorageManager()
                    : ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

            if(debugging) Logger.log(appContext, "onResume: file permission check, granted: " + fileGranted);

            if (fileGranted) {
                prefs.setFileAccessFlag(true); // already done?
                updateUI(); // not here , but after all checks done?
                checkFileAccessOnResume = false;
            }
        }
        // notif perm
        if (checkNotificationsOnResume) {
            boolean notifGranted = NotificationManagerCompat
                    .from(requireContext())
                    .areNotificationsEnabled();

            if(debugging) Logger.log(appContext, "onResume: notification permission check, granted: " + notifGranted);

            prefs.setNotificationFlag(notifGranted); //already done ?
            updateUI(); // later? 
            checkNotificationsOnResume = false;
        }
        // call log access and SMS access PHONE ONLY
        if(BuildConfig.FLAVOR.equals("phone")) {
            // fixme add call db perm if phone version
            if (checkCallAccessOnResume) {
                boolean callsGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALL_LOG )== PackageManager.PERMISSION_GRANTED;
                if(callsGranted) {
                    prefs.setCallAccessFlag(true);
                    updateUI();
                    checkCallAccessOnResume = false;
                }
            }
            // fixme add sms db perm if phone version
            if (checkSmsAccessOnResume) {
                boolean smsGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_SMS)== PackageManager.PERMISSION_GRANTED;
                if(smsGranted) {
                    prefs.setSmsAccessFlag(true);
                    updateUI();
                    checkSmsAccessOnResume=false;
                }
            }
        }
    }
    @SuppressWarnings("ConstantConditions")
    private void updateUI() {
        String tickSymbol = "✅";
        String crossSymbol ="❌";

        appContext = requireContext().getApplicationContext(); // safe inside method

        boolean fileGranted = prefs.getFileAccessFlag();
        fileStatusIcon.setText(fileGranted ? tickSymbol : crossSymbol);
        fileButton.setVisibility(fileGranted ? View.GONE : View.VISIBLE);

        boolean notifGranted = prefs.getNotificationFlag();
        notificationStatusIcon.setText(notifGranted ? tickSymbol : crossSymbol);
        notificationButton.setVisibility(notifGranted ? View.GONE : View.VISIBLE);

        boolean registered = prefs.getRegisteredFlag();
        registrationStatusIcon.setText(registered ? tickSymbol : crossSymbol); // no button to show or hide


        // mobile only
        if(BuildConfig.FLAVOR.equals("phone")) {
            boolean callAccessGranted = prefs.getCallAccessFlag();
            callsStatusIcon.setText(callAccessGranted ? tickSymbol : crossSymbol);
            callsButton.setVisibility( callAccessGranted ? View.GONE : View.VISIBLE); 

            boolean smsAccessGranted = prefs.getSmsAccessFlag();
            smsStatusIcon.setText(smsAccessGranted ? tickSymbol : crossSymbol);
            smsButton.setVisibility( smsAccessGranted ? View.GONE : View.VISIBLE);
        }
        
        // nb isSetupComplete() handles default or phone permission tests and flags
        boolean everythingGranted = prefs.isSetupComplete();
        setupCompleteMsg.setVisibility((everythingGranted) ? View.VISIBLE : View.GONE);
    }
    /** @noinspection deprecation*/ // vv 4 FILE PERMISSIONS
    private void requestFilePermission() {
        appContext = requireContext().getApplicationContext(); // safe inside method
        if (debugging) Logger.log(appContext, "running file access permissions request");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Full Storage Access Needed")
                    .setMessage("This app needs permission to access all files. Please grant 'All files access' in settings.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        //set the flag, onResume() will now check the FILE ACCESS perm in the system and update UI if true
                        checkFileAccessOnResume = true;
                        // required— only for Android 11+ (API 30+) when requesting MANAGE_EXTERNAL_STORAGE.
                        startActivityForResult(intent, REQUEST_CODE_FILE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            } else {
                if(debugging) Logger.log(appContext, "MANAGE_EXTERNAL_STORAGE already granted");
                prefs.setFileAccessFlag(true); // ✅ Add this
                updateUI();                    // ✅ Add this
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_FILE);
            } else {
                if(debugging) Logger.log(appContext, "READ_EXTERNAL_STORAGE already granted");
                prefs.setFileAccessFlag(true); // ✅ Add this
                updateUI();                    // ✅ Add this
            }
        }
    }
    // vv 5
    // FIXME some kind of deprecation problem
    // For API 32 and below, there's no runtime permission; just check with
    private void requestNotificationPermission() {
        appContext = requireContext().getApplicationContext();
        if (debugging) Logger.log(appContext, "running notifications permission request");

        boolean granted = NotificationManagerCompat
                .from(requireContext())
                .areNotificationsEnabled();

        if (granted) {
            prefs.setNotificationFlag(true);
            updateUI();
        } else {
            //set flag to check system has registered the grant, and update UI 
            checkNotificationsOnResume = true;

            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());

            startActivity(intent);  // no need for startActivityForResult
        }
    }

    private void requestCallLogPermission() {
        appContext = requireContext().getApplicationContext();
        if (debugging) Logger.log(appContext, "running call log permission request");

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CODE_CALL_LOG);
            checkCallAccessOnResume = true;

        } else {
            prefs.setCallAccessFlag(true);
            updateUI();
        }
    }

    private void requestSmsPermission() {
        appContext = requireContext().getApplicationContext();
        if (debugging) Logger.log(appContext, "running SMS permission request");

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUEST_CODE_SMS);
            checkSmsAccessOnResume = true;

        } else {
            prefs.setSmsAccessFlag(true);
            updateUI();
        }
    }

    /** @noinspection deprecation*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0) return;

        switch (requestCode) {
            case REQUEST_CODE_FILE:
                // Only applies to pre-API 30 (Android 10 and below)
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prefs.setFileAccessFlag(true);
                    updateUI();
                    if(debugging) Logger.log(appContext, "Legacy file access granted via READ_EXTERNAL_STORAGE");
                } else {
                    if(debugging) Logger.log(appContext, "Legacy file access denied");
                }
                break;

            case REQUEST_CODE_NOTIFICATIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prefs.setNotificationFlag(true);
                    updateUI();
                    if(debugging) Logger.log(appContext, "Notification permission granted");
                } else {
                    if(debugging) Logger.log(appContext, "Notification permission denied");
                }
                break;

            case REQUEST_CODE_CALL_LOG:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prefs.setCallAccessFlag(true);
                    updateUI();
                    Logger.log(appContext, "Call log permission granted");
                } else {
                    Logger.log(appContext, "Call log permission denied");
                }
                break;

            case REQUEST_CODE_SMS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prefs.setSmsAccessFlag(true);
                    updateUI();
                    if(debugging) Logger.log(appContext, "SMS permission granted");
                } else {
                    if(debugging) Logger.log(appContext, "SMS permission denied");
                }
                break;

            default:
                if(debugging) Logger.log(appContext, "Unknown permission request code: " + requestCode);
        }
    }


}