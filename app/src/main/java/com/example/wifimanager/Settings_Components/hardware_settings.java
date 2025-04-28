package com.example.wifimanager.Settings_Components;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.wifimanager.R;
import com.example.wifimanager.Tools.Update;

public class hardware_settings extends AppCompatActivity {

    private String stok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hardware_settings);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            stok = extras.getString("STOK");
            if (stok == null || stok.isEmpty()) {
                Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
                finish(); // Close activity if no token
                return;
            }
        } else {
            Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
            finish(); // Close activity if no extras
            return;
        }

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

        // Set up click listeners for menu items
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Reset Password option
        LinearLayout resetPasswordOption = findViewById(R.id.reset_password_option);
        resetPasswordOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the reset password activity
                Intent intent = new Intent(hardware_settings.this, pass_reset.class);
                intent.putExtra("STOK", stok); // Add this line to pass the token
                startActivity(intent);
            }
        });


        LinearLayout updateOption = findViewById(R.id.update_option);
        updateOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle update option click
                Intent intent = new Intent(hardware_settings.this, Sched_update.class);
                intent.putExtra("STOK", stok); // Add this line to pass the token
                startActivity(intent);
            }
        });

        LinearLayout timeZoneOption = findViewById(R.id.time_zone_option);
        timeZoneOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle time zone option click
                Intent intent = new Intent(hardware_settings.this, TimeZoneSettingsActivity.class);
                intent.putExtra("STOK", stok); // Add this line to pass the token
                startActivity(intent);
            }
        });

        LinearLayout factoryResetOption = findViewById(R.id.factory_reset_option);
        factoryResetOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle factory reset option click
                Intent intent = new Intent(hardware_settings.this, FactoryResetActivity.class);
                intent.putExtra("STOK", stok); // Add this line to pass the token
                startActivity(intent);
            }
        });


        LinearLayout log_data = findViewById(R.id.log_data);
        log_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(hardware_settings.this, logs.class);
                intent.putExtra("STOK", stok); // Add this line to pass the token
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}