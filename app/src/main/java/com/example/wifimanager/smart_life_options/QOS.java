package com.example.wifimanager.smart_life_options;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
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
import com.example.wifimanager.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qos);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Setup toolbar with back arrow
        setupToolbar();

        // Load saved settings and setup animation initial state
        loadSavedSettings();

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
                // Check if speeds are set before enabling QoS
                if (uploadSpeedValue.getText().toString().equals("Unknown") || 
                    downloadSpeedValue.getText().toString().equals("Unknown")) {
                    Toast.makeText(QOS.this, "Please set bandwidth limits first", Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false);
                    showSpeedSettingsDialog();
                    return;
                }
                // Play animation once when enabling QoS
                animationView.setProgress(0);
                animationView.setRepeatCount(0);
                animationView.playAnimation();
                priorityModeLayout.setVisibility(View.VISIBLE);
                Toast.makeText(QOS.this, "QoS enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Reset animation to initial state when disabling QoS
                animationView.setProgress(0);
                animationView.pauseAnimation();
                priorityModeLayout.setVisibility(View.GONE);
                Toast.makeText(QOS.this, "QoS disabled", Toast.LENGTH_SHORT).show();
            }
            saveSettings();
        });

        priorityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String mode;
            if (checkedId == R.id.radioAutoMode) mode = "auto";
            else if (checkedId == R.id.radioGameFirst) mode = "game";
            else if (checkedId == R.id.radioWebpageFirst) mode = "webpage";
            else if (checkedId == R.id.radioVideoFirst) mode = "video";
            else return;

            sharedPreferences.edit().putString(KEY_PRIORITY_MODE, mode).apply();
        });
    }

    private void showSpeedSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.qos_speed_settings_dialog, null);
        EditText uploadInput = dialogView.findViewById(R.id.uploadSpeedInput);
        EditText downloadInput = dialogView.findViewById(R.id.downloadSpeedInput);

        // Pre-fill existing values if they exist
        if (!uploadSpeedValue.getText().toString().equals("Unknown")) {
            uploadInput.setText(uploadSpeedValue.getText().toString().replace(" Mbps", ""));
        }
        if (!downloadSpeedValue.getText().toString().equals("Unknown")) {
            downloadInput.setText(downloadSpeedValue.getText().toString().replace(" Mbps", ""));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    String uploadSpeed = uploadInput.getText().toString().trim();
                    String downloadSpeed = downloadInput.getText().toString().trim();

                    if (uploadSpeed.isEmpty() || downloadSpeed.isEmpty()) {
                        Toast.makeText(this, "Please enter both upload and download speeds", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        float upload = Float.parseFloat(uploadSpeed);
                        float download = Float.parseFloat(downloadSpeed);
                        
                        if (upload <= 0 || download <= 0) {
                            Toast.makeText(this, "Speeds must be greater than 0", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        updateSpeedInfo(uploadSpeed, downloadSpeed);
                        saveSettings();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void updateSpeedInfo(String uploadSpeed, String downloadSpeed) {
        uploadSpeedValue.setText(uploadSpeed + " Mbps");
        downloadSpeedValue.setText(downloadSpeed + " Mbps");
        sharedPreferences.edit()
                .putString(KEY_UPLOAD_SPEED, uploadSpeed + " Mbps")
                .putString(KEY_DOWNLOAD_SPEED, downloadSpeed + " Mbps")
                .apply();
    }

    private void saveSettings() {
        sharedPreferences.edit()
                .putBoolean(KEY_QOS_ENABLED, qosSwitch.isChecked())
                .apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}