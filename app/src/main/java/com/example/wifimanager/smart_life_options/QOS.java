package com.example.wifimanager.smart_life_options;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.wifimanager.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

public class QOS extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "QOSSettings";
    private static final String KEY_QOS_ENABLED = "qos_enabled";
    private static final String KEY_PRIORITY_MODE = "priority_mode";
    private static final String KEY_UPLOAD_SPEED = "upload_speed";
    private static final String KEY_DOWNLOAD_SPEED = "download_speed";
    private LinearLayout priorityModeLayout;
    private RadioGroup priorityRadioGroup;
    private Switch qosSwitch;
    private TextView uploadSpeedValue;
    private TextView downloadSpeedValue;
    private LottieAnimationView animationView;
    private String stok;
    private RequestQueue requestQueue;

    private static final String BASE_URL = "http://192.168.31.1/cgi-bin/luci/;stok=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qos);

        // Initialize RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Get stok from intent
        stok = getIntent().getStringExtra("STOK");

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Setup toolbar with back arrow
        setupToolbar();

        // Load saved settings and setup animation initial state
        loadSavedSettings();

        // Fetch current bandwidth values
        fetchCurrentBandwidth();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        priorityModeLayout = findViewById(R.id.priorityModeLayout);
        priorityRadioGroup = findViewById(R.id.priorityRadioGroup);
        qosSwitch = findViewById(R.id.qosSwitch);
        uploadSpeedValue = findViewById(R.id.uploadSpeedValue);
        downloadSpeedValue = findViewById(R.id.downloadSpeedValue);
        animationView = findViewById(R.id.circularDial);

        // Initialize animation view
        animationView.setAnimation(R.raw.qos_animation);
        animationView.setSpeed(1.5f);
        animationView.setRepeatCount(0); // Don't repeat the animation

        // Speed limit settings click listener
        TextView speedLimitSettings = findViewById(R.id.speedLimitSettings);
        speedLimitSettings.setText(Html.fromHtml("<u>Speed limit settings</u>"));
        speedLimitSettings.setOnClickListener(v -> showSpeedSettingsDialog());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void loadSavedSettings() {
        // Load QoS state
        boolean qosEnabled = sharedPreferences.getBoolean(KEY_QOS_ENABLED, false);
        qosSwitch.setChecked(qosEnabled);
        priorityModeLayout.setVisibility(qosEnabled ? View.VISIBLE : View.GONE);

        // Set initial animation state based on QoS status
        if (qosEnabled) {
            animationView.playAnimation(); // Play once if QoS is enabled
        } else {
            animationView.setProgress(0); // Reset to start position if QoS is disabled
            animationView.pauseAnimation();
        }

        // Load priority mode
        String savedMode = sharedPreferences.getString(KEY_PRIORITY_MODE, "auto");
        switch (savedMode) {
            case "auto":
                priorityRadioGroup.check(R.id.radioAutoMode);
                break;
            case "game":
                priorityRadioGroup.check(R.id.radioGameFirst);
                break;
            case "webpage":
                priorityRadioGroup.check(R.id.radioWebpageFirst);
                break;
            case "video":
                priorityRadioGroup.check(R.id.radioVideoFirst);
                break;
        }

        // Load speed values
        String uploadSpeed = sharedPreferences.getString(KEY_UPLOAD_SPEED, "Unknown");
        String downloadSpeed = sharedPreferences.getString(KEY_DOWNLOAD_SPEED, "Unknown");
        uploadSpeedValue.setText(uploadSpeed);
        downloadSpeedValue.setText(downloadSpeed);
    }

    private void setupClickListeners() {
        qosSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (uploadSpeedValue.getText().toString().equals("Unknown") || 
                    downloadSpeedValue.getText().toString().equals("Unknown")) {
                    Toast.makeText(QOS.this, "Please set bandwidth limits first", Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false);
                    showSpeedSettingsDialog();
                    return;
                }
            }
            setQoSState(isChecked);
        });

        priorityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radioAutoMode) mode = 3;
            else if (checkedId == R.id.radioGameFirst) mode = 4;
            else if (checkedId == R.id.radioWebpageFirst) mode = 5;
            else if (checkedId == R.id.radioVideoFirst) mode = 6;
            else return;

            setQoSMode(mode);
        });
    }

    private void setQoSState(boolean enabled) {
        String url = BASE_URL + stok + "/api/misystem/qos_switch?on=" + (enabled ? "1" : "0");

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getInt("code") == 0) {
                            saveSettings();
                            if (enabled) {
                                priorityModeLayout.setVisibility(View.VISIBLE);
                                animationView.setProgress(0);
                                animationView.setRepeatCount(0);
                                animationView.playAnimation();
                            } else {
                                priorityModeLayout.setVisibility(View.GONE);
                                animationView.setProgress(0);
                                animationView.pauseAnimation();
                            }
                            Toast.makeText(this, "QoS " + (enabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                        } else {
                            qosSwitch.setChecked(!enabled);
                            Toast.makeText(this, "Failed to " + (enabled ? "enable" : "disable") + " QoS", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        qosSwitch.setChecked(!enabled);
                    }
                },
                error -> {
                    error.printStackTrace();
                    qosSwitch.setChecked(!enabled);
                    Toast.makeText(this, "Error toggling QoS", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

    private void showSpeedSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.qos_speed_settings_dialog, null);
        EditText uploadInput = dialogView.findViewById(R.id.uploadSpeedInput);
        EditText downloadInput = dialogView.findViewById(R.id.downloadSpeedInput);

        // Clear any existing hints
        uploadInput.setHint("");
        downloadInput.setHint("");

        // Get current bandwidth values
        String url = BASE_URL + stok + "/api/misystem/qos_info";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getInt("code") == 0) {
                            JSONObject band = response.getJSONObject("band");
                            // Get raw values without conversion
                            int uploadValue = band.getInt("upload");
                            int downloadValue = band.getInt("download");
                            
                            // Show values directly in the input fields
                            uploadInput.setText(String.valueOf(uploadValue));
                            downloadInput.setText(String.valueOf(downloadValue));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> error.printStackTrace()
        );
        requestQueue.add(request);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setTitle("Set Speed Limits (Mbit/s)")
                .setPositiveButton("Save", null) // Set to null initially
                .setNegativeButton("Cancel", null)
                .create();

        // Override the click listener to prevent dialog from closing if validation fails
        dialog.setOnShowListener(dialogInterface -> {
            Button button = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String uploadSpeed = uploadInput.getText().toString().trim();
                String downloadSpeed = downloadInput.getText().toString().trim();

                if (uploadSpeed.isEmpty() || downloadSpeed.isEmpty()) {
                    Toast.makeText(this, "Please enter both upload and download speeds", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int upload = Integer.parseInt(uploadSpeed);
                    int download = Integer.parseInt(downloadSpeed);

                    // Validate upload speed (0 < upload <= 10000)
                    if (upload > 10000) {
                        Toast.makeText(this, "Enter up to 10000 Mbit/s for upload speed", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (upload <= 0) {
                        Toast.makeText(this, "Use 0.01 or more for upload speed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Validate download speed (0 < download <= 2048)
                    if (download > 2048) {
                        Toast.makeText(this, "Enter up to 2048 Mbit/s for download speed", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (download <= 0) {
                        Toast.makeText(this, "Use 0.01 or more for download speed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Convert to Mb/s by dividing by 8 before saving
                    int uploadMbs = upload / 8;
                    int downloadMbs = download / 8;

                    // Update UI with Mb/s values
                    uploadSpeedValue.setText(uploadMbs + " Mbit/s");
                    downloadSpeedValue.setText(downloadMbs + " Mbit/s");
                    sharedPreferences.edit()
                            .putString(KEY_UPLOAD_SPEED, uploadMbs + " Mbit/s")
                            .putString(KEY_DOWNLOAD_SPEED, downloadMbs + " Mbit/s")
                            .apply();
                    
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void setQoSMode(int mode) {
        String url = BASE_URL + stok + "/api/misystem/qos_mode?mode=" + mode;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getInt("code") == 0) {
                            String modeStr;
                            switch (mode) {
                                case 3:
                                    modeStr = "auto";
                                    break;
                                case 4:
                                    modeStr = "game";
                                    break;
                                case 5:
                                    modeStr = "webpage";
                                    break;
                                case 6:
                                    modeStr = "video";
                                    break;
                                default:
                                    modeStr = "auto";
                            }
                            sharedPreferences.edit().putString(KEY_PRIORITY_MODE, modeStr).apply();
                            Toast.makeText(this, "QoS mode updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to change QoS mode", Toast.LENGTH_SHORT).show();
                            loadSavedSettings();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        loadSavedSettings();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error setting QoS mode", Toast.LENGTH_SHORT).show();
                    loadSavedSettings();
                }
        );

        requestQueue.add(request);
    }

    private void saveSettings() {
        sharedPreferences.edit()
                .putBoolean(KEY_QOS_ENABLED, qosSwitch.isChecked())
                .apply();
    }

    private void fetchCurrentBandwidth() {
        String url = BASE_URL + stok + "/api/misystem/qos_info";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getInt("code") == 0) {
                            JSONObject band = response.getJSONObject("band");
                            // Convert to Mbps by dividing by 8
                            int uploadMbps = band.getInt("upload") / 8;
                            int downloadMbps = band.getInt("download") / 8;
                            
                            // Update UI with the values
                            uploadSpeedValue.setText(uploadMbps + " Mbit/s");
                            downloadSpeedValue.setText(downloadMbps + " Mbit/s");
                            
                            // Save the values
                            sharedPreferences.edit()
                                .putString(KEY_UPLOAD_SPEED, uploadMbps + " Mbit/s")
                                .putString(KEY_DOWNLOAD_SPEED, downloadMbps + " Mbit/s")
                                .apply();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> error.printStackTrace()
        );
        requestQueue.add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}