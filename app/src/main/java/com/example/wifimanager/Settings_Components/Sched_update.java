package com.example.wifimanager.Settings_Components;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class Sched_update extends AppCompatActivity {
    private MaterialSwitch scheduleSwitch;
    private RelativeLayout schedulerLayout;
    private TextView schedTimeText;
    private TextView schedTimeInfoText;
    private DatabaseHelper dbHelper;

    private static final String TABLE_NAME = DatabaseHelper.TABLE_PREFERENCES;
    private static final String KEY_IS_SCHEDULED = "is_scheduled";
    private static final String KEY_SCHEDULE_HOUR = "schedule_hour";
    private static final String KEY_SCHEDULE_MINUTE = "schedule_minute";
    private static final String KEY_SCHEDULE_AM_PM = "schedule_am_pm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sched_update);

        // Initialize database helper
        dbHelper = DatabaseHelper.getInstance(this);

        // Set up toolbar with back arrow
        setupToolbar();

        // Initialize views
        scheduleSwitch = findViewById(R.id.material_switch);
        schedulerLayout = findViewById(R.id.schedluer);
        schedTimeText = findViewById(R.id.sched_text);
        schedTimeInfoText = findViewById(R.id.sched_time_info);

        // Load saved preferences
        boolean isScheduled = dbHelper.getBoolean(TABLE_NAME, KEY_IS_SCHEDULED, false);
        int savedHour = dbHelper.getInt(TABLE_NAME, KEY_SCHEDULE_HOUR, 4);
        int savedMinute = dbHelper.getInt(TABLE_NAME, KEY_SCHEDULE_MINUTE, 0);
        String savedAmPm = dbHelper.getString(TABLE_NAME, KEY_SCHEDULE_AM_PM, "AM");

        // Set initial state
        scheduleSwitch.setChecked(isScheduled);
        updateScheduleTimeText(savedHour, savedMinute, savedAmPm);

        // Set up click listeners
        scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dbHelper.putBoolean(TABLE_NAME, KEY_IS_SCHEDULED, isChecked);
        });

        schedulerLayout.setOnClickListener(v -> {
            if (scheduleSwitch.isChecked()) {
                showCustomTimePickerDialog();
            } else {
                Snackbar.make(v, "Please enable scheduled updates first", Snackbar.LENGTH_SHORT).show();
            }
        });

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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

    private void showCustomTimePickerDialog() {
        // Get current values from database or default to 4:00 AM
        int hour = dbHelper.getInt(TABLE_NAME, KEY_SCHEDULE_HOUR, 4);
        int minute = dbHelper.getInt(TABLE_NAME, KEY_SCHEDULE_MINUTE, 0);
        String amPm = dbHelper.getString(TABLE_NAME, KEY_SCHEDULE_AM_PM, "AM");

        // Create custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_time_picker_dialog);

        // Make dialog background transparent and round corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }

        // Initialize pickers
        NumberPicker hourPicker = dialog.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = dialog.findViewById(R.id.minutePicker);
        NumberPicker amPmPicker = dialog.findViewById(R.id.amPmPicker);

        // Set up hour picker (1-12)
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour));

        // Set up minute picker (0-59)
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(minute);
        minutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

        // Set up AM/PM picker
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setValue(amPm.equals("PM") ? 1 : 0);

        // Set up buttons
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button okButton = dialog.findViewById(R.id.okButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        okButton.setOnClickListener(v -> {
            int selectedHour = hourPicker.getValue();
            int selectedMinute = minutePicker.getValue();
            String selectedAmPm = amPmPicker.getValue() == 0 ? "AM" : "PM";

            // Convert to 24-hour format for storage
            if (selectedAmPm.equals("PM") && selectedHour != 12) {
                selectedHour += 12;
            } else if (selectedAmPm.equals("AM") && selectedHour == 12) {
                selectedHour = 0;
            }

            // Save to database
            dbHelper.putInt(TABLE_NAME, KEY_SCHEDULE_HOUR, selectedHour);
            dbHelper.putInt(TABLE_NAME, KEY_SCHEDULE_MINUTE, selectedMinute);
            dbHelper.putString(TABLE_NAME, KEY_SCHEDULE_AM_PM, selectedAmPm);

            // Update UI
            int displayHour = selectedHour % 12;
            if (displayHour == 0) displayHour = 12;
            updateScheduleTimeText(displayHour, selectedMinute, selectedAmPm);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateScheduleTimeText(int hour, int minute, String amPm) {
        String timeText = String.format(Locale.getDefault(), "%d:%02d %s", hour, minute, amPm);
        schedTimeText.setText(timeText);
        schedTimeInfoText.setText(String.format("The router will check for updates at %s", timeText));
    }
}