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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.deadmole.copytothebox.R;
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

    private static final int REQUEST_CODE_FILE = 1001;
    private static final int REQUEST_CODE_NOTIFICATION = 1002;
    private boolean checkFileAccessOnResume = false;
    private boolean checkNotificationsOnResume = false;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        appContext = requireContext().getApplicationContext(); //this is safe inside a method, nb diff to MainActivity handling

        prefs = SyncPreferences.getInstance();

        fileStatusIcon = view.findViewById(R.id.file_status_icon);
        notificationStatusIcon = view.findViewById(R.id.notifications_status_icon);
        registrationStatusIcon = view.findViewById(R.id.registration_status_icon);
        setupCompleteMsg = view.findViewById(R.id.setup_complete_msg);
        fileButton = view.findViewById(R.id.btn_file_permission);
        notificationButton = view.findViewById(R.id.btn_notifications_permission);

        //button listeners
        fileButton.setOnClickListener(v -> requestFilePermission());
        notificationButton.setOnClickListener(v -> requestNotificationPermission());
        // update in case user is nav back after a break
        updateUI();
        if (debugging) Logger.log(appContext, "setup onViewCreated, reviewing nec steps");
    }

    @Override
    public void onResume() {
        super.onResume();

        appContext = requireContext().getApplicationContext();

        if (checkFileAccessOnResume) {
            boolean fileGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? Environment.isExternalStorageManager()
                    : ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

            Logger.log(appContext, "onResume: file permission check, granted: " + fileGranted);

            if (fileGranted) {
                prefs.setFileAccessFlag(true);
                updateUI();
                checkFileAccessOnResume = false;
            }
        }

        if (checkNotificationsOnResume) {
            boolean notifGranted = NotificationManagerCompat
                    .from(requireContext())
                    .areNotificationsEnabled();

            Logger.log(appContext, "onResume: notification permission check, granted: " + notifGranted);

            prefs.setNotificationFlag(notifGranted);
            updateUI();
            checkNotificationsOnResume = false;
        }
    }


    private void updateUI() {
        appContext = requireContext().getApplicationContext(); // safe inside method
        if (debugging) Logger.log(appContext, "updating UI ...");

        boolean fileGranted = prefs.getFileAccessFlag();
        boolean notifGranted = prefs.getNotificationFlag();
        boolean registered = prefs.getRegisteredFlag();

        fileStatusIcon.setText(fileGranted ? "\u2705" : "\u274C");
        notificationStatusIcon.setText(notifGranted ? "\u2705" : "\u274C");
        registrationStatusIcon.setText(registered ? "\u2705" : "\u274C");

        fileButton.setVisibility(fileGranted ? View.GONE : View.VISIBLE);
        notificationButton.setVisibility(notifGranted ? View.GONE : View.VISIBLE);

        setupCompleteMsg.setVisibility(
                (fileGranted && notifGranted && registered) ? View.VISIBLE : View.GONE);
    }
    // vv 4 FILE PERMISSIONS
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
                        // reqd — only for Android 11+ (API 30+) when requesting MANAGE_EXTERNAL_STORAGE.
                        startActivityForResult(intent, REQUEST_CODE_FILE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            } else {
                Logger.log(appContext, "MANAGE_EXTERNAL_STORAGE already granted");
                prefs.setFileAccessFlag(true); // ✅ Add this
                updateUI();                    // ✅ Add this
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_FILE);
            } else {
                Logger.log(appContext, "READ_EXTERNAL_STORAGE already granted");
                prefs.setFileAccessFlag(true); // ✅ Add this
                updateUI();                    // ✅ Add this
            }
        }
    }
    // vv 5
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

    //vv4  NOTIFICATION PERMISSIONS
    // private void requestNotificationPermission() {
    //     appContext = requireContext().getApplicationContext(); // safe inside method
    //     if (debugging) Logger.log(appContext, "running notifications permission request");

    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    //         if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
    //                 != PackageManager.PERMISSION_GRANTED) {

    //             // App is not targeting API 33+, so system won't show dialog
    //             new AlertDialog.Builder(requireContext())
    //                     .setTitle("Enable Notifications")
    //                     .setMessage("To receive alerts, please enable notifications in settings.")
    //                     .setPositiveButton("Open Settings", (dialog, which) -> {
    //                         Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    //                                 .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
    //                         startActivityForResult(intent, REQUEST_CODE_NOTIFICATION_SETTINGS);

    //                     })
    //                     .setNegativeButton("Cancel", null)
    //                     .show();
    //         } else {
    //             prefs.setNotificationFlag(true);
    //             updateUI();
    //         }
    //     } else {
    //         prefs.setNotificationFlag(true);
    //         updateUI();
    //     }
    // }

    // // handle FILE ACCESS perm
    // @Override
    // public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    //     //log it
    //     appContext = requireContext().getApplicationContext(); // safe inside a method
    //     Logger.log(appContext, "handling permission RESULT");

    //     super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    //     if (requestCode == REQUEST_CODE_FILE) {
    //         if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    //             if(debugging) Logger.log(appContext, "file access granted by user, hooray");
    //             prefs.setFileAccessFlag(true);
    //         }
    //     } else if (requestCode == REQUEST_CODE_NOTIFICATION) {
    //         if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    //             if(debugging) Logger.log(appContext, "notification permission granted by user, find peace in your life");
    //             prefs.setNotificationFlag(true);
    //         }
    //     }
    //     updateUI();
    // }
    // // handle NOTIFICATIONS return from settings 
    // @Override
    // public void onActivityResult(int requestCode, int resultCode, Intent data) {
    //     super.onActivityResult(requestCode, resultCode, data);

    //     if (requestCode == REQUEST_CODE_NOTIFICATION_SETTINGS) {
    //         appContext = requireContext().getApplicationContext();

    //         boolean granted = NotificationManagerCompat
    //                 .from(requireContext())
    //                 .areNotificationsEnabled();

    //         Logger.log(appContext, "Returned from settings. Notifications enabled: " + granted);

    //         prefs.setNotificationFlag(granted);
    //         updateUI();
    //     }
    // }

}