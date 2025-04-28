package com.example.wifimanager.Settings_Components;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class FactoryResetActivity extends AppCompatActivity {

    private Button resetButton;
    private Button cancelButton;
    private CheckBox confirmationCheckbox;
    private TextInputEditText confirmPinEditText;
    private TextInputLayout confirmPinLayout;

    // Replace with your actual PIN checking logic
    private static final String SECURITY_PIN = "123456";
    private static final String PREFS_NAME = "WifiManagerPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factory_reset);

        // Set up toolbar with back arrow
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

        // Initialize views
        resetButton = findViewById(R.id.resetButton);
        cancelButton = findViewById(R.id.cancelButton);
        confirmationCheckbox = findViewById(R.id.confirmationCheckbox);
        confirmPinEditText = findViewById(R.id.confirmPinEditText);
        confirmPinLayout = findViewById(R.id.confirmPinLayout);

        // Set up listeners
        setupListeners();
    }

    private void setupListeners() {
        // Cancel button listener
        cancelButton.setOnClickListener(v -> finish());

        // Reset button listener
        resetButton.setOnClickListener(v -> showFinalConfirmationDialog());

        // Enable/disable reset button based on checkbox and PIN
        confirmationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            validateForm();
        });

        confirmPinEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateForm();
            }
        });
    }

    private void validateForm() {
        boolean isCheckboxChecked = confirmationCheckbox.isChecked();
        String enteredPin = confirmPinEditText.getText().toString().trim();

        // Only enable the reset button if checkbox is checked and PIN is valid
        resetButton.setEnabled(isCheckboxChecked && isPinValid(enteredPin));

        // Show error if PIN is invalid
        if (!enteredPin.isEmpty() && !isPinValid(enteredPin)) {
            confirmPinLayout.setError("Invalid PIN");
        } else {
            confirmPinLayout.setError(null);
        }
    }

    private boolean isPinValid(String pin) {
        // In a real app, you would validate against the user's stored PIN
        // This is just a placeholder example
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String storedPin = settings.getString("security_pin", SECURITY_PIN);
        return pin.equals(storedPin);
    }

    private void showFinalConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Factory Reset")
                .setMessage("Are you absolutely sure you want to reset this app to factory settings? All saved networks and settings will be permanently lost.")
                .setPositiveButton("Reset Now", (dialog, which) -> {
                    performFactoryReset();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performFactoryReset() {
        // Clear all shared preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();

        // Here you would also clear any other storage like databases
        // For example:
        // getApplicationContext().deleteDatabase("your_database_name.db");

        // Show success message
        Toast.makeText(this, "Factory reset completed successfully", Toast.LENGTH_LONG).show();

        // Close this activity and any activities in the stack
        finishAffinity();

        // Optionally restart the app
        // Intent intent = new Intent(this, MainActivity.class);
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}