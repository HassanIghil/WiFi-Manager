package com.example.wifimanager.Settings_Components;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;

public class pass_reset extends AppCompatActivity {

    private EditText newPasswordInput;
    private EditText confirmPasswordInput;
    private Button okButton;
    private ImageButton togglePasswordVisibility;
    private ImageButton toggleConfirmVisibility;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pass_reset);

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
        initializeViews();

        // Set up password visibility toggle
        setupPasswordVisibilityToggles();

        // Set up text watchers to validate input and enable/disable OK button
        setupTextWatchers();

        // Set up OK button click listener
        okButton.setOnClickListener(v -> {
            if (validatePasswords()) {
                // Save the new password - You'll need to implement this
                saveNewPassword();
                // Show success message
                Toast.makeText(pass_reset.this, "Password reset successfully", Toast.LENGTH_SHORT).show();
                // Navigate back
                finish();
            }
        });
    }

    private void initializeViews() {
        newPasswordInput = findViewById(R.id.new_password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        okButton = findViewById(R.id.ok_button);
        togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        toggleConfirmVisibility = findViewById(R.id.toggle_confirm_visibility);
    }

    private void setupPasswordVisibilityToggles() {
        togglePasswordVisibility.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            updatePasswordVisibility(newPasswordInput, togglePasswordVisibility, passwordVisible);
        });

        toggleConfirmVisibility.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            updatePasswordVisibility(confirmPasswordInput, toggleConfirmVisibility, confirmPasswordVisible);
        });
    }

    private void updatePasswordVisibility(EditText editText, ImageButton toggleButton, boolean isVisible) {
        if (isVisible) {
            // Show password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.eye_open);
        } else {
            // Hide password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.eye_closed);
        }
        // Keep cursor at the end of text
        editText.setSelection(editText.getText().length());
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
        };

        newPasswordInput.addTextChangedListener(textWatcher);
        confirmPasswordInput.addTextChangedListener(textWatcher);
    }

    private void validateInputs() {
        String password = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        boolean isValid = password.length() >= 8 && password.equals(confirmPassword) && !password.isEmpty();
        okButton.setEnabled(isValid);

        // Change button background color based on validity
        if (isValid) {
            okButton.setBackgroundResource(R.drawable.rounded_button_bg_dark_blue); // Use dark blue background when valid
        } else {
            okButton.setBackgroundResource(R.drawable.rounded_button_bg); // Use default background when invalid
        }
    }

    private boolean validatePasswords() {
        String password = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveNewPassword() {
        // Implement your password saving logic here
        // This could involve SharedPreferences, database, or API calls
        String newPassword = newPasswordInput.getText().toString();

        // Example using SharedPreferences:
        // SharedPreferences preferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        // SharedPreferences.Editor editor = preferences.edit();
        // editor.putString("password", newPassword);
        // editor.apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}