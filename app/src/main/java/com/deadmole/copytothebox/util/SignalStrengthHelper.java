package com.deadmole.copytothebox.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.List;
import java.util.concurrent.Executor;

/*
    Example usage:
    SignalStrengthHelper helper = new SignalStrengthHelper(context);
    helper.startListening();
    int dbm = helper.getSignalDbm();
    if (helper.isSignalStrongEnough()) { ... }

    Permissions:
        <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
*/

public class SignalStrengthHelper {

    private final TelephonyManager telephonyManager;
    private int currentDbm = Integer.MIN_VALUE;

    public SignalStrengthHelper(Context context) {
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @SuppressLint("MissingPermission")
    public void startListening() {
        Executor mainExecutor = command -> new Handler(Looper.getMainLooper()).post(command);

        PhoneStateListener listener = new PhoneStateListener(mainExecutor) {
            @Override
            public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                currentDbm = getDbmFromCellInfo();
            }
        };

        telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    @SuppressLint("MissingPermission")
    private int getDbmFromCellInfo() {
        int dbm = Integer.MIN_VALUE;
        try {
            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
            if (cellInfos != null) {
                for (CellInfo info : cellInfos) {
                    if (info instanceof CellInfoGsm) {
                        dbm = ((CellInfoGsm) info).getCellSignalStrength().getDbm();
                        break;
                    } else if (info instanceof CellInfoLte) {
                        dbm = ((CellInfoLte) info).getCellSignalStrength().getDbm();
                        break;
                    } else if (info instanceof CellInfoWcdma) {
                        dbm = ((CellInfoWcdma) info).getCellSignalStrength().getDbm();
                        break;
                    } else if (info instanceof CellInfoCdma) {
                        dbm = ((CellInfoCdma) info).getCellSignalStrength().getDbm();
                        break;
                    }
                }
            }
        } catch (Exception ignored) { }
        return dbm;
    }

    public int getSignalDbm() {
        return currentDbm;
    }

    public boolean isSignalStrongEnough() {
        int dbThreshold = Constants.dbThreshold; // eg -95
        return currentDbm != Integer.MIN_VALUE && currentDbm > dbThreshold;
    }
}
