package com.project_networks.project_android_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.Manifest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Random;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.text.DecimalFormatSymbols;



public class MainActivity extends AppCompatActivity {
    static MqttClient client;
    Double longitude;
    Double latitude;
    String deviceID;
    NetworkChecking networkMonitor;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private final Handler publishHandler = new Handler(Looper.getMainLooper());
    SwitchCompat mySwitch;
    private Runnable automaticPublishRunnable;
    private Runnable csvPublishRunnable;
    public static int manual_mode_seconds = 0;
    InputStream inputStream;
    public boolean isAutomaticPublishing = false; // Flag to avoid multiple executions of thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        manual_mode_seconds = getIntent().getIntExtra("SECONDS", 0); // 0 is the default value if SECONDS_KEY is not found

        // Initialize the FusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Set up the location callback
        setupLocationCallback();
        // Check and request location permissions
        checkLocationPermission();



        // Start checking if app is online
        networkMonitor = new NetworkChecking(this);
        networkMonitor.startChecking();



        // Create a Random number
        Random random = new Random();
        int x = random.nextInt(2); // Generate either 0 or 1


        if(x == 0) {
            inputStream = MainActivity.this.getResources().openRawResource(R.raw.android_1);
        } else {
            inputStream = MainActivity.this.getResources().openRawResource(R.raw.android_2);
        }

        XmlToCsvConverter.convertXmlToCsv(MainActivity.this, inputStream,"android_coordinates.csv");


        // Initialize the switch
        mySwitch = findViewById(R.id.switchGPS);

