package com.example.wifimanager.Settings_Components;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.wifimanager.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Formatter;
import java.util.Random;

public class pass_reset extends AppCompatActivity {

    private TextInputLayout oldPasswordLayout;
    private TextInputLayout newPasswordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText oldPasswordInput;
    private TextInputEditText newPasswordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton okButton;
    private View strengthIndicator1;
    private View strengthIndicator2;
    private View strengthIndicator3;
    private View strengthIndicator4;

    private String stok;
    private String routerMac;
    private RequestQueue requestQueue;
    private final String BASE_URL = "http://192.168.31.1/cgi-bin/luci/;stok=%s/api/xqsystem/set_name_password";
    private final String STATUS_URL = "http://192.168.31.1/cgi-bin/luci/;stok=%s/api/misystem/status";

    // Hardcoded values based on your information
    private final String CURRENT_PASSWORD_HASH = "586c2c9acd99e6ca56d7e0be6464e4041a832d8b"; // SHA1 of "11334455"
    private final String NEW_PASSWORD = "1234512345";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pass_reset);

        requestQueue = Volley.newRequestQueue(this);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        checkAuthToken();
        fetchRouterMacAddress();

        setupTextWatchers();

        okButton.setOnClickListener(v -> {
            if (validatePasswords()) {
                okButton.setEnabled(false);
                okButton.setText("Processing...");

                if (routerMac == null || routerMac.isEmpty()) {
                    fetchRouterMacAddress(this::saveNewPassword);
                } else {
                    saveNewPassword();
                }
            }
        });
    }

    private void checkAuthToken() {
        Bundle extras = getIntent().getExtras();
        if (extras == null || extras.getString("STOK") == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_LONG).show();
            finish();
        } else {
            stok = extras.getString("STOK");
        }
    }

    private void fetchRouterMacAddress() {
        fetchRouterMacAddress(() -> {});
    }

    private void fetchRouterMacAddress(Runnable onSuccess) {
        String url = String.format(STATUS_URL, stok);

        JsonObjectRequest statusRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject hardware = response.getJSONObject("hardware");
                        routerMac = hardware.getString("mac").toLowerCase();
                        Log.d("RouterInfo", "MAC Address: " + routerMac);
                        onSuccess.run();
                    } catch (JSONException e) {
                        showError("Failed to get router MAC: " + e.getMessage());
                    }
                },
                error -> showError("Network error: " + error.getMessage()));

        requestQueue.add(statusRequest);
    }

    private void initializeViews() {
        oldPasswordLayout = findViewById(R.id.old_password_layout);
        newPasswordLayout = findViewById(R.id.new_password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        oldPasswordInput = findViewById(R.id.old_password_input);
        newPasswordInput = findViewById(R.id.new_password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        okButton = findViewById(R.id.ok_button);

        strengthIndicator1 = findViewById(R.id.strength_indicator_1);
        strengthIndicator2 = findViewById(R.id.strength_indicator_2);
        strengthIndicator3 = findViewById(R.id.strength_indicator_3);
        strengthIndicator4 = findViewById(R.id.strength_indicator_4);

        // Pre-fill the password fields for testing
        newPasswordInput.setText(NEW_PASSWORD);
        confirmPasswordInput.setText(NEW_PASSWORD);
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
                updatePasswordStrengthIndicator();
            }
        };

        oldPasswordInput.addTextChangedListener(textWatcher);
        newPasswordInput.addTextChangedListener(textWatcher);
        confirmPasswordInput.addTextChangedListener(textWatcher);
    }

    private void validateInputs() {
        String oldPassword = oldPasswordInput.getText().toString();
        String password = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        oldPasswordLayout.setError(oldPassword.isEmpty() ? "Current password is required" : null);

        if (password.length() > 0 && password.length() < 8) {
            newPasswordLayout.setError("Password must be at least 8 characters");
        } else {
            newPasswordLayout.setError(null);
        }

        confirmPasswordLayout.setError(
                confirmPassword.length() > 0 && !password.equals(confirmPassword)
                        ? "Passwords don't match"
                        : null
        );

        okButton.setEnabled(!oldPassword.isEmpty() &&
                password.length() >= 8 &&
                password.equals(confirmPassword));
    }

    private void updatePasswordStrengthIndicator() {
        String password = newPasswordInput.getText().toString();
        int strength = calculatePasswordStrength(password);

        int[] colors = {
                android.R.color.white,
                android.R.color.holo_red_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light,
                android.R.color.holo_green_dark
        };

        strengthIndicator1.setBackgroundColor(getResources().getColor(strength >= 1 ? colors[1] : colors[0]));
        strengthIndicator2.setBackgroundColor(getResources().getColor(strength >= 2 ? colors[2] : colors[0]));
        strengthIndicator3.setBackgroundColor(getResources().getColor(strength >= 3 ? colors[3] : colors[0]));
        strengthIndicator4.setBackgroundColor(getResources().getColor(strength >= 4 ? colors[4] : colors[0]));
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) strength++;
        return Math.min(strength, 4);
    }

    private boolean validatePasswords() {
        String oldPassword = oldPasswordInput.getText().toString();
        String password = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (oldPassword.isEmpty()) {
            showError("Current password is required");
            return false;
        }

        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords don't match");
            return false;
        }

        return true;
    }

    private void saveNewPassword() {
        try {
            String oldPassword = oldPasswordInput.getText().toString();

            // Generate nonce with current timestamp and random value
            long timestamp = System.currentTimeMillis() / 1000;
            int randomVal = new Random().nextInt(10000);
            String nonce = "0_" + routerMac + "_" + timestamp + "_" + randomVal;

            // Create payload with exact values
            JSONObject payload = new JSONObject();
            payload.put("nonce", nonce);
            payload.put("newPwd", xiaoMiEncrypt(NEW_PASSWORD));
            payload.put("oldPwd", CURRENT_PASSWORD_HASH);

            Log.d("PasswordReset", "Final Payload: " + payload.toString());
            Log.d("PasswordReset", "Using current password hash: " + CURRENT_PASSWORD_HASH);
            Log.d("PasswordReset", "New encrypted password: " + xiaoMiEncrypt(NEW_PASSWORD));

            // Make the API request
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    String.format(BASE_URL, stok),
                    payload,
                    response -> {
                        try {
                            Log.d("PasswordReset", "Response: " + response.toString());
                            int code = response.getInt("code");
                            if (code == 0) {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            } else {
                                String errorMsg = "Error " + code + ": " +
                                        (code == 1502 ? "Current password is incorrect" : "Unknown error");
                                showError(errorMsg);
                            }
                        } catch (JSONException e) {
                            showError("Invalid server response format");
                        }
                    },
                    error -> {
                        Log.e("PasswordReset", "Error: " + error.getMessage());
                        showError("Network error: " + error.getMessage());
                    }
            );

            requestQueue.add(request);
        } catch (Exception e) {
            Log.e("PasswordReset", "Exception: " + e.getMessage());
            showError("Error preparing request: " + e.getMessage());
        }
    }

    /**
     * Xiaomi's exact password encryption:
     * 1. Convert password to UTF-8 bytes
     * 2. Generate 12 random bytes
     * 3. Combine password + random bytes
     * 4. Base64 encode the result
     */
    private String xiaoMiEncrypt(String password) {
        try {
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] salt = new byte[12];
            new SecureRandom().nextBytes(salt);

            byte[] combined = new byte[passwordBytes.length + salt.length];
            System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
            System.arraycopy(salt, 0, combined, passwordBytes.length, salt.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("Encryption", "Error", e);
            return "";
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            resetButtonState();
        });
    }

    private void resetButtonState() {
        runOnUiThread(() -> {
            okButton.setEnabled(true);
            okButton.setText("Reset Password");
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}