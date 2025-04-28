package com.example.wifimanager.Settings_Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import androidx.core.widget.NestedScrollView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.wifimanager.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class wifi_settings extends AppCompatActivity {

    // SharedPreferences constants
    private static final String PREFS_NAME = "WifiSettingsPrefs";
    private static final String PREF_WIFI_NAME = "wifi_name";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_ENCRYPTION = "encryption";
    private static final String PREF_SIGNAL_STRENGTH = "signal_strength";
    private static final String PREF_CHANNEL = "channel";
    private static final String PREF_BANDWIDTH = "bandwidth";
    private static final String PREF_HIDE_NETWORK = "hide_network";
    private static final String PREF_WIFI_STATE = "wifi_state";
    private static final String PREF_LAST_UPDATE_TIME = "last_update_time";
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes in milliseconds

    // UI components
    private EditText etWifiName;
    private EditText etPassword;
    private TextView tvEncryption;
    private TextView tvSignalStrength;
    private TextView tvChannel;
    private TextView tvBandwidth;
    private SwitchMaterial switchWifi;
    private SwitchMaterial switchHideNetwork;
    private Button btnApply;
    private ImageView ivTogglePassword;
    private ConstraintLayout dialogEncryption;
    private ConstraintLayout dialogSignalStrength;
    private ConstraintLayout dialogChannel;
    private ConstraintLayout dialogBandwidth;
    private NestedScrollView scrollView;

    // Radio buttons for dialogs
    private RadioButton radioNone;
    private RadioButton radioWpaWpa2;
    private RadioButton radioWpa2;
    private RadioButton radioHigh;
    private RadioButton radioMedium;
    private RadioButton radioLow;
    private RadioButton radioAutoChannel;
    private RadioButton radioChannel1, radioChannel2, radioChannel3, radioChannel4, radioChannel5,
            radioChannel6, radioChannel7, radioChannel8, radioChannel9, radioChannel10,
            radioChannel11, radioChannel12, radioChannel13;
    private RadioButton radio20Mhz;
    private RadioButton radio40Mhz;

    // Original values to track changes
    private String originalWifiName;
    private String originalPassword;
    private String originalEncryption;
    private String originalSignalStrength;
    private String originalChannel;
    private String originalBandwidth;
    private boolean originalHideNetwork;
    private boolean originalWifiState;

    // Current password visibility state
    private boolean isPasswordVisible = false;

    // API related
    private String stok;
    private RequestQueue requestQueue;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        // Get STOK from intent
        stok = getIntent().getStringExtra("STOK");
        requestQueue = Volley.newRequestQueue(this);

        // Initialize UI components
        initializeViews();

        // Set up toolbar with back arrow
        setupToolbar();

        // Setup window insets
        setupWindowInsets();

        // Set up touch listener to dismiss keyboard when clicking outside EditText
        setupTouchListener();

        // First check if we need to fetch fresh data
        if (shouldFetchFreshData()) {
            // Fetch fresh Wi-Fi settings from API
            fetchWifiSettings();
        } else {
            // Load settings from SharedPreferences while we fetch fresh data in background
            loadSettingsFromPrefs();
            fetchWifiSettings(); // Still fetch to ensure we have latest, but don't wait for it
        }

        // Setup listeners
        setupListeners();

        Log.d("WifiSettings", "Activity created");
    }

    private boolean shouldFetchFreshData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(PREF_LAST_UPDATE_TIME, 0);

        // If no data exists or data is stale, fetch fresh data
        return !prefs.contains(PREF_WIFI_NAME) ||
                (System.currentTimeMillis() - lastUpdateTime) > CACHE_EXPIRY_TIME;
    }

    private void saveSettingsToPrefs(JSONObject wifiInfo) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save all settings from the API response
            if (wifiInfo.has("ssid")) {
                editor.putString(PREF_WIFI_NAME, wifiInfo.getString("ssid").trim());
            }

            if (wifiInfo.has("password")) {
                editor.putString(PREF_PASSWORD, wifiInfo.getString("password"));
            }

            if (wifiInfo.has("encryption")) {
                String encryption = wifiInfo.getString("encryption");
                if (encryption.equals("mixed-psk")) {
                    editor.putString(PREF_ENCRYPTION, "WPA/WPA2");
                } else if (encryption.equals("psk2")) {
                    editor.putString(PREF_ENCRYPTION, "WPA2");
                } else if (encryption.equals("none")) {
                    editor.putString(PREF_ENCRYPTION, "None");
                }
            }

            if (wifiInfo.has("txpwr")) {
                String signal = wifiInfo.getString("txpwr");
                if (signal.equals("max")) {
                    editor.putString(PREF_SIGNAL_STRENGTH, "High");
                } else if (signal.equals("mid")) {
                    editor.putString(PREF_SIGNAL_STRENGTH, "Medium");
                } else if (signal.equals("min")) {
                    editor.putString(PREF_SIGNAL_STRENGTH, "Low");
                }
            }

            // Handle channel - check both nested and direct properties
            if (wifiInfo.has("channelInfo") && wifiInfo.getJSONObject("channelInfo").has("channel")) {
                editor.putString(PREF_CHANNEL, "Channel " + wifiInfo.getJSONObject("channelInfo").getString("channel"));
            } else if (wifiInfo.has("channel")) {
                editor.putString(PREF_CHANNEL, "Channel " + wifiInfo.getString("channel"));
            }

            // Handle bandwidth - check both nested and direct properties
            if (wifiInfo.has("channelInfo") && wifiInfo.getJSONObject("channelInfo").has("bandwidth")) {
                String bandwidth = wifiInfo.getJSONObject("channelInfo").getString("bandwidth");
                editor.putString(PREF_BANDWIDTH, bandwidth.equals("20") ? "20 MHz" : "40 MHz");
            } else if (wifiInfo.has("bandwidth")) {
                String bandwidth = wifiInfo.getString("bandwidth");
                editor.putString(PREF_BANDWIDTH, bandwidth.equals("20") ? "20 MHz" : "40 MHz");
            }

            if (wifiInfo.has("hidden")) {
                editor.putBoolean(PREF_HIDE_NETWORK, wifiInfo.getString("hidden").equals("1"));
            }

            if (wifiInfo.has("status")) {
                boolean isEnabled;
                if (wifiInfo.get("status") instanceof String) {
                    isEnabled = wifiInfo.getString("status").equals("1");
                } else {
                    isEnabled = wifiInfo.getInt("status") == 1;
                }
                editor.putBoolean(PREF_WIFI_STATE, isEnabled);
            }

            // Save the current time as last update time
            editor.putLong(PREF_LAST_UPDATE_TIME, System.currentTimeMillis());

            editor.apply();

            Log.d("WifiSettings", "Settings saved to SharedPreferences from API response");
        } catch (JSONException e) {
            Log.e("WifiSettings", "Error saving settings to SharedPreferences: " + e.getMessage());
        }
    }

    private void loadSettingsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if we have saved preferences
        if (!prefs.contains(PREF_WIFI_NAME)) {
            Log.d("WifiSettings", "No saved settings found in SharedPreferences");
            return;
        }

        // Load settings from SharedPreferences
        etWifiName.setText(prefs.getString(PREF_WIFI_NAME, ""));
        etPassword.setText(prefs.getString(PREF_PASSWORD, ""));

        String encryption = prefs.getString(PREF_ENCRYPTION, "WPA2");
        tvEncryption.setText(encryption);

        // Update radio button states based on encryption
        if (encryption.equals("None")) {
            radioNone.setChecked(true);
            etPassword.setEnabled(false);
            etPassword.setAlpha(0.5f);
            ivTogglePassword.setEnabled(false);
            ivTogglePassword.setAlpha(0.5f);
        } else if (encryption.equals("WPA/WPA2")) {
            radioWpaWpa2.setChecked(true);
        } else if (encryption.equals("WPA2")) {
            radioWpa2.setChecked(true);
        }

        String signalStrength = prefs.getString(PREF_SIGNAL_STRENGTH, "Medium");
        tvSignalStrength.setText(signalStrength);

        String channel = prefs.getString(PREF_CHANNEL, "Auto");
        tvChannel.setText(channel);

        String bandwidth = prefs.getString(PREF_BANDWIDTH, "20 MHz");
        tvBandwidth.setText(bandwidth);

        boolean hideNetwork = prefs.getBoolean(PREF_HIDE_NETWORK, false);
        switchHideNetwork.setChecked(hideNetwork);

        boolean wifiState = prefs.getBoolean(PREF_WIFI_STATE, true);
        switchWifi.setChecked(wifiState);

        // Apply the Wi-Fi state to enable/disable settings
        setSettingsEnabled(wifiState);

        // Save these as original values to prevent the apply button from being enabled
        saveOriginalValues();

        Log.d("WifiSettings", "Settings loaded from SharedPreferences");
    }

    private void fetchWifiSettings() {
        if (stok == null || stok.isEmpty()) {
            Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
            Log.e("WifiSettings", "STOK token is null or empty");
            return;
        }

        String url = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/xqnetwork/wifi_detail_all";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("WifiSettings", "API Response: " + response.toString());

                        if (response.getInt("code") == 0) {
                            JSONArray infoArray = response.getJSONArray("info");
                            if (infoArray.length() > 0) {
                                // Find the main WiFi network (usually the first one, but let's check for wl1)
                                JSONObject wifiInfo = null;
                                for (int i = 0; i < infoArray.length(); i++) {
                                    JSONObject network = infoArray.getJSONObject(i);
                                    if (network.has("ifname") && network.getString("ifname").equals("wl1")) {
                                        wifiInfo = network;
                                        break;
                                    }
                                }

                                // If no wl1 found, use the first one
                                if (wifiInfo == null && infoArray.length() > 0) {
                                    wifiInfo = infoArray.getJSONObject(0);
                                }

                                if (wifiInfo != null) {
                                    // Save the fresh data to SharedPreferences
                                    saveSettingsToPrefs(wifiInfo);

                                    // Update UI with the fresh data
                                    updateUiWithWifiSettings(wifiInfo);

                                    // Save original values after loading from API
                                    saveOriginalValues();

                                    // Mark initial load as complete
                                    isInitialLoad = false;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("WifiSettings", "Error parsing Wi-Fi settings: " + e.getMessage());
                        Toast.makeText(wifi_settings.this, "Error parsing Wi-Fi settings", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("WifiSettings", "Error fetching Wi-Fi settings: " + error.toString());
                    // Only show error if we don't have cached data
                    if (isInitialLoad && !getSharedPreferences(PREFS_NAME, MODE_PRIVATE).contains(PREF_WIFI_NAME)) {
                        Toast.makeText(wifi_settings.this, "Error fetching Wi-Fi settings", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void updateUiWithWifiSettings(JSONObject wifiInfo) throws JSONException {
        // Add logging to see the exact WiFi info being processed
        Log.d("WifiSettings", "Processing WiFi info: " + wifiInfo.toString());

        // Set Wi-Fi name
        if (wifiInfo.has("ssid")) {
            String ssid = wifiInfo.getString("ssid").trim();
            etWifiName.setText(ssid);
        }

        // Set password if available
        if (wifiInfo.has("password")) {
            String password = wifiInfo.getString("password");
            etPassword.setText(password);
        }

        // Set encryption
        if (wifiInfo.has("encryption")) {
            String encryption = wifiInfo.getString("encryption");
            if (encryption.equals("mixed-psk")) {
                tvEncryption.setText("WPA/WPA2");
                radioWpaWpa2.setChecked(true);
            } else if (encryption.equals("psk2")) {
                tvEncryption.setText("WPA2");
                radioWpa2.setChecked(true);
            } else if (encryption.equals("none")) {
                tvEncryption.setText("None");
                radioNone.setChecked(true);
                etPassword.setEnabled(false);
                etPassword.setAlpha(0.5f);
                ivTogglePassword.setEnabled(false);
                ivTogglePassword.setAlpha(0.5f);
            }
        }

        // Set channel - handle the nested channelInfo structure
        if (wifiInfo.has("channelInfo") && wifiInfo.getJSONObject("channelInfo").has("channel")) {
            String channel = wifiInfo.getJSONObject("channelInfo").getString("channel");
            tvChannel.setText("Channel " + channel);
        } else if (wifiInfo.has("channel")) {
            // Fallback to direct channel property
            String channel = wifiInfo.getString("channel");
            tvChannel.setText("Channel " + channel);
        }
        updateChannelRadioButtons();

        // Set bandwidth - check both nested and direct properties
        if (wifiInfo.has("channelInfo") && wifiInfo.getJSONObject("channelInfo").has("bandwidth")) {
            String bandwidth = wifiInfo.getJSONObject("channelInfo").getString("bandwidth");
            setBandwidthUI(bandwidth);
        } else if (wifiInfo.has("bandwidth")) {
            // Fallback to direct bandwidth property
            String bandwidth = wifiInfo.getString("bandwidth");
            setBandwidthUI(bandwidth);
        }

        // Set signal strength (assuming txpwr is signal strength)
        if (wifiInfo.has("txpwr")) {
            String signal = wifiInfo.getString("txpwr");
            if (signal.equals("max")) {
                tvSignalStrength.setText("High");
                radioHigh.setChecked(true);
            } else if (signal.equals("mid")) {
                tvSignalStrength.setText("Medium");
                radioMedium.setChecked(true);
            } else if (signal.equals("min")) {
                tvSignalStrength.setText("Low");
                radioLow.setChecked(true);
            }
        }

        // Set hidden network status
        if (wifiInfo.has("hidden")) {
            boolean isHidden = wifiInfo.getString("hidden").equals("1");
            switchHideNetwork.setChecked(isHidden);
        }

        // Set Wi-Fi status
        if (wifiInfo.has("status")) {
            boolean isEnabled;
            if (wifiInfo.get("status") instanceof String) {
                isEnabled = wifiInfo.getString("status").equals("1");
            } else {
                isEnabled = wifiInfo.getInt("status") == 1;
            }
            switchWifi.setChecked(isEnabled);
            setSettingsEnabled(isEnabled);
        }
    }

    // Helper method for setting bandwidth UI
    private void setBandwidthUI(String bandwidth) {
        if (bandwidth.equals("20")) {
            tvBandwidth.setText("20 MHz");
            radio20Mhz.setChecked(true);
        } else if (bandwidth.equals("40")) {
            tvBandwidth.setText("40 MHz");
            radio40Mhz.setChecked(true);
        }
    }

    private void setSettingsEnabled(boolean enabled) {
        // Enable/disable the entire settings card
        findViewById(R.id.settings_card).setEnabled(enabled);
        findViewById(R.id.settings_card).setAlpha(enabled ? 1.0f : 0.5f);

        // Enable/disable Wi-Fi name field
        etWifiName.setEnabled(enabled);

        // Enable/disable password field based on encryption and Wi-Fi state
        boolean passwordEnabled = enabled && !tvEncryption.getText().toString().equals("None");
        etPassword.setEnabled(passwordEnabled);
        etPassword.setAlpha(passwordEnabled ? 1.0f : 0.5f);
        ivTogglePassword.setEnabled(passwordEnabled);
        ivTogglePassword.setAlpha(passwordEnabled ? 1.0f : 0.5f);

        // Enable/disable other settings
        findViewById(R.id.layout_encryption).setEnabled(enabled);
        findViewById(R.id.layout_signal_strength).setEnabled(enabled);
        findViewById(R.id.layout_channel).setEnabled(enabled);
        findViewById(R.id.layout_bandwidth).setEnabled(enabled);
        switchHideNetwork.setEnabled(enabled);

        // If disabling, make sure Apply button is also disabled
        if (!enabled) {
            setApplyButtonEnabled(false);
        }
    }

    private void initializeViews() {
        scrollView = findViewById(R.id.scroll_view);
        etWifiName = findViewById(R.id.et_wifi_name);
        etPassword = findViewById(R.id.tv_password);
        tvEncryption = findViewById(R.id.tv_encryption);
        tvSignalStrength = findViewById(R.id.tv_signal_strength);
        tvChannel = findViewById(R.id.tv_channel);
        tvBandwidth = findViewById(R.id.tv_bandwidth);
        switchWifi = findViewById(R.id.switch_wifi);
        switchHideNetwork = findViewById(R.id.switch_hide_network);
        btnApply = findViewById(R.id.btn_apply);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        dialogEncryption = findViewById(R.id.dialog_encryption);
        dialogSignalStrength = findViewById(R.id.dialog_signal_strength);
        dialogChannel = findViewById(R.id.dialog_channel);
        dialogBandwidth = findViewById(R.id.dialog_bandwidth);

        // Initialize radio buttons
        radioNone = findViewById(R.id.radio_none);
        radioWpaWpa2 = findViewById(R.id.radio_wpa_wpa2);
        radioWpa2 = findViewById(R.id.radio_wpa2);
        radioHigh = findViewById(R.id.radio_high);
        radioMedium = findViewById(R.id.radio_medium);
        radioLow = findViewById(R.id.radio_low);
        radioAutoChannel = findViewById(R.id.radio_auto_channel);
        radioChannel1 = findViewById(R.id.radio_channel_1);
        radioChannel2 = findViewById(R.id.radio_channel_2);
        radioChannel3 = findViewById(R.id.radio_channel_3);
        radioChannel4 = findViewById(R.id.radio_channel_4);
        radioChannel5 = findViewById(R.id.radio_channel_5);
        radioChannel6 = findViewById(R.id.radio_channel_6);
        radioChannel7 = findViewById(R.id.radio_channel_7);
        radioChannel8 = findViewById(R.id.radio_channel_8);
        radioChannel9 = findViewById(R.id.radio_channel_9);
        radioChannel10 = findViewById(R.id.radio_channel_10);
        radioChannel11 = findViewById(R.id.radio_channel_11);
        radioChannel12 = findViewById(R.id.radio_channel_12);
        radioChannel13 = findViewById(R.id.radio_channel_13);
        radio20Mhz = findViewById(R.id.radio_20mhz);
        radio40Mhz = findViewById(R.id.radio_40mhz);

        // Initialize dialogs to GONE
        dialogEncryption.setVisibility(View.GONE);
        dialogSignalStrength.setVisibility(View.GONE);
        dialogChannel.setVisibility(View.GONE);
        dialogBandwidth.setVisibility(View.GONE);

        // Set the appropriate elevation and layout parameters
        ConstraintLayout mainLayout = findViewById(R.id.main);
        dialogEncryption.setElevation(20f);
        dialogSignalStrength.setElevation(20f);
        dialogChannel.setElevation(20f);
        dialogBandwidth.setElevation(20f);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        ConstraintLayout mainLayout = findViewById(R.id.main);
        mainLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    Rect outRect = new Rect();
                    currentFocus.getGlobalVisibleRect(outRect);
                    if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                        currentFocus.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        return true;
                    }
                }
            }
            return false;
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

    private void saveOriginalValues() {
        originalWifiName = etWifiName.getText().toString();
        originalPassword = etPassword.getText().toString();
        originalEncryption = tvEncryption.getText().toString();
        originalSignalStrength = tvSignalStrength.getText().toString();
        originalChannel = tvChannel.getText().toString();
        originalBandwidth = tvBandwidth.getText().toString();
        originalHideNetwork = switchHideNetwork.isChecked();
        originalWifiState = switchWifi.isChecked();
    }

    private void setupListeners() {
        setupPasswordToggle();
        setupTextChangeListeners();
        setupEncryptionDialog();
        setupSignalStrengthDialog();
        setupChannelDialog();
        setupBandwidthDialog();
        setupHideNetworkSwitch();
        setupWifiToggleSwitch();
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
        findViewById(R.id.layout_encryption).setOnClickListener(v -> {
            dialogEncryption.setVisibility(View.VISIBLE);
            dialogEncryption.bringToFront();
            dialogEncryption.invalidate();
            dialogEncryption.requestLayout();
            updateEncryptionRadioButtons();
        });

        View.OnClickListener encryptionOptionClickListener = v -> {
            radioNone.setChecked(v.getId() == R.id.option_none || v.getId() == R.id.radio_none);
            radioWpaWpa2.setChecked(v.getId() == R.id.option_wpa_wpa2 || v.getId() == R.id.radio_wpa_wpa2);
            radioWpa2.setChecked(v.getId() == R.id.option_wpa2 || v.getId() == R.id.radio_wpa2);
        };

        findViewById(R.id.option_none).setOnClickListener(encryptionOptionClickListener);
        findViewById(R.id.option_wpa_wpa2).setOnClickListener(encryptionOptionClickListener);
        findViewById(R.id.option_wpa2).setOnClickListener(encryptionOptionClickListener);
        radioNone.setOnClickListener(encryptionOptionClickListener);
        radioWpaWpa2.setOnClickListener(encryptionOptionClickListener);
        radioWpa2.setOnClickListener(encryptionOptionClickListener);

        findViewById(R.id.btn_cancel_encryption).setOnClickListener(v -> dialogEncryption.setVisibility(View.GONE));

        findViewById(R.id.btn_ok_encryption).setOnClickListener(v -> {
            if (radioNone.isChecked()) {
                tvEncryption.setText("None");
                etPassword.setEnabled(false);
                etPassword.setAlpha(0.5f);
                ivTogglePassword.setEnabled(false);
                ivTogglePassword.setAlpha(0.5f);
            } else if (radioWpaWpa2.isChecked()) {
                tvEncryption.setText("WPA/WPA2");
                etPassword.setEnabled(true);
                etPassword.setAlpha(1.0f);
                ivTogglePassword.setEnabled(true);
                ivTogglePassword.setAlpha(1.0f);
            } else if (radioWpa2.isChecked()) {
                tvEncryption.setText("WPA2");
                etPassword.setEnabled(true);
                etPassword.setAlpha(1.0f);
                ivTogglePassword.setEnabled(true);
                ivTogglePassword.setAlpha(1.0f);
            }
            dialogEncryption.setVisibility(View.GONE);
            checkForChanges();
        });
    }

    private void updateEncryptionRadioButtons() {
        String currentEncryption = tvEncryption.getText().toString();
        radioNone.setChecked(currentEncryption.equals("None"));
        radioWpaWpa2.setChecked(currentEncryption.equals("WPA/WPA2"));
        radioWpa2.setChecked(currentEncryption.equals("WPA2"));
    }

    private void setupSignalStrengthDialog() {
        findViewById(R.id.layout_signal_strength).setOnClickListener(v -> {
            dialogSignalStrength.setVisibility(View.VISIBLE);
            dialogSignalStrength.bringToFront();
            dialogSignalStrength.invalidate();
            dialogSignalStrength.requestLayout();
            updateSignalStrengthRadioButtons();
        });

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

        findViewById(R.id.btn_cancel_signal).setOnClickListener(v -> dialogSignalStrength.setVisibility(View.GONE));

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
        String currentStrength = tvSignalStrength.getText().toString();
        radioHigh.setChecked(currentStrength.equals("High"));
        radioMedium.setChecked(currentStrength.equals("Medium"));
        radioLow.setChecked(currentStrength.equals("Low"));
    }

    private void setupChannelDialog() {
        findViewById(R.id.layout_channel).setOnClickListener(v -> {
            dialogChannel.setVisibility(View.VISIBLE);
            updateChannelRadioButtons();
        });

        View.OnClickListener channelOptionClickListener = v -> {
            radioAutoChannel.setChecked(v.getId() == R.id.option_auto_channel || v.getId() == R.id.radio_auto_channel);
            radioChannel1.setChecked(v.getId() == R.id.option_channel_1 || v.getId() == R.id.radio_channel_1);
            radioChannel2.setChecked(v.getId() == R.id.option_channel_2 || v.getId() == R.id.radio_channel_2);
            radioChannel3.setChecked(v.getId() == R.id.option_channel_3 || v.getId() == R.id.radio_channel_3);
            radioChannel4.setChecked(v.getId() == R.id.option_channel_4 || v.getId() == R.id.radio_channel_4);
            radioChannel5.setChecked(v.getId() == R.id.option_channel_5 || v.getId() == R.id.radio_channel_5);
            radioChannel6.setChecked(v.getId() == R.id.option_channel_6 || v.getId() == R.id.radio_channel_6);
            radioChannel7.setChecked(v.getId() == R.id.option_channel_7 || v.getId() == R.id.radio_channel_7);
            radioChannel8.setChecked(v.getId() == R.id.option_channel_8 || v.getId() == R.id.radio_channel_8);
            radioChannel9.setChecked(v.getId() == R.id.option_channel_9 || v.getId() == R.id.radio_channel_9);
            radioChannel10.setChecked(v.getId() == R.id.option_channel_10 || v.getId() == R.id.radio_channel_10);
            radioChannel11.setChecked(v.getId() == R.id.option_channel_11 || v.getId() == R.id.radio_channel_11);
            radioChannel12.setChecked(v.getId() == R.id.option_channel_12 || v.getId() == R.id.radio_channel_12);
            radioChannel13.setChecked(v.getId() == R.id.option_channel_13 || v.getId() == R.id.radio_channel_13);
        };

        // Set click listeners for all channel options
        int[] channelOptionIds = {
                R.id.option_auto_channel, R.id.option_channel_1, R.id.option_channel_2,
                R.id.option_channel_3, R.id.option_channel_4, R.id.option_channel_5,
                R.id.option_channel_6, R.id.option_channel_7, R.id.option_channel_8,
                R.id.option_channel_9, R.id.option_channel_10, R.id.option_channel_11,
                R.id.option_channel_12, R.id.option_channel_13
        };

        for (int id : channelOptionIds) {
            findViewById(id).setOnClickListener(channelOptionClickListener);
        }

        // Set click listeners for all radio buttons
        int[] radioButtonIds = {
                R.id.radio_auto_channel, R.id.radio_channel_1, R.id.radio_channel_2,
                R.id.radio_channel_3, R.id.radio_channel_4, R.id.radio_channel_5,
                R.id.radio_channel_6, R.id.radio_channel_7, R.id.radio_channel_8,
                R.id.radio_channel_9, R.id.radio_channel_10, R.id.radio_channel_11,
                R.id.radio_channel_12, R.id.radio_channel_13
        };

        for (int id : radioButtonIds) {
            findViewById(id).setOnClickListener(channelOptionClickListener);
        }

        findViewById(R.id.btn_cancel_channel).setOnClickListener(v -> dialogChannel.setVisibility(View.GONE));

        findViewById(R.id.btn_ok_channel).setOnClickListener(v -> {
            String selectedChannel = getSelectedChannel();
            if (selectedChannel != null) {
                tvChannel.setText(selectedChannel);
                checkForChanges();
            }
            dialogChannel.setVisibility(View.GONE);
        });
    }

    private String getSelectedChannel() {
        if (radioAutoChannel.isChecked()) return "Auto";
        if (radioChannel1.isChecked()) return "Channel 1";
        if (radioChannel2.isChecked()) return "Channel 2";
        if (radioChannel3.isChecked()) return "Channel 3";
        if (radioChannel4.isChecked()) return "Channel 4";
        if (radioChannel5.isChecked()) return "Channel 5";
        if (radioChannel6.isChecked()) return "Channel 6";
        if (radioChannel7.isChecked()) return "Channel 7";
        if (radioChannel8.isChecked()) return "Channel 8";
        if (radioChannel9.isChecked()) return "Channel 9";
        if (radioChannel10.isChecked()) return "Channel 10";
        if (radioChannel11.isChecked()) return "Channel 11";
        if (radioChannel12.isChecked()) return "Channel 12";
        if (radioChannel13.isChecked()) return "Channel 13";
        return null;
    }

    private void updateChannelRadioButtons() {
        String currentChannel = tvChannel.getText().toString();
        radioAutoChannel.setChecked(currentChannel.equals("Auto"));
        radioChannel1.setChecked(currentChannel.equals("Channel 1"));
        radioChannel2.setChecked(currentChannel.equals("Channel 2"));
        radioChannel3.setChecked(currentChannel.equals("Channel 3"));
        radioChannel4.setChecked(currentChannel.equals("Channel 4"));
        radioChannel5.setChecked(currentChannel.equals("Channel 5"));
        radioChannel6.setChecked(currentChannel.equals("Channel 6"));
        radioChannel7.setChecked(currentChannel.equals("Channel 7"));
        radioChannel8.setChecked(currentChannel.equals("Channel 8"));
        radioChannel9.setChecked(currentChannel.equals("Channel 9"));
        radioChannel10.setChecked(currentChannel.equals("Channel 10"));
        radioChannel11.setChecked(currentChannel.equals("Channel 11"));
        radioChannel12.setChecked(currentChannel.equals("Channel 12"));
        radioChannel13.setChecked(currentChannel.equals("Channel 13"));
    }

    private void setupBandwidthDialog() {
        findViewById(R.id.layout_bandwidth).setOnClickListener(v -> {
            dialogBandwidth.setVisibility(View.VISIBLE);
            dialogBandwidth.bringToFront();
            dialogBandwidth.invalidate();
            dialogBandwidth.requestLayout();
            updateBandwidthRadioButtons();
        });

        View.OnClickListener bandwidthOptionClickListener = v -> {
            radio20Mhz.setChecked(v.getId() == R.id.option_20mhz || v.getId() == R.id.radio_20mhz);
            radio40Mhz.setChecked(v.getId() == R.id.option_40mhz || v.getId() == R.id.radio_40mhz);
        };

        findViewById(R.id.option_20mhz).setOnClickListener(bandwidthOptionClickListener);
        findViewById(R.id.option_40mhz).setOnClickListener(bandwidthOptionClickListener);
        radio20Mhz.setOnClickListener(bandwidthOptionClickListener);
        radio40Mhz.setOnClickListener(bandwidthOptionClickListener);

        findViewById(R.id.btn_cancel_bandwidth).setOnClickListener(v -> dialogBandwidth.setVisibility(View.GONE));

        findViewById(R.id.btn_ok_bandwidth).setOnClickListener(v -> {
            if (radio20Mhz.isChecked()) {
                tvBandwidth.setText("20 MHz");
            } else if (radio40Mhz.isChecked()) {
                tvBandwidth.setText("40 MHz");
            }
            dialogBandwidth.setVisibility(View.GONE);
            checkForChanges();
        });
    }

    private void updateBandwidthRadioButtons() {
        String currentBandwidth = tvBandwidth.getText().toString();
        radio20Mhz.setChecked(currentBandwidth.equals("20 MHz"));
        radio40Mhz.setChecked(currentBandwidth.equals("40 MHz"));
    }

    private void setupHideNetworkSwitch() {
        switchHideNetwork.setOnCheckedChangeListener((buttonView, isChecked) -> checkForChanges());
    }

    private void setupWifiToggleSwitch() {
        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setSettingsEnabled(isChecked);
            checkForChanges();
        });
    }

    private void setupApplyButton() {
        btnApply.setOnClickListener(v -> {
            // Show a progress dialog or spinner while applying settings
            Toast.makeText(this, "Applying Wi-Fi settings...", Toast.LENGTH_SHORT).show();

            // Call the API to apply settings to the router
            applyWifiSettingsToRouter();

            // The UI update will happen in the API response callback
        });
    }

    private void applyWifiState(boolean enabled) {
        // Actually enable/disable settings based on the new state
        findViewById(R.id.settings_card).setAlpha(enabled ? 1.0f : 0.5f);
        etWifiName.setEnabled(enabled);

        boolean passwordEnabled = enabled && !tvEncryption.getText().toString().equals("None");
        etPassword.setEnabled(passwordEnabled);
        etPassword.setAlpha(passwordEnabled ? 1.0f : 0.5f);
        ivTogglePassword.setEnabled(passwordEnabled);
        ivTogglePassword.setAlpha(passwordEnabled ? 1.0f : 0.5f);

        findViewById(R.id.layout_encryption).setEnabled(enabled);
        findViewById(R.id.layout_signal_strength).setEnabled(enabled);
        findViewById(R.id.layout_channel).setEnabled(enabled);
        findViewById(R.id.layout_bandwidth).setEnabled(enabled);
        switchHideNetwork.setEnabled(enabled);

        if (!enabled) {
            setApplyButtonEnabled(false);
        }
    }

    private void checkForChanges() {
        boolean hasChanges = switchWifi.isChecked() != originalWifiState ||
                !etWifiName.getText().toString().equals(originalWifiName) ||
                !etPassword.getText().toString().equals(originalPassword) ||
                !tvEncryption.getText().toString().equals(originalEncryption) ||
                !tvSignalStrength.getText().toString().equals(originalSignalStrength) ||
                !tvChannel.getText().toString().equals(originalChannel) ||
                !tvBandwidth.getText().toString().equals(originalBandwidth) ||
                switchHideNetwork.isChecked() != originalHideNetwork;

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

    private void applyWifiSettingsToRouter() {
        // Check if we have a valid authentication token
        if (stok == null || stok.isEmpty()) {
            Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
            Log.e("WifiSettings", "STOK token is null or empty");
            return;
        }

        // API endpoint URL with the stok token
        String url = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/xqnetwork/set_wifi";

        // Get current values from UI
        String ssid = etWifiName.getText().toString().trim();
        String password = etPassword.getText().toString();
        boolean wifiEnabled = switchWifi.isChecked();
        String encryption = tvEncryption.getText().toString();
        String signalStrength = tvSignalStrength.getText().toString();
        String channel = tvChannel.getText().toString().replace("Channel ", ""); // Extract just the number
        String bandwidth = tvBandwidth.getText().toString().replace(" MHz", ""); // Extract just the number
        boolean isHidden = switchHideNetwork.isChecked();

        // Map UI values to API values
        String encryptionValue;
        if (encryption.equals("None")) {
            encryptionValue = "none";
        } else if (encryption.equals("WPA/WPA2")) {
            encryptionValue = "mixed-psk";
        } else { // WPA2
            encryptionValue = "psk2";
        }

        String txpwr;
        if (signalStrength.equals("High")) {
            txpwr = "max";
        } else if (signalStrength.equals("Medium")) {
            txpwr = "mid";
        } else { // Low
            txpwr = "min";
        }

        // Check if channel is Auto
        if (channel.equals("Auto")) {
            channel = "0"; // Assuming 0 means auto-select channel
        }

        // Build request parameters
        Map<String, String> params = new HashMap<>();
        params.put("wifiIndex", "1"); // Assuming this is always 1 for your case
        params.put("on", wifiEnabled ? "1" : "0");
        params.put("ssid", ssid);

        // Only include password if encryption is not none
        if (!encryption.equals("None")) {
            params.put("pwd", password);
        }

        params.put("encryption", encryptionValue);
        params.put("channel", channel);
        params.put("bandwidth", bandwidth);
        params.put("hidden", isHidden ? "1" : "0");
        params.put("txpwr", txpwr);

        // Log the parameters we're sending (with masked password)
        StringBuilder logParams = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().equals("pwd")) {
                logParams.append(entry.getKey()).append("=******&");
            } else {
                logParams.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        Log.d("WifiSettings", "API Request Params: " + logParams.toString());

        // Convert parameters to x-www-form-urlencoded format
        StringBuilder requestBody = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                if (requestBody.length() > 0) {
                    requestBody.append("&");
                }
                requestBody.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        // Create the request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        int code = jsonResponse.getInt("code");

                        if (code == 0) {
                            // API call successful
                            Toast.makeText(wifi_settings.this, "Wi-Fi settings applied successfully", Toast.LENGTH_SHORT).show();

                            // Save settings to SharedPreferences as backup
                            saveSettingsToPrefs(new JSONObject(params));

                            // Update original values to prevent "Apply" button being enabled
                            saveOriginalValues();

                            // Now trigger the router reboot
                            rebootRouter();
                        } else {
                            // API returned an error
                            String msg = jsonResponse.optString("msg", "Unknown error");
                            Toast.makeText(wifi_settings.this, "Error: " + msg, Toast.LENGTH_LONG).show();
                            Log.e("WifiSettings", "API Error: " + msg);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("WifiSettings", "Error parsing response: " + e.getMessage());
                        Toast.makeText(wifi_settings.this, "Error applying Wi-Fi settings", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("WifiSettings", "API Error: " + error.toString());
                    Toast.makeText(wifi_settings.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return requestBody.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        // Add request to queue
        requestQueue.add(stringRequest);
    }

    private void rebootRouter() {
        String rebootUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/xqsystem/reboot?client=web";

        StringRequest rebootRequest = new StringRequest(Request.Method.GET, rebootUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getInt("code") == 0) {
                            Toast.makeText(wifi_settings.this, "Router is rebooting...", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(wifi_settings.this, "Reboot command sent but may not have succeeded", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(wifi_settings.this, "Reboot command sent but response was invalid", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    // Even if we get an error, the reboot might have been triggered
                    Toast.makeText(wifi_settings.this, "Reboot command sent but connection was interrupted", Toast.LENGTH_LONG).show();
                });

        requestQueue.add(rebootRequest);
    }

    @Override
    public void onBackPressed() {
        if (dialogEncryption.getVisibility() == View.VISIBLE) {
            dialogEncryption.setVisibility(View.GONE);
            return;
        }

        if (dialogSignalStrength.getVisibility() == View.VISIBLE) {
            dialogSignalStrength.setVisibility(View.GONE);
            return;
        }

        if (dialogChannel.getVisibility() == View.VISIBLE) {
            dialogChannel.setVisibility(View.GONE);
            return;
        }

        if (dialogBandwidth.getVisibility() == View.VISIBLE) {
            dialogBandwidth.setVisibility(View.GONE);
            return;
        }

        super.onBackPressed();
    }
}