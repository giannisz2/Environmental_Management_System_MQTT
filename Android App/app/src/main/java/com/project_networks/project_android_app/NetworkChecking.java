package com.project_networks.project_android_app;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

public class NetworkChecking {
    // Context to access application resources and settings
    private final Context context;

    // Handler to run network checking at regular intervals
    private final Handler handler = new Handler();

    // Constructor
    public NetworkChecking(Context context) {
        this.context = context;
    }

    // Method to start checking network periodically
    public void startChecking() {
        handler.post(checkNetworkRunnable);
    }

    // Runnable thread to check network connection at a specific interval
    private final Runnable checkNetworkRunnable = new Runnable() {
        @Override
        public void run() {
            // Check network connectivity
            if (!isConnectedToInternet()) {
                // Show alert if no internet connection is found
                showNetworkAlertDialog();
            }

            // Set interval for the next network check (in milliseconds)
            int CHECK_INTERVAL = 10000;
            // Schedule the next network check
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    // Helper method to check if the device is connected to the internet
    private boolean isConnectedToInternet() {
        // Get the ConnectivityManager service
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            // Get the active network
            Network activeNetwork = cm.getActiveNetwork();

            if (activeNetwork != null) {
                // Get network capabilities for the active network
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);

                // Check if the network has either an INTERNET or WIFI capability
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            }
        }
        return false; // No active network or no internet capability
    }


    // Alert dialog in case Android is offline
    private void showNetworkAlertDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Internet Connection Needed")
                .setMessage("Please enable your connection.")
                // Button to open Wi-Fi settings for enabling internet connection
                .setPositiveButton("Settings", (dialog, which) -> context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                // Negative "Cancel" button to close the dialog without any action
                .setNegativeButton("ΟΚ", (dialog, which) -> dialog.dismiss())
                // Prevent dialog from being dismissed by touching outside
                .setCancelable(false)
                .show();
    }
}
