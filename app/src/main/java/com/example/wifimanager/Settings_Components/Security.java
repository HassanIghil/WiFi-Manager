package com.example.wifimanager.Settings_Components;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;

public class Security extends AppCompatActivity {
    private ImageView highLevelCheck, mediumLevelCheck, lowLevelCheck;
    private LinearLayout highLevelLayout, mediumLevelLayout, lowLevelLayout;
    private static final String TABLE_NAME = DatabaseHelper.TABLE_SECURITY;
    private static final String KEY_SECURITY_LEVEL = "security_level";
    private String currentLevel;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupToolbar();
        setClickListeners();
        loadSavedSecurityLevel();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        highLevelCheck = findViewById(R.id.high_level_check);
        mediumLevelCheck = findViewById(R.id.medium_level_check);
        lowLevelCheck = findViewById(R.id.low_level_check);
        highLevelLayout = findViewById(R.id.high_level_layout);
        mediumLevelLayout = findViewById(R.id.medium_level_layout);
        lowLevelLayout = findViewById(R.id.low_level_layout);
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

    private void setClickListeners() {
        highLevelLayout.setOnClickListener(v -> setSelectedLevel("high"));
        mediumLevelLayout.setOnClickListener(v -> setSelectedLevel("medium"));
        lowLevelLayout.setOnClickListener(v -> setSelectedLevel("low"));
    }

    private void loadSavedSecurityLevel() {
        currentLevel = dbHelper.getString(TABLE_NAME, KEY_SECURITY_LEVEL, "high");
        setSelectedLevel(currentLevel);
    }

    private void setSelectedLevel(String level) {
        highLevelCheck.setVisibility(View.INVISIBLE);
        mediumLevelCheck.setVisibility(View.INVISIBLE);
        lowLevelCheck.setVisibility(View.INVISIBLE);

        switch (level) {
            case "high":
                highLevelCheck.setVisibility(View.VISIBLE);
                currentLevel = "high";
                break;
            case "medium":
                mediumLevelCheck.setVisibility(View.VISIBLE);
                currentLevel = "medium";
                break;
            case "low":
                lowLevelCheck.setVisibility(View.VISIBLE);
                currentLevel = "low";
                break;
        }

        saveSecurityLevel(level);

        // Set result to be sent back to Firewall activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_level", level);
        setResult(RESULT_OK, resultIntent);
    }

    private void saveSecurityLevel(String level) {
        dbHelper.putString(TABLE_NAME, KEY_SECURITY_LEVEL, level);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}