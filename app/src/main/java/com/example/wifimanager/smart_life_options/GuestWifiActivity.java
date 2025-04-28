package com.example.wifimanager.smart_life_options;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class GuestWifiActivity extends AppCompatActivity {

    // UI Components
    private SwitchMaterial switchGuestWifi;
    private EditText etGuestWifiName;
    private EditText etPassword;
    private TextView tvEncryption;
    private ImageView ivTogglePassword;
    private MaterialButton btnApply;
    private LinearLayout layoutPassword;
    private LinearLayout layoutEncryption;
    private ConstraintLayout dialogEncryption;
    private View settingsCard;
    private ConstraintLayout mainLayout;

    // Radio buttons for encryption options
    private RadioButton radioMixed;
    private RadioButton radioStrong;
    private RadioButton radioNone;

    // Encryption types
    private static final String ENCRYPTION_MIXED = "Mixed (WPA/WPA2-personal)";
    private static final String ENCRYPTION_STRONG = "Strong (WPA2-personal)";
    private static final String ENCRYPTION_NONE = "Not encrypted (everyone can access)";

    // Flag to track if password is visible
    private boolean isPasswordVisible = false;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "GuestWifiPrefs";
    private static final String KEY_WIFI_ENABLED = "wifi_enabled";
    private static final String KEY_NETWORK_NAME = "network_name";
    private static final String KEY_ENCRYPTION_TYPE = "encryption_type";
    private static final String KEY_PASSWORD = "password";

    // Track if any changes were made
    private boolean hasChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_wifi);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize UI components
        initViews();

        // Set up toolbar with back arrow
        setupToolbar();

        // Set up window insets
        setupWindowInsets();

        // Load saved preferences
        loadSavedPreferences();

        // Set up listeners
        setupListeners();

        // Initialize UI state
        updateUIState();
        updateEncryptionTypeUI();
        updateApplyButtonState();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main);
        switchGuestWifi = findViewById(R.id.switch_guest_wifi);
        etGuestWifiName = findViewById(R.id.et_guest_wifi_name);
        etPassword = findViewById(R.id.et_password);
        tvEncryption = findViewById(R.id.tv_encryption);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        btnApply = findViewById(R.id.btn_apply);
        layoutPassword = findViewById(R.id.layout_password);
        layoutEncryption = findViewById(R.id.layout_encryption);
        dialogEncryption = findViewById(R.id.dialog_encryption);
        settingsCard = findViewById(R.id.settings_card);

        // Dialog components
        radioMixed = findViewById(R.id.radio_mixed);
        radioStrong = findViewById(R.id.radio_strong);
        radioNone = findViewById(R.id.radio_none);

        // Set up radio group behavior
        radioMixed.setOnClickListener(v -> {
            radioMixed.setChecked(true);
            radioStrong.setChecked(false);
            radioNone.setChecked(false);
        });

        radioStrong.setOnClickListener(v -> {
            radioMixed.setChecked(false);
            radioStrong.setChecked(true);
            radioNone.setChecked(false);
        });

        radioNone.setOnClickListener(v -> {
            radioMixed.setChecked(false);
            radioStrong.setChecked(false);
            radioNone.setChecked(true);
        });
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

    private void loadSavedPreferences() {
        boolean isEnabled = sharedPreferences.getBoolean(KEY_WIFI_ENABLED, false);
        String networkName = sharedPreferences.getString(KEY_NETWORK_NAME, "MiShareWiFi_2251");
        String encryptionType = sharedPreferences.getString(KEY_ENCRYPTION_TYPE, ENCRYPTION_MIXED);
        String password = sharedPreferences.getString(KEY_PASSWORD, "");

        switchGuestWifi.setChecked(isEnabled);
        etGuestWifiName.setText(networkName);
        tvEncryption.setText(encryptionType);
        etPassword.setText(password);

        updateEncryptionTypeUI();
    }

    private void setupListeners() {
        // Main layout touch listener
        mainLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    clearFocusAndHideKeyboard(currentFocus);
                }
            }
            return false;
        });

        // Guest WiFi switch listener
        switchGuestWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasChanges = true;
            updateUIState();
            updateApplyButtonState();
        });

        // Text change listeners
        etGuestWifiName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanges = true;
                updateApplyButtonState();
            }
        });

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanges = true;
                updateApplyButtonState();
            }
        });

        // Password visibility toggle
        ivTogglePassword.setOnClickListener(v -> {
            togglePasswordVisibility();
            hasChanges = true;
            updateApplyButtonState();
        });

        // Encryption selection
        layoutEncryption.setOnClickListener(v -> {
            if (switchGuestWifi.isChecked()) {
                showEncryptionDialog();
            }
        });

        // Dialog buttons
        findViewById(R.id.btn_cancel_encryption).setOnClickListener(v -> hideEncryptionDialog());
        findViewById(R.id.btn_ok_encryption).setOnClickListener(v -> {
            updateEncryptionType();
            hideEncryptionDialog();
            hasChanges = true;
            updateApplyButtonState();
        });

        // Apply button
        btnApply.setOnClickListener(v -> saveSettings());
    }

    private void updateUIState() {
        boolean isEnabled = switchGuestWifi.isChecked();
        settingsCard.setAlpha(isEnabled ? 1.0f : 0.5f);
        etGuestWifiName.setEnabled(isEnabled);
        layoutEncryption.setEnabled(isEnabled);
        ivTogglePassword.setEnabled(isEnabled);
        ivTogglePassword.setAlpha(isEnabled ? 1.0f : 0.5f);

        // Update password field state based on encryption
        updateEncryptionTypeUI();
    }

    private void updateEncryptionTypeUI() {
        String currentEncryption = tvEncryption.getText().toString();
        boolean isNone = currentEncryption.equals(ENCRYPTION_NONE);
        layoutPassword.setVisibility(isNone ? View.GONE : View.VISIBLE);
        etPassword.setEnabled(!isNone && switchGuestWifi.isChecked());
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(null);
            ivTogglePassword.setImageResource(R.drawable.eye_open);
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivTogglePassword.setImageResource(R.drawable.eye_closed);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void showEncryptionDialog() {
        dialogEncryption.setVisibility(View.VISIBLE);
        String currentEncryption = tvEncryption.getText().toString();
        radioMixed.setChecked(currentEncryption.equals(ENCRYPTION_MIXED));
        radioStrong.setChecked(currentEncryption.equals(ENCRYPTION_STRONG));
        radioNone.setChecked(currentEncryption.equals(ENCRYPTION_NONE));
    }

    private void hideEncryptionDialog() {
        dialogEncryption.setVisibility(View.GONE);
    }

    private void updateEncryptionType() {
        String selectedEncryption;
        if (radioMixed.isChecked()) {
            selectedEncryption = ENCRYPTION_MIXED;
        } else if (radioStrong.isChecked()) {
            selectedEncryption = ENCRYPTION_STRONG;
        } else {
            selectedEncryption = ENCRYPTION_NONE;
        }
        tvEncryption.setText(selectedEncryption);
        updateEncryptionTypeUI();
    }

    private void updateApplyButtonState() {
        btnApply.setEnabled(hasChanges);
        btnApply.setBackgroundTintList(getColorStateList(
                hasChanges ? R.color.topColor : R.color.light_gray));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_WIFI_ENABLED, switchGuestWifi.isChecked());
        editor.putString(KEY_NETWORK_NAME, etGuestWifiName.getText().toString().trim());
        editor.putString(KEY_ENCRYPTION_TYPE, tvEncryption.getText().toString());
        editor.putString(KEY_PASSWORD, etPassword.getText().toString());
        editor.apply();

        hasChanges = false;
        updateApplyButtonState();

        Toast.makeText(this, "Guest Wi-Fi settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void clearFocusAndHideKeyboard(View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                float x = event.getRawX();
                float y = event.getRawY();
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                if (x < location[0] || x > location[0] + v.getWidth() ||
                        y < location[1] || y > location[1] + v.getHeight()) {
                    clearFocusAndHideKeyboard(v);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public abstract void onTextChanged(CharSequence s, int start, int before, int count);
        @Override public void afterTextChanged(Editable s) {}
    }
}