package com.example.wifimanager.Settings_Components;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class wifi_settings extends AppCompatActivity {

    // UI components
    private EditText etWifiName;
    private EditText etPassword;
    private TextView tvEncryption;
    private TextView tvSignalStrength;
    private SwitchMaterial switchWifi;
    private SwitchMaterial switchHideNetwork;
    private Button btnApply;
    private ImageView ivTogglePassword;
    private ConstraintLayout dialogEncryption;
    private ConstraintLayout dialogSignalStrength;

    // Original values to track changes
    private String originalWifiName;
    private String originalPassword;
    private String originalEncryption;
    private String originalSignalStrength;
    private boolean originalHideNetwork;

    // Current password visibility state
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        // Initialize UI components
        initializeViews();

        // Set up toolbar with back arrow
        setupToolbar();

        // Setup window insets
        setupWindowInsets();

        // Save original values
        saveOriginalValues();

        // Setup listeners
        setupListeners();
    }

    private void initializeViews() {
        etWifiName = findViewById(R.id.et_wifi_name);
        etPassword = findViewById(R.id.tv_password);
        tvEncryption = findViewById(R.id.tv_encryption);
        tvSignalStrength = findViewById(R.id.tv_signal_strength);
        switchWifi = findViewById(R.id.switch_wifi);
        switchHideNetwork = findViewById(R.id.switch_hide_network);
        btnApply = findViewById(R.id.btn_apply);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        dialogEncryption = findViewById(R.id.dialog_encryption);
        dialogSignalStrength = findViewById(R.id.dialog_signal_strength);

        // Initialize dialogs to GONE
        dialogEncryption.setVisibility(View.GONE);
        dialogSignalStrength.setVisibility(View.GONE);

        // Set the appropriate elevation and layout parameters
        ConstraintLayout mainLayout = findViewById(R.id.main);
        dialogEncryption.setElevation(20f); // Higher elevation than other components
        dialogSignalStrength.setElevation(20f);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void saveOriginalValues() {
        originalWifiName = etWifiName.getText().toString();
        originalPassword = etPassword.getText().toString();
        originalEncryption = tvEncryption.getText().toString();
        originalSignalStrength = tvSignalStrength.getText().toString();
        originalHideNetwork = switchHideNetwork.isChecked();
    }

    private void setupListeners() {
        // Handle password visibility toggle
        setupPasswordToggle();

        // Handle text changes in WiFi name and password
        setupTextChangeListeners();

        // Handle encryption dialog
        setupEncryptionDialog();

        // Handle signal strength dialog
        setupSignalStrengthDialog();

        // Handle hide network switch
        setupHideNetworkSwitch();

        // Handle WiFi toggle switch
        setupWifiToggleSwitch();

        // Handle apply button
        setupApplyButton();
    }

    private void setupPasswordToggle() {
        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(null);
                ivTogglePassword.setImageResource(R.drawable.eye_closed);
            } else {
                etPassword.setTransformationMethod(new PasswordTransformationMethod());
                ivTogglePassword.setImageResource(R.drawable.eye_open);
            }
            // Place cursor at the end of text
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    private void setupTextChangeListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                checkForChanges();
            }
        };

        etWifiName.addTextChangedListener(textWatcher);
        etPassword.addTextChangedListener(textWatcher);
    }

    private void setupEncryptionDialog() {
        // Show encryption dialog
        findViewById(R.id.layout_encryption).setOnClickListener(v -> {
            // Ensure the dialog is visible and above other content
            dialogEncryption.setVisibility(View.VISIBLE);
            dialogEncryption.bringToFront();

            // Force layout update
            dialogEncryption.invalidate();
            dialogEncryption.requestLayout();

            // Update the radio buttons based on current selection
            updateEncryptionRadioButtons();
        });

        // Handle encryption option selection
        RadioButton radioWpa = findViewById(R.id.radio_wpa);
        RadioButton radioWpaWpa2 = findViewById(R.id.radio_wpa_wpa2);
        RadioButton radioWpa2 = findViewById(R.id.radio_wpa2);

        View.OnClickListener encryptionOptionClickListener = v -> {
            radioWpa.setChecked(v.getId() == R.id.option_wpa || v.getId() == R.id.radio_wpa);
            radioWpaWpa2.setChecked(v.getId() == R.id.option_wpa_wpa2 || v.getId() == R.id.radio_wpa_wpa2);
            radioWpa2.setChecked(v.getId() == R.id.option_wpa2 || v.getId() == R.id.radio_wpa2);
        };

        findViewById(R.id.option_wpa).setOnClickListener(encryptionOptionClickListener);
        findViewById(R.id.option_wpa_wpa2).setOnClickListener(encryptionOptionClickListener);
        findViewById(R.id.option_wpa2).setOnClickListener(encryptionOptionClickListener);
        radioWpa.setOnClickListener(encryptionOptionClickListener);
        radioWpaWpa2.setOnClickListener(encryptionOptionClickListener);
        radioWpa2.setOnClickListener(encryptionOptionClickListener);

        // Handle encryption dialog buttons
        findViewById(R.id.btn_cancel_encryption).setOnClickListener(v -> {
            dialogEncryption.setVisibility(View.GONE);
        });

        findViewById(R.id.btn_ok_encryption).setOnClickListener(v -> {
            if (radioWpa.isChecked()) {
                tvEncryption.setText("WPA");
            } else if (radioWpaWpa2.isChecked()) {
                tvEncryption.setText("WPA/WPA2");
            } else if (radioWpa2.isChecked()) {
                tvEncryption.setText("WPA2");
            }
            dialogEncryption.setVisibility(View.GONE);
            checkForChanges();
        });
    }

    private void updateEncryptionRadioButtons() {
        RadioButton radioWpa = findViewById(R.id.radio_wpa);
        RadioButton radioWpaWpa2 = findViewById(R.id.radio_wpa_wpa2);
        RadioButton radioWpa2 = findViewById(R.id.radio_wpa2);

        String currentEncryption = tvEncryption.getText().toString();
        radioWpa.setChecked(currentEncryption.equals("WPA"));
        radioWpaWpa2.setChecked(currentEncryption.equals("WPA/WPA2"));
        radioWpa2.setChecked(currentEncryption.equals("WPA2"));
    }

    private void setupSignalStrengthDialog() {
        // Show signal strength dialog
        findViewById(R.id.layout_signal_strength).setOnClickListener(v -> {
            // Ensure the dialog is visible and above other content
            dialogSignalStrength.setVisibility(View.VISIBLE);
            dialogSignalStrength.bringToFront();

            // Force layout update
            dialogSignalStrength.invalidate();
            dialogSignalStrength.requestLayout();

            // Update the radio buttons based on current selection
            updateSignalStrengthRadioButtons();
        });

        // Handle signal strength option selection
        RadioButton radioHigh = findViewById(R.id.radio_high);
        RadioButton radioMedium = findViewById(R.id.radio_medium);
        RadioButton radioLow = findViewById(R.id.radio_low);

        View.OnClickListener signalStrengthOptionClickListener = v -> {
            radioHigh.setChecked(v.getId() == R.id.option_high || v.getId() == R.id.radio_high);
            radioMedium.setChecked(v.getId() == R.id.option_medium || v.getId() == R.id.radio_medium);
            radioLow.setChecked(v.getId() == R.id.option_low || v.getId() == R.id.radio_low);
        };

        findViewById(R.id.option_high).setOnClickListener(signalStrengthOptionClickListener);
        findViewById(R.id.option_medium).setOnClickListener(signalStrengthOptionClickListener);
        findViewById(R.id.option_low).setOnClickListener(signalStrengthOptionClickListener);
        radioHigh.setOnClickListener(signalStrengthOptionClickListener);
        radioMedium.setOnClickListener(signalStrengthOptionClickListener);
        radioLow.setOnClickListener(signalStrengthOptionClickListener);

        // Handle signal strength dialog buttons
        findViewById(R.id.btn_cancel_signal).setOnClickListener(v -> {
            dialogSignalStrength.setVisibility(View.GONE);
        });

        findViewById(R.id.btn_ok_signal).setOnClickListener(v -> {
            if (radioHigh.isChecked()) {
                tvSignalStrength.setText("High");
            } else if (radioMedium.isChecked()) {
                tvSignalStrength.setText("Medium");
            } else if (radioLow.isChecked()) {
                tvSignalStrength.setText("Low");
            }
            dialogSignalStrength.setVisibility(View.GONE);
            checkForChanges();
        });
    }

    private void updateSignalStrengthRadioButtons() {
        RadioButton radioHigh = findViewById(R.id.radio_high);
        RadioButton radioMedium = findViewById(R.id.radio_medium);
        RadioButton radioLow = findViewById(R.id.radio_low);

        String currentStrength = tvSignalStrength.getText().toString();
        radioHigh.setChecked(currentStrength.equals("High"));
        radioMedium.setChecked(currentStrength.equals("Medium"));
        radioLow.setChecked(currentStrength.equals("Low"));
    }

    private void setupHideNetworkSwitch() {
        switchHideNetwork.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkForChanges();
        });
    }

    private void setupWifiToggleSwitch() {
        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Enable or disable all settings based on Wi-Fi state
            setSettingsEnabled(isChecked);
        });
    }

    private void setupApplyButton() {
        btnApply.setOnClickListener(v -> {
            // Here you would implement the actual saving of settings
            // For demonstration purposes, we'll just update original values and show a toast
            saveOriginalValues();
            checkForChanges();
            Toast.makeText(this, "Wi-Fi settings saved successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void setSettingsEnabled(boolean enabled) {
        // Enable/disable all settings components based on WiFi toggle
        findViewById(R.id.settings_card).setAlpha(enabled ? 1.0f : 0.5f);
        etWifiName.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        findViewById(R.id.layout_encryption).setEnabled(enabled);
        findViewById(R.id.layout_signal_strength).setEnabled(enabled);
        switchHideNetwork.setEnabled(enabled);

        // Check if we need to enable apply button
        if (enabled) {
            checkForChanges();
        } else {
            // Disable apply button if WiFi is off
            setApplyButtonEnabled(false);
        }
    }

    private void checkForChanges() {
        boolean hasChanges = false;

        // Only check for changes if WiFi is enabled
        if (switchWifi.isChecked()) {
            hasChanges = !etWifiName.getText().toString().equals(originalWifiName) ||
                    !etPassword.getText().toString().equals(originalPassword) ||
                    !tvEncryption.getText().toString().equals(originalEncryption) ||
                    !tvSignalStrength.getText().toString().equals(originalSignalStrength) ||
                    switchHideNetwork.isChecked() != originalHideNetwork;
        }

        setApplyButtonEnabled(hasChanges);
    }

    private void setApplyButtonEnabled(boolean enabled) {
        btnApply.setEnabled(enabled);
        btnApply.setBackgroundTintList(getColorStateList(enabled ? R.color.topColor : R.color.light_gray));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Check if a dialog is showing and close it instead
        if (dialogEncryption.getVisibility() == View.VISIBLE) {
            dialogEncryption.setVisibility(View.GONE);
            return;
        }

        if (dialogSignalStrength.getVisibility() == View.VISIBLE) {
            dialogSignalStrength.setVisibility(View.GONE);
            return;
        }

        // Otherwise, proceed with normal back behavior
        super.onBackPressed();
    }
}