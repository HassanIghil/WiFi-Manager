package com.example.wifimanager.Settings_Components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.button.MaterialButton;
import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class network_settings extends AppCompatActivity {

    private TextView currentModeText;
    private ConstraintLayout connectionModeDialog;
    private RadioGroup modeRadioGroup;
    private TextView ipAddress, gatewayAddress;
    private LinearLayout settingsContainer;
    private View currentSettingsView;
    private MaterialButton saveButton;
    private boolean hasUnsavedChanges = false;
    private DatabaseHelper dbHelper;

    private String stok; // Router API authentication token
    private RequestQueue requestQueue;
    private static final String ROUTER_URL = "http://192.168.31.1";
    private static final String API_BASE = "/cgi-bin/luci/;stok=%s/api/";

    // Database table name
    private static final String TABLE_NAME = DatabaseHelper.TABLE_NETWORK_SETTINGS;

    // Keys for database
    private static final String KEY_CONNECTION_MODE = "connection_mode";
    private static final String KEY_IP_ADDRESS = "ip_address";
    private static final String KEY_GATEWAY = "gateway";
    private static final String KEY_DNS1 = "dns1";
    private static final String KEY_DNS2 = "dns2";
    private static final String KEY_SUBNET = "subnet_mask";
    private static final String KEY_PPPOE_USERNAME = "pppoe_username";
    private static final String KEY_PPPOE_PASSWORD = "pppoe_password";
    private static final String KEY_PPPOE_SERVICE = "pppoe_service";
    private static final String KEY_MTU = "mtu";
    private static final String KEY_DHCP_IP = "dhcp_ip";
    private static final String KEY_DHCP_GATEWAY = "dhcp_gateway";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_settings);

        // Get STOK from intent
        stok = getIntent().getStringExtra("STOK");
        if (stok == null || stok.isEmpty()) {
            Toast.makeText(this, "Authentication token missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
        dbHelper = DatabaseHelper.getInstance(this);

        initializeViews();
        setupToolbar();
        setupConnectionModeDialog();
        
        // Fetch current settings from router before loading saved settings
        fetchCurrentNetworkSettings();
        
        loadSavedSettings();
        setupModeSpecificSettings();
        setupSaveButton();
    }

    private void initializeViews() {
        currentModeText = findViewById(R.id.current_mode_text);
        connectionModeDialog = findViewById(R.id.connection_mode_dialog);
        modeRadioGroup = findViewById(R.id.mode_radio_group);
        ipAddress = findViewById(R.id.ip_address);
        gatewayAddress = findViewById(R.id.gateway_address);
        settingsContainer = findViewById(R.id.settings_container);
        saveButton = findViewById(R.id.btn_save);
        saveButton.setEnabled(false); // Initially disabled until changes are made

        // Load saved network info
        String savedIp = dbHelper.getString(TABLE_NAME, KEY_IP_ADDRESS, "192.168.1.23");
        String savedGateway = dbHelper.getString(TABLE_NAME, KEY_GATEWAY, "192.168.1.1");
        ipAddress.setText(savedIp);
        gatewayAddress.setText(savedGateway);

        // Setup current mode selector click listener
        LinearLayout currentModeSelector = findViewById(R.id.current_mode_selector);
        currentModeSelector.setOnClickListener(v -> showConnectionModeDialog());

        // Close dialog when clicking outside
        connectionModeDialog.setOnClickListener(v -> hideConnectionModeDialog());

        // Prevent clicks on the dialog content from closing the dialog
        connectionModeDialog.findViewById(R.id.mode_radio_group).setOnClickListener(v -> {
            // Do nothing, prevent click from propagating
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

    private void setupConnectionModeDialog() {
        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnOk = findViewById(R.id.btn_ok);

        // Setup dialog buttons
        btnCancel.setOnClickListener(v -> hideConnectionModeDialog());
        btnOk.setOnClickListener(v -> {
            // Update IP display if in Static IP mode
            if (modeRadioGroup.getCheckedRadioButtonId() == R.id.radio_static) {
                EditText ipInput = currentSettingsView.findViewById(R.id.ip_input);
                EditText gatewayInput = currentSettingsView.findViewById(R.id.gateway_input);
                if (ipInput != null && gatewayInput != null) {
                    ipAddress.setText(ipInput.getText().toString());
                    gatewayAddress.setText(gatewayInput.getText().toString());
                }
            }
            updateSelectedMode();
            hideConnectionModeDialog();
            updateSettingsView();
        });
    }

    private void loadSavedSettings() {
        String savedMode = dbHelper.getString(TABLE_NAME, KEY_CONNECTION_MODE, "Dynamic IP Internet");
        currentModeText.setText(savedMode);

        // Load saved IP and Gateway based on the saved mode
        if (savedMode.equals("Dynamic IP Internet")) {
            String savedDhcpIp = dbHelper.getString(TABLE_NAME, KEY_DHCP_IP, "192.168.1.23");
            String savedDhcpGateway = dbHelper.getString(TABLE_NAME, KEY_DHCP_GATEWAY, "192.168.1.1");
            ipAddress.setText(savedDhcpIp);
            gatewayAddress.setText(savedDhcpGateway);
        } else if (savedMode.equals("Static IP")) {
            String savedStaticIp = dbHelper.getString(TABLE_NAME, KEY_IP_ADDRESS, "");
            String savedStaticGateway = dbHelper.getString(TABLE_NAME, KEY_GATEWAY, "");
            ipAddress.setText(savedStaticIp);
            gatewayAddress.setText(savedStaticGateway);
        }

        // Set the correct radio button based on saved mode
        if (savedMode.equals("Static IP")) {
            modeRadioGroup.check(R.id.radio_static);
        } else if (savedMode.equals("PPPoE")) {
            modeRadioGroup.check(R.id.radio_pppoe);
        } else {
            modeRadioGroup.check(R.id.radio_dhcp);
        }
    }

    private void setupModeSpecificSettings() {
        // Initial setup with saved mode
        updateSettingsView();
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void showConnectionModeDialog() {
        connectionModeDialog.setVisibility(View.VISIBLE);
    }

    private void hideConnectionModeDialog() {
        connectionModeDialog.setVisibility(View.GONE);
    }

    private void updateSelectedMode() {
        int selectedId = modeRadioGroup.getCheckedRadioButtonId();
        String previousMode = currentModeText.getText().toString();
        String newMode;

        if (selectedId == R.id.radio_dhcp) {
            newMode = "Dynamic IP Internet";
            // Set default DHCP values
            ipAddress.setText("192.168.1.23");
            gatewayAddress.setText("192.168.1.1");
        } else if (selectedId == R.id.radio_static) {
            newMode = "Static IP";
            // Don't update IP/Gateway here - will be updated when OK is clicked
        } else if (selectedId == R.id.radio_pppoe) {
            newMode = "PPPoE";
        } else {
            return;
        }

        if (!previousMode.equals(newMode)) {
            currentModeText.setText(newMode);
            markUnsavedChanges();
        }
    }

    private void updateSettingsView() {
        if (currentSettingsView != null) {
            settingsContainer.removeView(currentSettingsView);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        int layoutResId;

        int selectedId = modeRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_dhcp) {
            layoutResId = R.layout.layout_dhcp_settings;
        } else if (selectedId == R.id.radio_static) {
            layoutResId = R.layout.layout_static_ip_settings;
        } else {
            layoutResId = R.layout.layout_pppoe_settings;
        }

        currentSettingsView = inflater.inflate(layoutResId, settingsContainer, false);
        settingsContainer.addView(currentSettingsView);

        // Load saved values into the new view
        loadValuesIntoCurrentView();
        setupModeSpecificListeners();
    }

    private void loadValuesIntoCurrentView() {
        int selectedId = modeRadioGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_static) {
            EditText ipInput = currentSettingsView.findViewById(R.id.ip_input);
            EditText subnetInput = currentSettingsView.findViewById(R.id.subnet_input);
            EditText gatewayInput = currentSettingsView.findViewById(R.id.gateway_input);
            EditText dns1Input = currentSettingsView.findViewById(R.id.dns1_input);
            EditText dns2Input = currentSettingsView.findViewById(R.id.dns2_input);

            // Load saved values into the form fields
            ipInput.setText(dbHelper.getString(TABLE_NAME, KEY_IP_ADDRESS, ""));
            subnetInput.setText(dbHelper.getString(TABLE_NAME, KEY_SUBNET, "255.255.255.0"));
            gatewayInput.setText(dbHelper.getString(TABLE_NAME, KEY_GATEWAY, ""));
            dns1Input.setText(dbHelper.getString(TABLE_NAME, KEY_DNS1, ""));
            dns2Input.setText(dbHelper.getString(TABLE_NAME, KEY_DNS2, ""));

            // Update display only if we have saved values
            String savedIp = dbHelper.getString(TABLE_NAME, KEY_IP_ADDRESS, "");
            String savedGateway = dbHelper.getString(TABLE_NAME, KEY_GATEWAY, "");
            if (!savedIp.isEmpty() && !savedGateway.isEmpty()) {
                ipAddress.setText(savedIp);
                gatewayAddress.setText(savedGateway);
            }
        } else if (selectedId == R.id.radio_pppoe) {
            EditText usernameInput = currentSettingsView.findViewById(R.id.account_input);
            EditText passwordInput = currentSettingsView.findViewById(R.id.password_input);
            EditText serviceNameInput = currentSettingsView.findViewById(R.id.service_name_input);
            EditText mtuInput = currentSettingsView.findViewById(R.id.mtu_input);

            usernameInput.setText(dbHelper.getString(TABLE_NAME, KEY_PPPOE_USERNAME, ""));
            passwordInput.setText(dbHelper.getString(TABLE_NAME, KEY_PPPOE_PASSWORD, ""));
            serviceNameInput.setText(dbHelper.getString(TABLE_NAME, KEY_PPPOE_SERVICE, ""));
            mtuInput.setText(dbHelper.getString(TABLE_NAME, KEY_MTU, "1480"));
        }
    }

    private void saveChanges() {
        // Validate fields before saving
        if (!validatePppoeFields() && !validateStaticIpFields()) {
            return;
        }

        int selectedId = modeRadioGroup.getCheckedRadioButtonId();

        // Save connection mode
        dbHelper.putString(TABLE_NAME, KEY_CONNECTION_MODE, currentModeText.getText().toString());

        if (selectedId == R.id.radio_dhcp) {
            // Save current DHCP values
            dbHelper.putString(TABLE_NAME, KEY_DHCP_IP, ipAddress.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_DHCP_GATEWAY, gatewayAddress.getText().toString());
        } else if (selectedId == R.id.radio_static) {
            EditText ipInput = currentSettingsView.findViewById(R.id.ip_input);
            EditText subnetInput = currentSettingsView.findViewById(R.id.subnet_input);
            EditText gatewayInput = currentSettingsView.findViewById(R.id.gateway_input);
            EditText dns1Input = currentSettingsView.findViewById(R.id.dns1_input);
            EditText dns2Input = currentSettingsView.findViewById(R.id.dns2_input);

            dbHelper.putString(TABLE_NAME, KEY_IP_ADDRESS, ipInput.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_SUBNET, subnetInput.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_GATEWAY, gatewayInput.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_DNS1, dns1Input.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_DNS2, dns2Input.getText().toString());

            // Update the display values
            ipAddress.setText(ipInput.getText().toString());
            gatewayAddress.setText(gatewayInput.getText().toString());
        } else if (selectedId == R.id.radio_pppoe) {
            EditText usernameInput = currentSettingsView.findViewById(R.id.account_input);
            EditText passwordInput = currentSettingsView.findViewById(R.id.password_input);
            EditText serviceNameInput = currentSettingsView.findViewById(R.id.service_name_input);
            EditText mtuInput = currentSettingsView.findViewById(R.id.mtu_input);

            dbHelper.putString(TABLE_NAME, KEY_PPPOE_USERNAME, usernameInput.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_PPPOE_PASSWORD, passwordInput.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_PPPOE_SERVICE, serviceNameInput.getText().toString());
            dbHelper.putString(TABLE_NAME, KEY_MTU, mtuInput.getText().toString());
        }

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        hasUnsavedChanges = false;
        saveButton.setEnabled(false);
    }

    private void setupModeSpecificListeners() {
        int selectedId = modeRadioGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_dhcp) {
            setupDhcpListeners();
        } else if (selectedId == R.id.radio_static) {
            setupStaticIpListeners();
        } else if (selectedId == R.id.radio_pppoe) {
            setupPppoeListeners();
        }

        // Add text change listeners to mark unsaved changes
        addTextChangeListeners(currentSettingsView);
    }

    private void setupDhcpListeners() {
        RadioGroup dnsConfigGroup = currentSettingsView.findViewById(R.id.dns_config_group);
        View manualDnsContainer = currentSettingsView.findViewById(R.id.manual_dns_container);

        if (dnsConfigGroup != null && manualDnsContainer != null) {
            dnsConfigGroup.setOnCheckedChangeListener((group, checkedId) -> {
                manualDnsContainer.setVisibility(
                    checkedId == R.id.radio_manual_dns ? View.VISIBLE : View.GONE
                );
            });
        }
    }

    private void setupStaticIpListeners() {
        // Remove real-time update listeners since we want to update only on OK click
        // The fields will be validated and updated when OK is clicked
    }

    private void setupPppoeListeners() {
        RadioGroup dnsConfigGroup = currentSettingsView.findViewById(R.id.dns_config_group);
        View otherSettingsContainer = currentSettingsView.findViewById(R.id.other_settings_container);
        
        // Get individual field references
        CheckBox specialIspMode = currentSettingsView.findViewById(R.id.special_isp_mode);
        EditText mtuInput = currentSettingsView.findViewById(R.id.mtu_input);
        EditText serviceNameInput = currentSettingsView.findViewById(R.id.service_name_input);
        EditText dns1Input = currentSettingsView.findViewById(R.id.dns1_input);
        EditText dns2Input = currentSettingsView.findViewById(R.id.dns2_input);

        if (dnsConfigGroup != null) {
            boolean isManual = dnsConfigGroup.getCheckedRadioButtonId() == R.id.radio_manual_configure;
            
            // Set initial state of fields
            setOtherFieldsEnabled(isManual, specialIspMode, mtuInput, serviceNameInput);
            setDnsFieldsEnabled(isManual, dns1Input, dns2Input);

            dnsConfigGroup.setOnCheckedChangeListener((group, checkedId) -> {
                boolean isManualConfig = checkedId == R.id.radio_manual_configure;
                
                // Enable/disable other fields
                setOtherFieldsEnabled(isManualConfig, specialIspMode, mtuInput, serviceNameInput);
                setDnsFieldsEnabled(isManualConfig, dns1Input, dns2Input);
                
                if (!isManualConfig) {
                    // Clear all fields when switching to auto
                    clearFields(specialIspMode, mtuInput, serviceNameInput, dns1Input, dns2Input);
                }
                
                markUnsavedChanges();
            });
        }
    }

    private void setOtherFieldsEnabled(boolean enabled, CheckBox specialIspMode, EditText mtuInput, EditText serviceNameInput) {
        specialIspMode.setEnabled(enabled);
        mtuInput.setEnabled(enabled);
        serviceNameInput.setEnabled(enabled);
    }

    private void setDnsFieldsEnabled(boolean enabled, EditText dns1Input, EditText dns2Input) {
        dns1Input.setEnabled(enabled);
        dns2Input.setEnabled(enabled);
    }

    private void clearFields(CheckBox specialIspMode, EditText... fields) {
        specialIspMode.setChecked(false);
        for (EditText field : fields) {
            if (field.getId() == R.id.mtu_input) {
                field.setText("1480"); // Reset MTU to default value
            } else {
                field.setText("");
            }
        }
    }

    private boolean validatePppoeFields() {
        if (modeRadioGroup.getCheckedRadioButtonId() != R.id.radio_pppoe) {
            return true; // Not in PPPoE mode, skip validation
        }

        EditText accountInput = currentSettingsView.findViewById(R.id.account_input);
        EditText passwordInput = currentSettingsView.findViewById(R.id.password_input);
        EditText dns1Input = currentSettingsView.findViewById(R.id.dns1_input);
        EditText dns2Input = currentSettingsView.findViewById(R.id.dns2_input);
        RadioGroup dnsConfigGroup = currentSettingsView.findViewById(R.id.dns_config_group);

        String account = accountInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (account.isEmpty()) {
            Toast.makeText(this, "Please enter your account", Toast.LENGTH_SHORT).show();
            accountInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            passwordInput.requestFocus();
            return false;
        }

        // Check DNS fields only if in manual mode
        if (dnsConfigGroup.getCheckedRadioButtonId() == R.id.radio_manual_configure) {
            String dns1 = dns1Input.getText().toString().trim();
            String dns2 = dns2Input.getText().toString().trim();

            if (dns1.isEmpty()) {
                Toast.makeText(this, "Please enter DNS1 server", Toast.LENGTH_SHORT).show();
                dns1Input.requestFocus();
                return false;
            }

            // DNS2 is optional, so we don't validate it
        }

        return true;
    }

    private boolean validateStaticIpFields() {
        if (modeRadioGroup.getCheckedRadioButtonId() != R.id.radio_static) {
            return true; // Not in Static IP mode, skip validation
        }

        EditText ipInput = currentSettingsView.findViewById(R.id.ip_input);
        EditText subnetInput = currentSettingsView.findViewById(R.id.subnet_input);
        EditText gatewayInput = currentSettingsView.findViewById(R.id.gateway_input);
        EditText dns1Input = currentSettingsView.findViewById(R.id.dns1_input);

        String ip = ipInput.getText().toString().trim();
        String subnet = subnetInput.getText().toString().trim();
        String gateway = gatewayInput.getText().toString().trim();
        String dns1 = dns1Input.getText().toString().trim();

        if (ip.isEmpty() || !isValidIpAddress(ip)) {
            Toast.makeText(this, "Please enter a valid IP address", Toast.LENGTH_SHORT).show();
            ipInput.requestFocus();
            return false;
        }

        if (subnet.isEmpty() || !isValidIpAddress(subnet)) {
            Toast.makeText(this, "Please enter a valid subnet mask", Toast.LENGTH_SHORT).show();
            subnetInput.requestFocus();
            return false;
        }

        if (gateway.isEmpty() || !isValidIpAddress(gateway)) {
            Toast.makeText(this, "Please enter a valid gateway address", Toast.LENGTH_SHORT).show();
            gatewayInput.requestFocus();
            return false;
        }

        if (dns1.isEmpty() || !isValidIpAddress(dns1)) {
            Toast.makeText(this, "Please enter a valid DNS1 server", Toast.LENGTH_SHORT).show();
            dns1Input.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidIpAddress(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setFieldsEnabled(View container, boolean enabled) {
        if (container instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) container;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof EditText) {
                    child.setEnabled(enabled);
                    child.setFocusable(enabled);
                    child.setFocusableInTouchMode(enabled);
                    if (!enabled) {
                        ((EditText) child).setText(""); // Clear text when disabled
                    }
                } else if (child instanceof CheckBox) {
                    child.setEnabled(enabled);
                    if (!enabled) {
                        ((CheckBox) child).setChecked(false);
                    }
                } else if (child instanceof ViewGroup) {
                    setFieldsEnabled(child, enabled);
                }
            }
        }
    }

    private void addTextChangeListeners(View view) {
        if (view instanceof EditText) {
            ((EditText) view).addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    markUnsavedChanges();
                }
            });
        } else if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            for (int i = 0; i < layout.getChildCount(); i++) {
                addTextChangeListeners(layout.getChildAt(i));
            }
        }
    }

    private void markUnsavedChanges() {
        hasUnsavedChanges = true;
        saveButton.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            // Show confirmation dialog if there are unsaved changes
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showUnsavedChangesDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save them before leaving?")
            .setPositiveButton("Save", (dialog, which) -> {
                saveChanges();
                finish();
            })
            .setNegativeButton("Discard", (dialog, which) -> finish())
            .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void applyNetworkSettings() {
        if (stok == null || stok.isEmpty()) {
            Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
            return;
        }

        String url = String.format("%s/cgi-bin/luci/;stok=%s/api/xqnetwork/set_wan", ROUTER_URL, stok);
        
        // Prepare parameters based on selected mode
        Map<String, String> params = new HashMap<>();
        int selectedId = modeRadioGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_dhcp) {
            params.put("proto", "dhcp");
        } else if (selectedId == R.id.radio_static) {
            EditText ipInput = currentSettingsView.findViewById(R.id.ip_input);
            EditText subnetInput = currentSettingsView.findViewById(R.id.subnet_input);
            EditText gatewayInput = currentSettingsView.findViewById(R.id.gateway_input);
            EditText dns1Input = currentSettingsView.findViewById(R.id.dns1_input);
            EditText dns2Input = currentSettingsView.findViewById(R.id.dns2_input);

            params.put("proto", "static");
            params.put("ipaddr", ipInput.getText().toString());
            params.put("netmask", subnetInput.getText().toString());
            params.put("gateway", gatewayInput.getText().toString());
            
            String dns = dns1Input.getText().toString();
            if (!dns2Input.getText().toString().isEmpty()) {
                dns += "," + dns2Input.getText().toString();
            }
            params.put("dns", dns);
        } else if (selectedId == R.id.radio_pppoe) {
            EditText usernameInput = currentSettingsView.findViewById(R.id.account_input);
            EditText passwordInput = currentSettingsView.findViewById(R.id.password_input);
            EditText serviceNameInput = currentSettingsView.findViewById(R.id.service_name_input);
            EditText mtuInput = currentSettingsView.findViewById(R.id.mtu_input);
            EditText dns1Input = currentSettingsView.findViewById(R.id.dns1_input);
            EditText dns2Input = currentSettingsView.findViewById(R.id.dns2_input);
            RadioGroup dnsConfigGroup = currentSettingsView.findViewById(R.id.dns_config_group);

            params.put("proto", "pppoe");
            params.put("username", usernameInput.getText().toString());
            params.put("password", passwordInput.getText().toString());
            
            if (!serviceNameInput.getText().toString().isEmpty()) {
                params.put("service", serviceNameInput.getText().toString());
            }
            
            if (!mtuInput.getText().toString().isEmpty()) {
                params.put("mtu", mtuInput.getText().toString());
            }

            // Add DNS servers if manual configuration is selected
            if (dnsConfigGroup.getCheckedRadioButtonId() == R.id.radio_manual_configure) {
                String dns = dns1Input.getText().toString();
                if (!dns2Input.getText().toString().isEmpty()) {
                    dns += "," + dns2Input.getText().toString();
                }
                params.put("dns", dns);
            }
        }

        // Create the request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        int code = jsonResponse.getInt("code");
                        if (code == 0) {
                            Toast.makeText(this, "Network settings applied successfully", Toast.LENGTH_SHORT).show();
                            saveChanges(); // Save to local database after successful API call
                        } else {
                            String msg = jsonResponse.optString("msg", "Unknown error");
                            Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void fetchCurrentNetworkSettings() {
        if (stok == null || stok.isEmpty()) {
            Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
            return;
        }

        String url = String.format("%s/cgi-bin/luci/;stok=%s/api/xqnetwork/wan_info", ROUTER_URL, stok);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject info = response.getJSONObject("info");
                        
                        // Get IP address from ipv4 array
                        String fetchedIp = "";
                        JSONArray ipv4Array = info.getJSONArray("ipv4");
                        if (ipv4Array.length() > 0) {
                            JSONObject ipv4 = ipv4Array.getJSONObject(0);
                            fetchedIp = ipv4.getString("ip");
                            ipAddress.setText(fetchedIp);  // Set IP address in UI
                        }
                        
                        // Get gateway address
                        String fetchedGateway = info.getString("gateWay");
                        gatewayAddress.setText(fetchedGateway);  // Set gateway in UI
                        
                        // Get connection type and update radio buttons
                        JSONObject details = info.getJSONObject("details");
                        String wanType = details.getString("wanType");
                        
                        // Store the fetched values in database based on connection type
                        switch (wanType) {
                            case "dhcp":
                                modeRadioGroup.check(R.id.radio_dhcp);
                                currentModeText.setText("Dynamic IP Internet");
                                dbHelper.putString(TABLE_NAME, KEY_CONNECTION_MODE, "Dynamic IP Internet");
                                dbHelper.putString(TABLE_NAME, KEY_DHCP_IP, fetchedIp);
                                dbHelper.putString(TABLE_NAME, KEY_DHCP_GATEWAY, fetchedGateway);
                                break;
                            case "pppoe":
                                modeRadioGroup.check(R.id.radio_pppoe);
                                currentModeText.setText("PPPoE");
                                dbHelper.putString(TABLE_NAME, KEY_CONNECTION_MODE, "PPPoE");
                                // Store PPPoE specific IP and gateway
                                dbHelper.putString(TABLE_NAME, KEY_IP_ADDRESS, fetchedIp);
                                dbHelper.putString(TABLE_NAME, KEY_GATEWAY, fetchedGateway);
                                break;
                            case "static":
                                modeRadioGroup.check(R.id.radio_static);
                                currentModeText.setText("Static IP");
                                dbHelper.putString(TABLE_NAME, KEY_CONNECTION_MODE, "Static IP");
                                dbHelper.putString(TABLE_NAME, KEY_IP_ADDRESS, fetchedIp);
                                dbHelper.putString(TABLE_NAME, KEY_GATEWAY, fetchedGateway);
                                // Also store subnet mask if available
                                if (ipv4Array.length() > 0) {
                                    String mask = ipv4Array.getJSONObject(0).getString("mask");
                                    dbHelper.putString(TABLE_NAME, KEY_SUBNET, mask);
                                }
                                break;
                        }
                        
                        updateSettingsView();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing network settings", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error fetching network settings: " + error.getMessage(), 
                        Toast.LENGTH_LONG).show()
        );

        requestQueue.add(request);
    }
}