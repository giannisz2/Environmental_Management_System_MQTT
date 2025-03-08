package com.example.menu_test;

import android.annotation.SuppressLint;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.Button;

import android.location.Location;

import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;

import androidx.core.graphics.Insets;

import androidx.annotation.NonNull;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.activity.EdgeToEdge;

import androidx.fragment.app.Fragment;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.Manifest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements smoke_fragment.SmokeSensorListener, gas_fragment.GasSensorListener, uv_fragment.UvSensorListener, temp_fragment.TempSensorListener {
    static MqttClient client;
    Double latitude;
    Double longitude;
    String deviceID;
    NetworkChecking networkMonitor;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private final Handler publishHandler = new Handler(Looper.getMainLooper());
    SwitchCompat mySwitch;
    private Runnable automaticPublishRunnable;
    private String ipAddress;
    private String portNumber;
    TabLayout tabLayout;
    ViewPager2 viewPager2;
    VPAdapter vpAdapter;
    String selectedTopic = "project/iot_data1";
    private boolean settingsLocked = false;
    Integer batteryLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mySwitch = findViewById(R.id.switchGPS);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationCallback();

        checkLocationPermission();

        startBatteryMonitoring();

        EdgeToEdge.enable(this);


        // Back pressed button action
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });


        // Toolbar tabs creation and optimization
        Toolbar toolbar = findViewById(R.id.toolBar);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tablayout);
        viewPager2 = findViewById(R.id.viewpager);
        vpAdapter = new VPAdapter(this);
        viewPager2.setAdapter(vpAdapter);

        new TabLayoutMediator(tabLayout, viewPager2,
                (tab, position) -> tab.setText(vpAdapter.getPageTitle(position))
        ).attach();

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Objects.requireNonNull(tabLayout.getTabAt(position)).select();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Retrieve manual mode coordinates if available
        Intent intent = getIntent();
        if (intent.hasExtra("MANUAL_LATITUDE") && intent.hasExtra("MANUAL_LONGITUDE")) {
            longitude = intent.getDoubleExtra("MANUAL_LATITUDE", 0.0);
            latitude = intent.getDoubleExtra("MANUAL_LONGITUDE", 0.0);
            Toast.makeText(this, "Manual GPS Mode: " + latitude + ", " + longitude, Toast.LENGTH_LONG).show();
        }


        // Start checking if app is online
        networkMonitor = new NetworkChecking(this);
        networkMonitor.startChecking();


        // Switch listener for GPS mode
        mySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startLocationUpdates();
                Toast.makeText(MainActivity.this, "Automatic GPS enabled.", Toast.LENGTH_SHORT).show();
            } else {
                stopLocationUpdates();
                setManualCoordinates();  // Set default manual coordinates when switching to manual mode
                Toast.makeText(MainActivity.this, "Manual GPS mode activated.", Toast.LENGTH_SHORT).show();
            }
        });

    }


    // D.I.T. Coordinates
    private void setManualCoordinates() {
        double[][] predefinedCoordinates = {
                {37.96809452684323, 23.76630586399502},
                {37.96799937191987, 23.766603589104385},
                {37.967779456380754, 23.767174897611685},
                {37.96790421900921, 23.76626294807113}
        };

        // Pick a random pair of coordinates
        Random random = new Random();
        int index = random.nextInt(predefinedCoordinates.length);

        latitude = predefinedCoordinates[index][0];
        longitude = predefinedCoordinates[index][1];

        // Notify the user
        Toast.makeText(this, "Manual GPS Mode: " + latitude + ", " + longitude, Toast.LENGTH_LONG).show();
    }


    // Create Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return true;
    }


    // Usage of set button in connection settings
    public void connectClicked(View view) {

        // Validate IP and Port before attempting connection
        if (ipAddress == null || portNumber == null || ipAddress.isEmpty() || portNumber.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter a valid IP and Port.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int port = Integer.parseInt(portNumber);
            if (port < 1 || port > 65535) {
                Toast.makeText(MainActivity.this, "Port number must be between 1 and 65535.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Invalid port number format.", Toast.LENGTH_SHORT).show();
            return;
        }


        // Set context for toast messages
        Context context = MainActivity.this;


        // Build the Server URL
        String serverURL = "tcp://" + ipAddress + ":" + portNumber;
        Toast.makeText(context, "Attempting to connect to: " + serverURL, Toast.LENGTH_SHORT).show();


        runOnUiThread(() -> {
            try {
                // Set options for Mqtt Service
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);

                // Generate unique device ID
                deviceID = MqttClient.generateClientId();

                // Initialize MQTT client
                client = new MqttClient(serverURL, deviceID, new MemoryPersistence());

                // Set callback to handle messages and connection loss
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        runOnUiThread(() ->
                                Toast.makeText(context, "Connection lost: " + cause.getMessage(), Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String payload = new String(message.getPayload());
                        runOnUiThread(() -> Toast.makeText(context, "MQTT Message: " + payload, Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                // Attempt to connect
                client.connect(options);

                runOnUiThread(() -> Toast.makeText(context, "Connected to server: " + serverURL, Toast.LENGTH_SHORT).show());


                // Start sending data to the server
                startLocationPublishing();

            } catch (MqttException e) {
                Log.e("MQTT Connection", "Connection failed", e);
                runOnUiThread(() -> Toast.makeText(context, "Failed to connect. Check IP and Port.", Toast.LENGTH_LONG).show());
            }
        });
    }

    // Action of cancel button in connection settings
    public void disconnectClicked(View view) {
        if (client == null) {
            Toast.makeText(MainActivity.this, "Not connected to any server.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (client.isConnected()) {
                client.disconnect();
                Toast.makeText(MainActivity.this, "Disconnected from MQTT server.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Already disconnected.", Toast.LENGTH_SHORT).show();
            }
        } catch (MqttException e) {
            Log.e("MQTT Disconnect", "Error while disconnecting", e);
            Toast.makeText(MainActivity.this, "Error disconnecting: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Stop all background publishing tasks
        publishHandler.removeCallbacksAndMessages(null);

        // Reset the client to avoid referencing an old instance
        client = null;
    }


    // Menu Options
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        SwitchCompat mySwitch = findViewById(R.id.switchGPS);
        int id = item.getItemId();

        // Change activity when pressing Manual Mode in menu
        if (id == R.id.manual_mode && !mySwitch.isChecked()) {
            showManualCoordinateDialog(); // Open input dialog
            return true;
        } else if(id == R.id.manual_mode && mySwitch.isChecked()){
            Toast.makeText(MainActivity.this, "You must disable automatic GPS first", Toast.LENGTH_SHORT).show();

        }

        if(id == R.id.new_sensor) {
            showNewSensorDialog();
            return true;
        } else if (id == R.id.settings){
            showSettingsDialog();
            return true;
        } else if (id == R.id.exit) {
            showExitConfirmationDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    // Manual Mode setting coordinates
    private void showManualCoordinateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Manual Coordinates");

        // Inflate the custom dialog layout
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.activity_manual_mode_screen, null);
        builder.setView(viewInflated);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Find input fields and buttons by their IDs
        EditText latitudeInput = viewInflated.findViewById(R.id.editTextLatitude);
        EditText longitudeInput = viewInflated.findViewById(R.id.editTextLongitude);
        Button setCoordinatesButton = viewInflated.findViewById(R.id.buttonSet);
        Button backButton = viewInflated.findViewById(R.id.buttonBack);

        // Set Button (acts as OK) with validation
        setCoordinatesButton.setOnClickListener(v -> {
            try {
                double newLatitude = Double.parseDouble(latitudeInput.getText().toString());
                double newLongitude = Double.parseDouble(longitudeInput.getText().toString());

                // Validate longitude and latitude
                if (newLatitude < -90 || newLatitude > 90) {
                    Toast.makeText(MainActivity.this, "Invalid Latitude! Must be between -90 and 90.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newLongitude < -180 || newLongitude > 180) {
                    Toast.makeText(MainActivity.this, "Invalid Longitude! Must be between -180 and 180.", Toast.LENGTH_SHORT).show();
                    return;
                }

                latitude = newLatitude;
                longitude = newLongitude;


                Toast.makeText(MainActivity.this, "Manual Coordinates Updated: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
                dialog.dismiss(); // Close the dialog after setting coordinates
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid input! Please enter numeric values.", Toast.LENGTH_SHORT).show();
            }
        });

        // Back Button for Manual Mode
        backButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Manual coordinate input canceled", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }


    // List to store all active sensors dynamically
    private List<SensorData> activeSensors = new ArrayList<>();

    // SensorData class to store sensor details
    class SensorData {
        String type;
        double value;

        public SensorData(String type, double value) {
            this.type = type;
            this.value = value;
        }
    }


    // Automatic mode setting coordinates

    private void startLocationPublishing() {
        automaticPublishRunnable = new Runnable() {
            @Override
            public void run() {
                if (mySwitch.isChecked()) {
                    getLocation(); // Update GPS if in automatic mode
                }

                publishData(); // Send all sensor values dynamically

                publishHandler.postDelayed(this, 1000); // Publish every 1 second
            }
        };

        publishHandler.removeCallbacks(automaticPublishRunnable);
        publishHandler.post(automaticPublishRunnable);
    }

    // Publishing all data to the MQTT server
    private void publishData() {
        if (client == null || !client.isConnected()) {
            Log.e("MQTT Publish", "Client not connected. Cannot send data.");
            return;
        }

        if (latitude == null || longitude == null) {
            Log.e("MQTT Publish", "Invalid location data. Skipping publish.");
            return;
        }

        if (mySwitch.isChecked()) {  // If manual mode is active, uses manual coordinates
            getLocation();
        }

        //  Clear activeSensors to avoid duplicates before adding new values
        List<SensorData> sensorsToSend = new ArrayList<>(activeSensors);
        activeSensors.clear();

        // Start CSV message with latitude and longitude
        StringBuilder csvRow = new StringBuilder();
        csvRow.append(deviceID).append(" ").append(batteryLevel).append(" ").append(latitude).append(" ").append(longitude);

        // Add all unique active sensors dynamically
        for (SensorData sensor : sensorsToSend) {
            csvRow.append(" ").append(sensor.type).append(" ").append(sensor.value);
        }


        // Send MQTT message
        MqttMessage message = new MqttMessage(csvRow.toString().getBytes());
        message.setQos(2);

        try {
            client.publish(selectedTopic, message);
            Log.d("MQTT Publish", "Sent CSV: " + csvRow);
        } catch (MqttException e) {
            Log.e("MQTT Publish", "Failed to publish message", e);
        }
    }


    // Check if location permissions are granted, if not, request them
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissions are already granted, get location
            getLocation();
        }
    }

    // Result of location permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation(); // GPS is allowed, continue normal operation
            } else {
                // GPS permission was denied so it forces manual mode to the user
                mySwitch.setChecked(false);
                mySwitch.setEnabled(false);
                setManualCoordinates(); // Set random manual coordinates
                Toast.makeText(this, "GPS permission denied. Manual mode only.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getLocation() {
        if (!mySwitch.isChecked()) {  // If manual mode is active, do not update location
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Finding location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                    } else {
                        Toast.makeText(MainActivity.this, "Unable to find location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    // Callbacks for lifecycle of activity
    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();// Start updates when activity is resumed
        startBatteryMonitoring();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates(); // Stop updates when activity is paused
        stopBatteryMonitoring();
    }

    // GPS permission check
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If GPS permission denied, do NOT start updates
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(1000)
                .setMinUpdateIntervalMillis(500)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        };
    }

    //Battery percentage acquisition
    private static final int BATTERY_UPDATE_INTERVAL = 1000; // 1 second
    private final Handler batteryHandler = new Handler();

    private void startBatteryMonitoring() {
        batteryHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendBatteryPercentageToMQTT(); // Fetch battery data and send it
                batteryHandler.postDelayed(this, BATTERY_UPDATE_INTERVAL);
            }
        }, 0);
    }

    private void sendBatteryPercentageToMQTT() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            Log.e("BatteryMonitor", "BatteryManager is unavailable.");
        }
    }

    private void stopBatteryMonitoring() {
        batteryHandler.removeCallbacksAndMessages(null);
        Log.d("BatteryMonitor", "Battery monitoring stopped.");
    }


    // Method to create new Sensor
    @SuppressLint("InflateParams")
    private void showNewSensorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setTitle("New Sensor");
        builder.setView(inflater.inflate(R.layout.dialog_new_sensor, null)).setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog alertDialog = (AlertDialog) dialog;
                EditText sensorTypeEditText = alertDialog.findViewById(R.id.sensor_type_edittext);
                EditText startValueEditText = alertDialog.findViewById(R.id.sensor_value_start_edittext);
                EditText endValueEditText = alertDialog.findViewById(R.id.sensor_value_end_edittext);

                if(sensorTypeEditText != null && startValueEditText != null && endValueEditText != null) {
                    String sensorType = sensorTypeEditText.getText().toString();
                    String startValueString = startValueEditText.getText().toString();
                    String endValueString = endValueEditText.getText().toString();

                    if(!sensorType.isEmpty() && !startValueString.isEmpty() && !endValueString.isEmpty()) {
                        try {
                            double startValue = Double.parseDouble(startValueString);
                            double endValue = Double.parseDouble(endValueString);
                            if (!sensorType.equalsIgnoreCase("Smoke") &&
                                    !sensorType.equalsIgnoreCase("Gas") &&
                                    !sensorType.equalsIgnoreCase("UV") &&
                                    !sensorType.equalsIgnoreCase("Temp")) {
                                Toast.makeText(MainActivity.this, "Invalid sensor type. Please enter Smoke, Gas, UV, or Temp.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if ("Smoke".equalsIgnoreCase(sensorType)) {
                                if (startValue < 0 || endValue > 0.25 || startValue >= endValue) {
                                    Toast.makeText(MainActivity.this, "Smoke values must be between 0 and 0.25", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } else if ("Gas".equalsIgnoreCase(sensorType)) {
                                if (startValue < 0 || endValue > 11 || startValue >= endValue) {
                                    Toast.makeText(MainActivity.this, "Gas values must be between 0 and 11", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } else if ("UV".equalsIgnoreCase(sensorType)) {
                                if (startValue < 0 || endValue > 11 || startValue >= endValue) {
                                    Toast.makeText(MainActivity.this, "UV values must be between 0 and 11", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } else if ("Temp".equalsIgnoreCase(sensorType)) {
                                if (startValue < -5 || endValue > 80 || startValue >= endValue) {
                                    Toast.makeText(MainActivity.this, "Temperature values must be between -5 and 80", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            Fragment newFragment = null;

                            if("Smoke".equalsIgnoreCase(sensorType)) {
                                newFragment = new smoke_fragment();
                            }else if("Gas".equalsIgnoreCase(sensorType)) {
                                newFragment = new gas_fragment();
                            }else if("UV".equalsIgnoreCase(sensorType)) {
                                newFragment = new uv_fragment();
                            } else if ("Temp".equalsIgnoreCase(sensorType)) {
                                newFragment = new temp_fragment();
                            }

                            if(newFragment != null) {
                                vpAdapter.addFragment(newFragment, sensorType);

                                new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> tab.setText(vpAdapter.getPageTitle(position))).attach();

                                viewPager2.setCurrentItem(vpAdapter.getItemCount() - 1, true);
                            }
                        } catch(NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
                        }

                    }else {
                        Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Sensor value updater
    private void updateSensorValue(String type, double value) {
        activeSensors.removeIf(sensor -> sensor.type.equals(type));

        // Add updated sensor value
        activeSensors.add(new SensorData(type, value));

        Log.d("Sensor Update", "Updated sensor: " + type + " -> " + value);
    }

    @Override
    public void onSmokeSensorValueChanged(double value) {
        updateSensorValue("OD", value);
    }

    @Override
    public void onGasSensorValueChanged(double value) {
        updateSensorValue("GAS", value);
    }

    @Override
    public void onUvSensorValueChanged(double value) {
        updateSensorValue("UV", value);
    }

    @Override
    public void onTempSensorValueChanged(double value) {
        updateSensorValue("TEMP", value);
    }


    // Method for getting ip, port and topic from user
    private void showSettingsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final android.view.View dialogView = inflater.inflate(R.layout.dialog_settings, null);

        final EditText ipEditText = dialogView.findViewById(R.id.ip_edittext);
        final EditText portEditText = dialogView.findViewById(R.id.port_edittext);
        final RadioGroup topicRadioGroup = dialogView.findViewById(R.id.topic_radiogroup);

        // Pre-fill fields with the last entered values
        ipEditText.setText(ipAddress);
        portEditText.setText(portNumber);

        // Keep the previously selected topic checked
        if (selectedTopic.equals("project/iot_data1")) {
            topicRadioGroup.check(R.id.topic1_radiobutton);
        } else if (selectedTopic.equals("project/iot_data2")) {
            topicRadioGroup.check(R.id.topic2_radiobutton);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connection Settings")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)

                .setPositiveButton("OK", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        //Ok and cancel button behaviour
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        final Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);


        if (settingsLocked) {
            okButton.setEnabled(false);
        }


        okButton.setOnClickListener(v -> {
            if (!settingsLocked) {
                ipAddress = ipEditText.getText().toString().trim();
                portNumber = portEditText.getText().toString().trim();
                int topicSelection = topicRadioGroup.getCheckedRadioButtonId();

                if (topicSelection == R.id.topic1_radiobutton) {
                    selectedTopic = "project/iot_data1";
                } else if (topicSelection == R.id.topic2_radiobutton) {
                    selectedTopic = "project/iot_data2";
                }

                connectClicked(null);
                settingsLocked = true;
                okButton.setEnabled(false);
                dialog.dismiss();


                Toast.makeText(MainActivity.this, "IP: " + ipAddress + "\nPort: " + portNumber, Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Topic: " + selectedTopic, Toast.LENGTH_SHORT).show();
            }
        });


        cancelButton.setOnClickListener(v -> {
            settingsLocked = false;
            okButton.setEnabled(true);
            dialog.dismiss();
            disconnectClicked(null);
        });
    }


    //Back button double press working like exit
    private long backPressedTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000;

    private void handleBackPress() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - backPressedTime < BACK_PRESS_INTERVAL) {
            // Show exit confirmation dialog on second tap
            showExitConfirmationDialog();
        } else {
            // First tap does nothing, just updates time
            backPressedTime = currentTime;
        }
    }

    // Method to close and kill app
    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> killApp()) // Call killApp()
                .setNegativeButton("No", null) // Do nothing if "No" is pressed
                .show();
    }

    private void killApp() {
        // Finish all activities and remove from recent apps
        finishAndRemoveTask();

        // Kill the app process
        android.os.Process.killProcess(android.os.Process.myPid());

        // Stop all background services
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.killBackgroundProcesses(getPackageName());
        }

        // Exit the application completely
        System.exit(0);
    }

}