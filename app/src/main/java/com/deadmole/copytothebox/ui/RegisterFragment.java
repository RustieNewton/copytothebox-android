// RegisterFragment.java
package com.deadmole.copytothebox.ui;

import android.content.Context;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//import java.nio.charset.StandardCharsets; // url encoding
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.deadmole.copytothebox.R;
import com.deadmole.copytothebox.util.Constants;
import com.deadmole.copytothebox.util.Logger;
import com.deadmole.copytothebox.util.PathUtils;
import com.deadmole.copytothebox.util.SyncPreferences;
import com.deadmole.copytothebox.util.AuthPreferences;
import com.deadmole.copytothebox.util.NetworkUtils;

public class RegisterFragment extends Fragment {

    private EditText usernameEdit, passwordEdit;
    private Context appContext; //def but don't instantiate yet
    private static final Boolean debugging = Constants.DEBUG_MODE;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        appContext = requireContext().getApplicationContext(); //this is safe inside a method, nb diff to MainActivity handling

        View view = inflater.inflate(R.layout.fragment_register, container, false);
        usernameEdit = view.findViewById(R.id.edit_username);
        passwordEdit = view.findViewById(R.id.edit_password);
        view.findViewById(R.id.registration_status_report);
        Button registerBtn = view.findViewById(R.id.btn_register);

        // Initialize preferences ONCE here
        SyncPreferences.init(appContext);// usage SyncPreferences.getInstance().isFirstTime()
        AuthPreferences.init(appContext); 

        // listener for the one button
        registerBtn.setOnClickListener(v -> performRegister(view));

        // report
        if(debugging) Logger.log(appContext, "Register fragment opened");

        return view;
    }
    //the onViewCreated method is req because we have to wait for the view to load before calling updateStatus()
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Now the view hierarchy is ready — safe to update UI here
        updateStatus(view);
    }
    private void updateStatus(View view) {

        boolean isRegistered = SyncPreferences.getInstance().getRegisteredFlag();
        appContext = requireContext().getApplicationContext(); //this is safe inside a method, nb diff handling to MainActivity.

        if(debugging) Logger.log(appContext, isRegistered
            ? "App is registered"
            : "App not registered");

        // Get reference to the TextView
        TextView statusReport = view.findViewById(R.id.registration_status_report);

        // Update UI text based on registration status
        if (statusReport != null) {
            if (isRegistered) {
                statusReport.setText(R.string.registered_report);
            } else {
                statusReport.setText(R.string.not_registered_report);
            }
        } else {
            if(debugging) Logger.log(appContext, "Failed to find status report TextView.");
        }
    }
    private void performRegister(View view) {

        appContext = requireContext().getApplicationContext();

        //check connectivity
        boolean wifiAvailable = NetworkUtils.isWifiAvailable(appContext);
        boolean gsmAvailable = NetworkUtils.isMobileDataAvailable(appContext);
        if( !wifiAvailable && !gsmAvailable ){
            //bail out with logging and toast
            if(debugging) Logger.log(appContext, "registration failed - no connection");
            Toast.makeText(appContext, "No connection - enable wifi or data in settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        String username = usernameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(appContext, "Username and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            //boolean success = false;

            try {
                String REGISTER_URL = PathUtils.getRegisterUrl();
                String query = String.format("u=%s&p=%s",
                        URLEncoder.encode(username, "UTF-8"), // don't use StandardCharsets...
                        URLEncoder.encode(password, "UTF-8"));
                URL url = new URL(REGISTER_URL + "?" + query);
                if (debugging) Logger.log(appContext, "Sending reg req to this url " + url);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setDoOutput(false);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                    String errMessage = jsonResponse.has("errors") ? jsonResponse.get("errors").getAsString() : "";

                    if (Objects.equals(errMessage, "none")) {
                        String publicDomain = jsonResponse.has("publicDomain") ? jsonResponse.get("publicDomain").getAsString() : "";
                        String serverID = jsonResponse.has("serverID") ? jsonResponse.get("serverID").getAsString() : "";
                        String rsyncID = jsonResponse.has("rsyncID") ? jsonResponse.get("rsyncID").getAsString() : "";

                        AuthPreferences.getInstance().setServerID(serverID);
                        AuthPreferences.getInstance().setRsyncID(rsyncID);
                        AuthPreferences.getInstance().setPublicDomain(publicDomain);
                        SyncPreferences.getInstance().setRegisteredFlag();

                        //success = true;
                        handler.post(() -> {
                            Toast.makeText(appContext, "Registration successful", Toast.LENGTH_LONG).show();
                            if(debugging) Logger.log(appContext, "Registration worked");
                            updateStatus(view);
                        });
                    } else {
                        handler.post(() -> {
                            Toast.makeText(appContext, "Registration failed: invalid username or password", Toast.LENGTH_LONG).show();
                            if(debugging) Logger.log(appContext, "Registration failed: INVALID username or password");
                            updateStatus(view);
                        });
                    }
                } else {
                    handler.post(() -> {
                        Toast.makeText(appContext, "Registration failed. HTTP code: " + responseCode, Toast.LENGTH_LONG).show();
                        if(debugging) Logger.log(appContext, "Registration failed. Response code: " + responseCode);
                        updateStatus(view);
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(appContext, "Registration failed. You may need to sign into wifi.", Toast.LENGTH_LONG).show();
                    if(debugging) Logger.log(appContext, "Register failed: " + Log.getStackTraceString(e));
                    updateStatus(view);
                });
            }
        });
    }

}
