package com.deadmole.copytothebox.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // ✅ Use this version
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.deadmole.copytothebox.BuildConfig;
import com.deadmole.copytothebox.R;
import com.deadmole.copytothebox.system.RunnerManager;
import com.deadmole.copytothebox.util.SyncPreferences;

public class ChoicesFragment extends Fragment {

    //private static final boolean debugging = Constants.DEBUG_MODE; // alongside Logger, if used

    // At class level:
    private ActivityResultLauncher<String[]> gsmPermissionLauncher;

    // always define these elements (so we can hide or show them)
    SwitchCompat callDataSwitch;
    SwitchCompat smsDataSwitch;
    SwitchCompat gsmSwitch;

    public ChoicesFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    @SuppressWarnings("ConstantConditions")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_choices, container, false);
        Context appContext = requireContext().getApplicationContext();
        // init default prefs
        SyncPreferences.init(appContext);

        //get ready for GSM perm cycle if nec
        initPermissionLauncher();

        //set the change interval button
        Button intervalButton;
        intervalButton = view.findViewById(R.id.change_interval_button);
        // listener
        intervalButton.setOnClickListener(v -> showIntervalDialog());

        callDataSwitch = view.findViewById(R.id.call_toggle_switch);
        smsDataSwitch = view.findViewById(R.id.sms_toggle_switch);
        gsmSwitch = view.findViewById(R.id.gsm_toggle_switch);
        //default viz
        callDataSwitch.setVisibility(View.GONE);
        smsDataSwitch.setVisibility(View.GONE);
        gsmSwitch.setVisibility(View.GONE);

        // phone version
        if (BuildConfig.FLAVOR.equals("phone")) {
            
            // init mobile prefs if required
            SyncPreferences.init(appContext);
            
            //show switches
            callDataSwitch.setVisibility(View.VISIBLE);
            smsDataSwitch.setVisibility(View.VISIBLE);
            gsmSwitch.setVisibility(View.VISIBLE);
            //listeners
            if (callDataSwitch != null)
                callDataSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleCallsPush(isChecked));

            if (smsDataSwitch != null)
                smsDataSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleSmsPush(isChecked));

            if (gsmSwitch != null)
                gsmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleGSM(isChecked));

        } 

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatus();
    }

    @SuppressWarnings("ConstantConditions")
    private void updateStatus() {

        if (BuildConfig.FLAVOR.equals("phone")) {
            boolean smsDataEnabled = SyncPreferences.getInstance().getPushSMS();
            boolean callDataEnabled = SyncPreferences.getInstance().getPushCalls();
            boolean gsmEnabled = SyncPreferences.getInstance().getUseGSM();

            if (gsmSwitch != null) gsmSwitch.setChecked(gsmEnabled);
            if (smsDataSwitch != null) smsDataSwitch.setChecked(smsDataEnabled);
            if (callDataSwitch != null) callDataSwitch.setChecked(callDataEnabled);
        }

    }
    // interval hours select list
    private void showIntervalDialog() {

        Context appContext = requireContext().getApplicationContext();
        
        String[] options = new String[12];
        for (int i = 0; i < 12; i++) {
            options[i] = (i + 1) + " hour" + (i > 0 ? "s" : "");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Interval")
                .setSingleChoiceItems(options, -1, (dialog, which) -> {
                    int interval = which + 1;
                    SyncPreferences.getInstance().setSyncInterval(interval); // reset settings
                    SyncPreferences.getInstance().updateNextRun();
                    RunnerManager.scheduleNextJob(appContext); // cnx existing and create new one, appContext is good
                    updateStatus();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    //SMS
    private void toggleSmsPush(boolean isChecked) {
        SyncPreferences.getInstance().setPushSMS(isChecked);
    }
    private void toggleCallsPush(boolean isChecked) {
        SyncPreferences.getInstance().setPushCalls(isChecked);
    }
    //GSM
    private void toggleGSM(boolean isChecked) {
        //we are turning GSM use on
        if( isChecked ) {
            //we don't have perm
            if(!hasGsmPermissions()) {
                requestGsmPermissions(); // initPermissionLauncher() handles toggle on sucess
                return; 
            }
        }

        // toggle the flag
        SyncPreferences.getInstance().setUseGSM(isChecked);            
    }
    // GSM permissions helpers
    private boolean hasGsmPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestGsmPermissions() {
        // Show rationale if needed
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GSM Access Required")
                    .setMessage("We need access to your phone state and location to check your mobile signal strength before syncing.")
                    .setPositiveButton("OK", (dialog, which) -> gsmPermissionLauncher.launch(new String[]{
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            gsmPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }
    // Call this in onCreate() or onViewCreated() once to register the launcher
    // handle GSM perm result
    private void initPermissionLauncher() {
        gsmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean phoneStateGranted = result.getOrDefault(Manifest.permission.READ_PHONE_STATE, false);
                    Boolean locationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);

                    if (Boolean.TRUE.equals(phoneStateGranted) && Boolean.TRUE.equals(locationGranted)) {
                        // Permissions granted → toggle GSM on
                        SyncPreferences.getInstance().setUseGSM(true);
                    } else {
                        Toast.makeText(requireContext(),
                                "GSM access denied. Cannot enable GSM features.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
