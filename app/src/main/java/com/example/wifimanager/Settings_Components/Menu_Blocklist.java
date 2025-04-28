package com.example.wifimanager.Settings_Components;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;

public class Menu_Blocklist extends AppCompatActivity {
    private ImageView blocklistCheck, exceptionCheck;
    private LinearLayout blocktLayout, excepLayout;

    // SharedPreferences Constants
    private static final String PREFS_NAME = "block_pref";
    private static final String KEY_SECURITY_LEVEL = "block_method";

    // Security Level Constants
    private static final String LEVEL_BLOCKLIST = "blockList";
    private static final String LEVEL_EXCEPTION = "exception";

    private String currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_blocklist);

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
        blocktLayout = findViewById(R.id.blocklist_layout);
        excepLayout = findViewById(R.id.exception_layout);
        blocklistCheck = findViewById(R.id.blocklist_check);
        exceptionCheck = findViewById(R.id.on_the_exec_check);
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
        blocktLayout.setOnClickListener(v -> setSelectedLevel(LEVEL_BLOCKLIST));
        excepLayout.setOnClickListener(v -> setSelectedLevel(LEVEL_EXCEPTION));
    }

    private void loadSavedSecurityLevel() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentLevel = prefs.getString(KEY_SECURITY_LEVEL, LEVEL_BLOCKLIST); // Default is BLOCKLIST
        setSelectedLevel(currentLevel);
    }

    private void setSelectedLevel(String level) {
        // Hide both checkmarks first
        blocklistCheck.setVisibility(View.INVISIBLE);
        exceptionCheck.setVisibility(View.INVISIBLE);

        // Show the correct checkmark based on selection
        if (LEVEL_BLOCKLIST.equals(level)) {
            blocklistCheck.setVisibility(View.VISIBLE);
            currentLevel = LEVEL_BLOCKLIST;
        } else if (LEVEL_EXCEPTION.equals(level)) {
            exceptionCheck.setVisibility(View.VISIBLE);
            currentLevel = LEVEL_EXCEPTION;
        }

        saveSecurityLevel();
    }

    private void saveSecurityLevel() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_SECURITY_LEVEL, currentLevel)
                .apply();
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_method", currentLevel);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}