        // Set up the switch listener to toggle between modes
        mySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Switch is ON: Start automatic GPS publishing
                publishHandler.removeCallbacks(csvPublishRunnable);
                publishHandler.post(automaticPublishRunnable);
                Toast.makeText(MainActivity.this, "Automatic GPS publishing.", Toast.LENGTH_SHORT).show();
            } else {
                // Switch is OFF: Start CSV-based manual publishing
                publishHandler.removeCallbacks(automaticPublishRunnable);
                publishHandler.post(csvPublishRunnable);
                Toast.makeText(MainActivity.this, "Publishing from CSV.", Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void connectClicked(View view) {
        EditText serverIP = findViewById(R.id.editTextSelectServer);
        EditText port = findViewById(R.id.editTextPort);
        Button connectButton = findViewById(R.id.connectButton);
        Button disconnectButton = findViewById(R.id.buttonDisconnect);

        // Set context for toast messages
        Context context = MainActivity.this;

        String ipAddress = serverIP.getText().toString().trim();
        String portString = port.getText().toString().trim();

        // Validate IP address and port number
        if (ipAddress.isEmpty() || portString.isEmpty()) {
            Toast.makeText(context, "Please enter both server IP and port.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the port is a valid number
        int portNumber;
        try {
            portNumber = Integer.parseInt(portString);
            if (portNumber < 1 || portNumber > 65535) {
                Toast.makeText(context, "Please enter a valid port number (1-65535).", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(context, "Port must be a valid number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the broker URL
        String brokerURL = "tcp://" + ipAddress + ":" + portNumber;
        Toast.makeText(context, "Attempting to connect to: " + brokerURL, Toast.LENGTH_SHORT).show();

        runOnUiThread(() -> {
            try {

                // Set options for Mqtt Service
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setAutomaticReconnect(true);
                options.setConnectionTimeout(10);  // Timeout in seconds

                // Random ID
                deviceID = MqttClient.generateClientId();

                // Receive client
                client = new MqttClient(brokerURL, deviceID, new MemoryPersistence());

                // Set callback to handle messages and connection loss
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.e("MQTT Error", "Connection lost: " + cause.getMessage(), cause);
                        runOnUiThread(() -> {
                            Toast.makeText(context, "MQTT Connection lost.", Toast.LENGTH_LONG).show();
                            disconnectButton.setVisibility(View.GONE);
                            connectButton.setVisibility(View.VISIBLE);
                        });
                    }


                    private String last_payload = "";  // Move outside to persist between calls
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String payload = new String(message.getPayload());

                        // Check if the payload is the same as the last one
                        if (payload.equals(last_payload)) return;

                        if (payload.isEmpty()) {
                            Log.e("MQTT Error", "Received empty payload");
                            return;
                        }

                        // Split the payload by space to separate the risk level and the distance
                        String[] parts = payload.split(" ");

                        // Check if the payload contains both risk level and distance
                        if (parts.length < 2) {
                            Log.e("MQTT Error", "Invalid payload format: " + payload);
                            return;
                        }

                        String riskLevel = parts[0];

                        // Ensure locale consistency
                        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
                        DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));

                        String messageWithCorrectDecimal = parts[1].trim();
                        Log.d("MQTT Debug", "Original distance string: '" + parts[1] + "' | After trim: '" + messageWithCorrectDecimal + "'");
                        Log.d("MQTT Debug", "System Locale: " + Locale.getDefault());

                        double distance;
                        try {
                            Number parsedNumber = numberFormat.parse(messageWithCorrectDecimal);
                            assert parsedNumber != null;
                            distance = parsedNumber.doubleValue();
                            Log.d("MQTT Debug", "Parsed distance: " + distance);

                            // Format distance to two decimal places
                            String formattedDistance = df.format(distance);
                            Log.d("Distance", "Distance: " + formattedDistance);

                            handleRiskAlert(riskLevel, Double.parseDouble(formattedDistance));

                            // Update last payload after successful parsing
                            last_payload = payload;
                        } catch (ParseException e) {
                            Log.e("MQTT Error", "Invalid number format for distance: '" + messageWithCorrectDecimal + "'", e);
                        }
                    }



                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });


                // Connect to broker
                new Thread(() -> {
                    try {
                        client.connect(options);
                        Log.d("MQTT Debug", "Connected successfully to: " + brokerURL);
                    } catch (MqttException e) {
                        Log.e("MQTT Error", "Failed to connect", e);
                        runOnUiThread(() -> Toast.makeText(context, "Failed to connect to broker. Please check the URL and try again.", Toast.LENGTH_LONG).show());
                        connectButton.setVisibility(View.VISIBLE);
                    }
                }).start();


                runOnUiThread(() -> Toast.makeText(context, "Connected to broker: " + brokerURL, Toast.LENGTH_SHORT).show());

                // Subscribe to topic to receive server message
                String topic = "project/server_message";
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        client.subscribe(topic);
                        Log.d("MQTT Debug", "Subscribed to topic: project/server_message");
                    } catch (MqttException e) {
                        Log.e("MQTT Error", "Subscription failed", e);
                    }
                }, 2000);  // Delay for stability

                // Publish a message to the topic after subscribing
                startLocationPublishing();

                // Disconnect button appears and connect button disappears
                connectButton.setVisibility(View.GONE);
                disconnectButton.setVisibility(View.VISIBLE);

            } catch (MqttException e) {
                Log.e("MQTT Error", "Connection failed", e);

                int reasonCode = e.getReasonCode();
                String errorMessage = e.getMessage();
                String detailedMessage = "Connection error: " + errorMessage + "\nReason code: " + reasonCode;

                // Display the detailed error message
                runOnUiThread(() -> {
                    Toast.makeText(context, detailedMessage, Toast.LENGTH_LONG).show();

                    // Disconnect button disappears and connect button appears
                    disconnectButton.setVisibility(View.GONE);
                    connectButton.setVisibility(View.VISIBLE);
                });
            }
        });


    }


    // Action of disconnect button
    public void disconnectClicked(View view) throws MqttException {
        Button connectButton = findViewById(R.id.connectButton);
        Button disconnectButton = findViewById(R.id.buttonDisconnect);
        if (client.isConnected()) {
            MqttMessage message = new MqttMessage("BYE".getBytes());
            client.publish("project/long_lat_deviceID", message);
            client.disconnect();
            Toast.makeText(MainActivity.this, "Broker disconnected", Toast.LENGTH_SHORT).show();
            // Stop both runnable threads for publishing
            publishHandler.removeCallbacks(automaticPublishRunnable);
            publishHandler.removeCallbacks(csvPublishRunnable);

        }
        // Disconnect button disappears and connect button appears
        connectButton.setVisibility(View.VISIBLE);
        disconnectButton.setVisibility(View.GONE);

    }


    // Set automatic GPS or manual mode for GPS

    // Change activity when pressing Manual Mode in menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        SwitchCompat mySwitch = findViewById(R.id.switchGPS);

        if (id == R.id.manual_mode && !mySwitch.isChecked()) {
            Intent toTimeIntervalScreen = new Intent(MainActivity.this, ManualMode.class);
            startActivity(toTimeIntervalScreen);
            return true;
        } else if(id == R.id.manual_mode && mySwitch.isChecked()){
            Toast.makeText(MainActivity.this, "You must disable automatic GPS first", Toast.LENGTH_SHORT).show();
        } else if(id == R.id.exit_app) {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Exit Confirmation")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Close the app when the user confirms
                        finishAffinity();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // Dismiss the dialog if the user cancels
                        dialog.dismiss();
                    })
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }


    // Automatic way of location //

    private void startLocationPublishing() {
        if (isAutomaticPublishing) return;
        isAutomaticPublishing = true;
        // Define the automatic publishing Runnable for GPS data
        automaticPublishRunnable = new Runnable() {



            @Override
            public void run() {
                publishCoordinates(latitude.toString(), longitude.toString(), deviceID);

                // Schedule the next automatic publish
                publishHandler.postDelayed(this, 1000);
            }
        };

        // Define the Runnable for CSV data publishing
        csvPublishRunnable = () -> {
            publishFromCsvFile(MainActivity.this, manual_mode_seconds);
            publishHandler.removeCallbacks(csvPublishRunnable);
            publishHandler.removeCallbacks(csvPublishRunnable);
        };

        // Start the publishing based on the switch state
        if (mySwitch.isChecked()) {
            // Stop CSV publishing if it was previously running
            publishHandler.removeCallbacks(csvPublishRunnable);
            // Start automatic GPS publishing
            publishHandler.post(automaticPublishRunnable);
        } else {
            // Stop automatic GPS publishing if it was previously running
            publishHandler.removeCallbacks(automaticPublishRunnable);
            // Start CSV data publishing
            publishHandler.post(csvPublishRunnable);
        }

    }


    public void publishFromCsvFile(Context context, int manual_mode_seconds) {
        BufferedReader reader;
        final int[] dataCounter = {0};

        try {
            // Open the file from internal storage
            FileInputStream fis;
            try {
                fis = context.openFileInput("android_coordinates.csv");
            } catch (FileNotFoundException e) {
                Log.e("Read CSV", "File not found", e);
                return;
            }
            reader = new BufferedReader(new InputStreamReader(fis));
            reader.readLine(); // Skip header

            // Schedule each publication at fixed intervals
            Handler handler = new Handler(Looper.getMainLooper());
            BufferedReader finalReader = reader;
            Runnable publishTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        String line = finalReader.readLine();
                        if (line != null && (manual_mode_seconds == 0 || dataCounter[0] < manual_mode_seconds)) {
                            String[] columns = line.split(",");
                            String x = columns[0].trim(); // X value
                            String y = columns[1].trim(); // Y value

                            // Publish coordinates to MQTT broker
                            publishCoordinates(y, x, deviceID);
                            dataCounter[0]++;

                            // Schedule the next run after 1 second
                            handler.postDelayed(this, 1000);
                        } else {
                            // Close reader when done
                            finalReader.close();
                        }
                    } catch (IOException e) {
                        Log.e("CSV Publish", "Error reading file", e);
                    }
                }
            };

            // Start the first execution
            handler.post(publishTask);

        } catch (IOException e) {
            Log.e("CSV Publish", "Error initializing file read", e);
        }
    }



    private static void publishCoordinates(String latitude, String longitude, String deviceID) {
        String messageContent = deviceID + " " + latitude + " " + longitude;
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        message.setQos(2); // Set quality of service with 2

        try {
            if (client != null && client.isConnected()) {
                client.publish("project/long_lat_deviceID", message);
                Log.d("MQTT Publish", "Published coordinates: " + messageContent);
            }
        } catch (MqttException e) {
            Log.e("MQTT Error", "Error publishing coordinates", e);
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

    // Result of permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) { // Indeed the request code is about location permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // If we received permissions
                getLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation() {
        // Checking whether the location permissions (ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION) are granted or not

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
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
        startLocationUpdates(); // Start updates when activity is resumed
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates(); // Stop updates when activity is paused
    }

    private void startLocationUpdates() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Create LocationRequest using the new Builder
        LocationRequest locationRequest = new LocationRequest.Builder(1000) // Set interval to 1000 ms (1 second)
                .setMinUpdateIntervalMillis(500) // Set fastest interval to 500 ms
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY) // Set high accuracy priority
                .build();

        // Request location updates
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



    // ALERTS //

    // Parse message and trigger alert
    private void handleRiskAlert(String riskLevel, double distance) {
        Log.d("MESSAGE RECEIVED", riskLevel);
        // Trigger an alert based on the risk level
        if ("Medium".equals(riskLevel)) {
            showAlert("Medium Risk Alert", "Medium Danger near your area..", Color.YELLOW, "Medium", distance);
        } else if ("High".equals(riskLevel)) {
            showAlert("High Risk Alert!", "High danger! Leave immediately!!! ", Color.RED, "High", distance);
        }
    }

    private void showAlert(String title, String message, int color, String riskLevel, double distance) {
        // Determine the sound URI and channel ID based on the risk level
        Uri soundUri;
        String channelId;
        int icon;

        if ("Medium".equals(riskLevel)) {
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert1);
            channelId = "ALERT_CHANNEL_MODERATE";
            icon = R.drawable.warning_yellow;
        } else { // High equals riskLevel, no other risk levels will be sent from server anyway
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert2);
            channelId = "ALERT_CHANNEL_HIGH";
            icon = R.drawable.warning_red;
        }


        // Create the notification notification
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message + "Distance from sensors: " + distance * 1000 + " m")
                .setColor(color)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Create the notification channel
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        It is used to check if the Android device is running on Android Oreo (API level 26)
//        or higher. Build.VERSION.SDK_INT provides the device's current Android version,
//        while Build.VERSION_CODES.O represents Android 8.0 (Oreo).
//        This check is essential when using newer features that are only available in
//        recent Android versions, helping avoid compatibility issues.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    riskLevel + " Risk Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for " + riskLevel + " risk alerts");
            channel.setLightColor(Color.RED);
            channel.enableLights(true);

            // Set the sound and attributes for the notification channel
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);
            channel.enableVibration(true);

            // Register the channel with the notification manager
            notificationManager.createNotificationChannel(channel);
        }

        // Issue the notification
        notificationManager.notify(riskLevel.hashCode(), notification.build()); // Use different ID for each alert using hashCode() to avoid overwriting
    }


}
