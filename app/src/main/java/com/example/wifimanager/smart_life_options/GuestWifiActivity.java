package com.example.wifimanager.smart_life_options;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

    // Encryption types exactly matching API values
    private static final String ENCRYPTION_MIXED = "mixed-psk";    // Mixed (WPA/WPA2-personal)
    private static final String ENCRYPTION_STRONG = "psk2";        // Strong (WPA2-personal)
    private static final String ENCRYPTION_NONE = "none";          // No encryption

    // Human readable encryption descriptions
    private static final String ENCRYPTION_MIXED_DESC = "Mixed (WPA/WPA2-personal)";
    private static final String ENCRYPTION_STRONG_DESC = "Strong (WPA2-personal)";
    private static final String ENCRYPTION_NONE_DESC = "Not encrypted";

    // API related fields
    private String stok;
    private static final String BASE_URL = "http://192.168.31.1/cgi-bin/luci/;stok=";
    private RequestQueue requestQueue;

    // Flag to track if password is visible
    private boolean isPasswordVisible = false;

    // Database Helper
    private DatabaseHelper dbHelper;
    private static final String TABLE_NAME = DatabaseHelper.TABLE_GUEST_WIFI;
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

        // Get STOK from intent
        stok = getIntent().getStringExtra("STOK");
        if (stok == null) {
            Toast.makeText(this, "Authentication token missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize RequestQueue for API calls
        requestQueue = Volley.newRequestQueue(this);

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper.getInstance(this);

        // Initialize UI components first
        initViews();

        // Set up toolbar with back arrow
        setupToolbar();

        // Set up window insets
        setupWindowInsets();

        // Load current settings from router
        fetchGuestWifiSettings();

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

        // Initialize encryption tag with default value
        tvEncryption.setTag(ENCRYPTION_MIXED);
        tvEncryption.setText(ENCRYPTION_MIXED_DESC);

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

    private void fetchGuestWifiSettings() {
        String url = BASE_URL + stok + "/api/xqnetwork/wifi_detail_all";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("GuestWiFi", "Received settings response: " + response.toString());
                        if (response.getInt("code") == 0) {
                            JSONArray infoArray = response.getJSONArray("info");
                            for (int i = 0; i < infoArray.length(); i++) {
                                JSONObject wifiInfo = infoArray.getJSONObject(i);
                                if (wifiInfo.getString("ifname").equals("wl3")) {
                                    boolean isEnabled = wifiInfo.optInt("status", 0) == 1;
                                    String ssid = wifiInfo.getString("ssid").trim();
                                    String encryption = wifiInfo.optString("encryption", ENCRYPTION_MIXED);
                                    String password = wifiInfo.optString("password", "");

                                    // Map API value to display text
                                    String displayText;
                                    switch (encryption) {
                                        case ENCRYPTION_MIXED:
                                            displayText = ENCRYPTION_MIXED_DESC;
                                            break;
                                        case ENCRYPTION_STRONG:
                                            displayText = ENCRYPTION_STRONG_DESC;
                                            break;
                                        case ENCRYPTION_NONE:
                                            displayText = ENCRYPTION_NONE_DESC;
                                            break;
                                        default:
                                            displayText = encryption;
                                    }

                                    switchGuestWifi.setChecked(isEnabled);
                                    etGuestWifiName.setText(ssid);
                                    tvEncryption.setTag(encryption);
                                    tvEncryption.setText(displayText);
                                    etPassword.setText(password);

                                    dbHelper.putBoolean(TABLE_NAME, KEY_WIFI_ENABLED, isEnabled);
                                    dbHelper.putString(TABLE_NAME, KEY_NETWORK_NAME, ssid);
                                    dbHelper.putString(TABLE_NAME, KEY_ENCRYPTION_TYPE, encryption);
                                    dbHelper.putString(TABLE_NAME, KEY_PASSWORD, password);

                                    hasChanges = false;
                                    updateApplyButtonState();
                                    updateEncryptionTypeUI();
                                    break;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("GuestWiFi", "Error parsing WiFi settings: " + e.getMessage());
                        Toast.makeText(this, "Error parsing WiFi settings", Toast.LENGTH_SHORT).show();
                        loadSavedSettings();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.e("GuestWiFi", "Error fetching WiFi settings: " + error.getMessage());
                    Toast.makeText(this, "Error fetching WiFi settings", Toast.LENGTH_SHORT).show();
                    loadSavedSettings();
                }
        );
        requestQueue.add(request);
    }

    private void loadSavedSettings() {
        boolean isEnabled = dbHelper.getBoolean(TABLE_NAME, KEY_WIFI_ENABLED, false);
        String networkName = dbHelper.getString(TABLE_NAME, KEY_NETWORK_NAME, "MiShareWiFi_2251");
        String encryptionType = dbHelper.getString(TABLE_NAME, KEY_ENCRYPTION_TYPE, ENCRYPTION_MIXED);
        String password = dbHelper.getString(TABLE_NAME, KEY_PASSWORD, "");

        switchGuestWifi.setChecked(isEnabled);
        etGuestWifiName.setText(networkName);

        // Map stored encryption to display text
        String displayText;
        switch (encryptionType) {
            case ENCRYPTION_MIXED:
                displayText = ENCRYPTION_MIXED_DESC;
                break;
            case ENCRYPTION_STRONG:
                displayText = ENCRYPTION_STRONG_DESC;
                break;
            case ENCRYPTION_NONE:
                displayText = ENCRYPTION_NONE_DESC;
                break;
            default:
                displayText = encryptionType;
        }

        tvEncryption.setTag(encryptionType);
        tvEncryption.setText(displayText);
        etPassword.setText(password);

        updateEncryptionTypeUI();
    }

    private void setupListeners() {
        mainLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    clearFocusAndHideKeyboard(currentFocus);
                }
            }
            return false;
        });

        switchGuestWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasChanges = true;
            updateUIState();
            updateApplyButtonState();
        });

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

        ivTogglePassword.setOnClickListener(v -> {
            togglePasswordVisibility();
            hasChanges = true;
            updateApplyButtonState();
        });

        layoutEncryption.setOnClickListener(v -> {
            if (switchGuestWifi.isChecked()) {
                showEncryptionDialog();
            }
        });

        findViewById(R.id.btn_cancel_encryption).setOnClickListener(v -> hideEncryptionDialog());
        findViewById(R.id.btn_ok_encryption).setOnClickListener(v -> {
            updateEncryptionType();
            hideEncryptionDialog();
            hasChanges = true;
            updateApplyButtonState();
        });

        btnApply.setOnClickListener(v -> saveSettings());
    }

    private void updateUIState() {
        boolean isEnabled = switchGuestWifi.isChecked();
        settingsCard.setAlpha(isEnabled ? 1.0f : 0.5f);
        etGuestWifiName.setEnabled(isEnabled);
        layoutEncryption.setEnabled(isEnabled);
        ivTogglePassword.setEnabled(isEnabled);
        ivTogglePassword.setAlpha(isEnabled ? 1.0f : 0.5f);
        updateEncryptionTypeUI();
    }

    private void updateEncryptionTypeUI() {
        Object tag = tvEncryption.getTag();
        String currentEncryption = tag != null ? tag.toString() : ENCRYPTION_MIXED;
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
        Object tag = tvEncryption.getTag();
        String currentEncryption = tag != null ? tag.toString() : ENCRYPTION_MIXED;
        radioMixed.setChecked(currentEncryption.equals(ENCRYPTION_MIXED));
        radioStrong.setChecked(currentEncryption.equals(ENCRYPTION_STRONG));
        radioNone.setChecked(currentEncryption.equals(ENCRYPTION_NONE));
    }

    private void hideEncryptionDialog() {
        dialogEncryption.setVisibility(View.GONE);
    }

    private void updateEncryptionType() {
        String selectedEncryption;
        String displayText;
        if (radioMixed.isChecked()) {
            selectedEncryption = ENCRYPTION_MIXED;
            displayText = ENCRYPTION_MIXED_DESC;
        } else if (radioStrong.isChecked()) {
            selectedEncryption = ENCRYPTION_STRONG;
            displayText = ENCRYPTION_STRONG_DESC;
        } else {
            selectedEncryption = ENCRYPTION_NONE;
            displayText = ENCRYPTION_NONE_DESC;
        }
        tvEncryption.setTag(selectedEncryption);
        tvEncryption.setText(displayText);
        updateEncryptionTypeUI();
    }

    private void updateApplyButtonState() {
        btnApply.setEnabled(hasChanges);
        btnApply.setBackgroundTintList(getColorStateList(
                hasChanges ? R.color.topColor : R.color.light_gray));
    }

    private void saveSettings() {
        if (!validateSettings()) {
            return;
        }

        btnApply.setEnabled(false);

        try {
            Object encryptionTag = tvEncryption.getTag();
            String encryption = encryptionTag != null ? encryptionTag.toString() : ENCRYPTION_MIXED;
            String onValue = switchGuestWifi.isChecked() ? "1" : "0";
            String password = encryption.equals(ENCRYPTION_NONE) ? "" : etPassword.getText().toString();

            Map<String, String> params = new HashMap<>();
            params.put("wifiIndex", "3");
            params.put("on", onValue);
            params.put("enabled", "1");
            params.put("status", onValue);
            params.put("ssid", etGuestWifiName.getText().toString().trim());
            params.put("pwd", password);
            params.put("encryption", encryption);
            params.put("hidden", "0");
            params.put("txpwr", "max");

            String url = BASE_URL + stok + "/api/xqnetwork/set_wifi";

            Log.d("GuestWiFi", "Sending request with params: " + params.toString());

            CustomStringRequest request = new CustomStringRequest(Request.Method.POST, url, params,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            Log.d("GuestWiFi", "Received response: " + jsonResponse.toString());
                            if (jsonResponse.getInt("code") == 0) {
                                dbHelper.putBoolean(TABLE_NAME, KEY_WIFI_ENABLED, switchGuestWifi.isChecked());
                                dbHelper.putString(TABLE_NAME, KEY_NETWORK_NAME, etGuestWifiName.getText().toString().trim());
                                dbHelper.putString(TABLE_NAME, KEY_ENCRYPTION_TYPE, encryption);
                                dbHelper.putString(TABLE_NAME, KEY_PASSWORD, password);

                                hasChanges = false;
                                updateApplyButtonState();

                                String action = switchGuestWifi.isChecked() ? "enabled" : "disabled";
                                Toast.makeText(this, "Guest WiFi " + action + " successfully", Toast.LENGTH_SHORT).show();

                                new Handler().postDelayed(() -> {
                                    Log.d("GuestWiFi", "Verifying settings after delay...");
                                    fetchGuestWifiSettings();
                                }, 3000);
                            } else {
                                String errorMsg = jsonResponse.optString("msg", "Unknown error");
                                Log.e("GuestWiFi", "API Error: " + errorMsg);
                                loadSavedSettings();
                                Toast.makeText(this, "Failed to save settings: " + errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("GuestWiFi", "JSON Error: " + e.getMessage());
                            loadSavedSettings();
                            Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
                        }
                        btnApply.setEnabled(true);
                    },
                    error -> {
                        error.printStackTrace();
                        Log.e("GuestWiFi", "Network Error: " + error.getMessage());
                        loadSavedSettings();
                        Toast.makeText(this, "Error communicating with router", Toast.LENGTH_SHORT).show();
                        btnApply.setEnabled(true);
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    2,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GuestWiFi", "Error creating request: " + e.getMessage());
            Toast.makeText(this, "Error preparing settings", Toast.LENGTH_SHORT).show();
            btnApply.setEnabled(true);
        }
    }

    private boolean validateSettings() {
        String ssid = etGuestWifiName.getText().toString().trim();
        if (ssid.isEmpty()) {
            Toast.makeText(this, "Please enter a network name", Toast.LENGTH_SHORT).show();
            etGuestWifiName.requestFocus();
            return false;
        }

        Object encryptionTag = tvEncryption.getTag();
        String encryption = encryptionTag != null ? encryptionTag.toString() : ENCRYPTION_MIXED;
        if (!encryption.equals(ENCRYPTION_NONE)) {
            String password = etPassword.getText().toString();
            if (password.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                etPassword.requestFocus();
                return false;
            }
        }

        return true;
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
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
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

    private static class CustomStringRequest extends com.android.volley.toolbox.StringRequest {
        private final Map<String, String> params;

        public CustomStringRequest(int method, String url, Map<String, String> params,
                                com.android.volley.Response.Listener<String> listener,
                                com.android.volley.Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            this.params = params;
        }

        @Override
        protected Map<String, String> getParams() {
            return params;
        }

        @Override
        public String getBodyContentType() {
            return "application/x-www-form-urlencoded; charset=UTF-8";
        }
    }
}