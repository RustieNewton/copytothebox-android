package com.deadmole.copytothebox.ui;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Context;
import android.os.Bundle;

import java.util.Date;
import java.util.Locale;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.ocpsoft.prettytime.PrettyTime;  // time display

import com.deadmole.copytothebox.R;
import com.deadmole.copytothebox.util.Constants;
import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.SyncPreferences;
import com.deadmole.copytothebox.util.AuthPreferences;
import com.deadmole.copytothebox.runner.Runner;
import com.deadmole.copytothebox.system.RunnerManager;

public class MainActivity extends AppCompatActivity {

    private static final Boolean debugging = Constants.DEBUG_MODE;

    private Context appContext;
    private TextView lastRunTimeView, nextRunTimeView, syncIntervalView; //alarmStatusView, registrationStatusView, 
    private ActionBar actionBar; // used everywhere
    private SwitchCompat enabledSwitch; // used in updateStatus() and onCreate()

    // new 30 Jul to fix running enableApp each time the UI is opened
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = getApplicationContext();
        setContentView(R.layout.activity_main);

        initPreferences();
        setupToolbar();
        bindViews();

        // ✅ Initialize switch state before attaching listener
        boolean initialEnabledState = SyncPreferences.getInstance().getSyncEnabled();
        enabledSwitch.setChecked(initialEnabledState);

        setupListeners();

        if (!SyncPreferences.getInstance().isSetupComplete()) {
            showSetupFragment();
        } else {
            showStatusPanel();
            loadStatus();
        }
    }

    private void setupListeners() {
        // Run now button
        findViewById(R.id.run_now_btn).setOnClickListener(v -> runNow());

        // Toggle app on and off
        // ✅ Detach listener first to avoid duplicate callbacks
        enabledSwitch.setOnCheckedChangeListener(null);

        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableApp(isChecked);
        });
    }

    public void onResume() {
        super.onResume();
        loadStatus();
    }

    // ~~~~~~~~~~~~~ listeners and methods ~~~~~~~~~~~~~~~~~
    private void runNow() {
        if(debugging) Logger.log( appContext, "starting sync from UI");
        ProgressBar spinner = findViewById(R.id.progressSpinner);
        
        // Show spinner on UI thread
        runOnUiThread(() -> spinner.setVisibility(View.VISIBLE));

        new Thread(() -> {
            boolean success = Runner.run(appContext);

            // Use UI thread to update UI
            runOnUiThread(() -> {
                spinner.setVisibility(View.GONE); // Hide spinner

                if (success) {
                    Toast.makeText(this, "Run succeeded", Toast.LENGTH_SHORT).show();
                    loadStatus();
                } else {
                    Toast.makeText(this, "Run failed", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    private void enableApp(Boolean isChecked) {
        Logger.log(appContext,"enabling app with RunnerManager from MainActivity");
        if (isChecked) {
            RunnerManager.enable(appContext);
        } else {
            RunnerManager.disable(appContext);
        }
        loadStatus();
    }

    // ~~~ helper methods ~~~~~~~~~~~~~~~~
    private void initPreferences() {
        SyncPreferences.init(appContext);
        AuthPreferences.init(appContext);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
    }

    private void bindViews() {
        lastRunTimeView = findViewById(R.id.last_run_report);
        nextRunTimeView = findViewById(R.id.next_run_report);
        syncIntervalView = findViewById(R.id.interval_status_report);
        enabledSwitch = findViewById(R.id.enable_toggle_switch);
    }
    private void showSetupFragment() {
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        // Avoid adding if SetupFragment is already displayed
        if (!(currentFragment instanceof SetupFragment)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SetupFragment(), "SetupFragment")
                    .commit();

            // Hide status panel if visible
            findViewById(R.id.status_panel).setVisibility(View.GONE);
            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle("Setup");
            }
        }
    }

    private void showStatusPanel() {
        findViewById(R.id.status_panel).setVisibility(View.VISIBLE);
        findViewById(R.id.fragment_container).setVisibility(View.GONE);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle("CopyToTheBox");
        }
    }

    private void loadStatus() {
        //we need the app status
        //if disabled, the next run time is never
        boolean isEnabled = SyncPreferences.getInstance().getSyncEnabled();

        // run times
        PrettyTime prettyTime = new PrettyTime(Locale.getDefault());

        //last run
        String lastRunStr;
        long lastRun = SyncPreferences.getInstance().getLastRun();
        if(lastRun==0) {
            lastRunStr = "Never run";
        } else {
            Date lastRunDate = new Date(lastRun);
            lastRunStr = prettyTime.format(lastRunDate);
        }
        lastRunTimeView.setText(lastRunStr);

        // next run
        String nextRunStr;
        long nextRun = SyncPreferences.getInstance().getNextRun();
        if(nextRun==0) {
            nextRunStr = "Never run";
        } else if (!isEnabled) {
            nextRunStr = "App turned off";
        }
        else {
            Date nextRunDate = new Date(nextRun);
            nextRunStr = prettyTime.format(nextRunDate);
        }
        nextRunTimeView.setText(nextRunStr);

        // minimum interval between runs
        int interval = SyncPreferences.getInstance().getSyncInterval();
        String intervalStr =  interval + " hour(s)";
        syncIntervalView.setText(intervalStr);

        //set the enable toggle
        boolean isSyncEnabled = SyncPreferences.getInstance().getSyncEnabled();
        enabledSwitch.setChecked(isSyncEnabled);

    }
    // menu helper
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    // menu handler
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        String title = "";

        int id = item.getItemId();

        if (id == R.id.menu_status) {
            if (!SyncPreferences.getInstance().isSetupComplete()) {
                Toast.makeText(this, "Please complete setup before using Status", Toast.LENGTH_SHORT).show();
                showSetupFragment();  // Use clean method
                return true;
            }

            // Show status panel normally
            findViewById(R.id.status_panel).setVisibility(View.VISIBLE);
            findViewById(R.id.fragment_container).setVisibility(View.GONE);

            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle("CopyToTheBox");
            }

            loadStatus();
            return true;
        }

        // Other menu options
        if (id == R.id.menu_register) {
            fragment = new RegisterFragment();
            title = "Register";
        } else if (id == R.id.menu_about) {
            fragment = new AboutFragment();
            title = "How it works";
        } else if (id == R.id.menu_logs) {
            fragment = new LogFragment();
            title = "Logs";
        } else if (id == R.id.menu_choices) {
            fragment = new ChoicesFragment();
            title = "Choices";
        }
        else if (id == R.id.menu_setup) {
            showSetupFragment();  // Direct to setup from menu
            return true;
        }

        if (fragment != null) {
            findViewById(R.id.status_panel).setVisibility(View.GONE);
            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();

            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(title);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // navigation helper
    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        if (actionBar != null) {
            actionBar.setTitle("CopyToTheBox");
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        findViewById(R.id.fragment_container).setVisibility(View.GONE);
        findViewById(R.id.status_panel).setVisibility(View.VISIBLE);
        return true;
    }
}