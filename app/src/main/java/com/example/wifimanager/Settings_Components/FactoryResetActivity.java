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
import com.example.wifimanager.database.DatabaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class FactoryResetActivity extends AppCompatActivity {
    private Button resetButton;
    private Button cancelButton;
    private CheckBox confirmationCheckbox;
    private TextInputEditText confirmPinEditText;
    private TextInputLayout confirmPinLayout;
    private DatabaseHelper dbHelper;

    private static final String TABLE_NAME = DatabaseHelper.TABLE_PREFERENCES;
    private static final String TABLE_LOGIN = "MyWiFiApp"; // SharedPreferences name for login data
    private static final String KEY_LOGIN_PASSWORD = "login_password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factory_reset);

        dbHelper = DatabaseHelper.getInstance(this);

        // Initialize UI components
        resetButton = findViewById(R.id.resetButton);
        cancelButton = findViewById(R.id.cancelButton);
        confirmationCheckbox = findViewById(R.id.confirmationCheckbox);
        confirmPinEditText = findViewById(R.id.confirmPinEditText);
        confirmPinLayout = findViewById(R.id.confirmPinLayout);

        setupToolbar();
        setupListeners();
        setupWindowInsets();
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

    private void setupListeners() {
        resetButton.setEnabled(false);
        cancelButton.setOnClickListener(v -> finish());

        confirmationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            validateForm();
        });

        resetButton.setOnClickListener(v -> showFinalConfirmationDialog());

        confirmPinEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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
        SharedPreferences sharedPreferences = getSharedPreferences(TABLE_LOGIN, MODE_PRIVATE);
        String storedPassword = sharedPreferences.getString(KEY_LOGIN_PASSWORD, "");
        return !storedPassword.isEmpty() && pin.equals(storedPassword);
    }

    private void showFinalConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Factory Reset")
                .setMessage("This action will reset all settings to their default values. This cannot be undone. Are you sure you want to continue?")
                .setPositiveButton("Reset", (dialog, which) -> performFactoryReset())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performFactoryReset() {
        // Perform the actual factory reset operations here
        dbHelper.clearTable(TABLE_NAME);

        // Show success message
        Toast.makeText(this, "Factory reset completed", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}