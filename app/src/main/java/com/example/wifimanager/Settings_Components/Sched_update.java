package com.example.wifimanager.Settings_Components;

import android.app.Dialog;
import android.content.SharedPreferences;
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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class Sched_update extends AppCompatActivity {

    private MaterialSwitch scheduleSwitch;
    private RelativeLayout schedulerLayout;
    private TextView schedTimeText;
    private TextView schedTimeInfoText;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UpdateSchedulerPrefs";
    private static final String IS_SCHEDULED_KEY = "isScheduled";
    private static final String SCHEDULE_HOUR_KEY = "scheduleHour";
    private static final String SCHEDULE_MINUTE_KEY = "scheduleMinute";
    private static final String SCHEDULE_AM_PM_KEY = "scheduleAmPm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sched_update);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Set up toolbar with back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize views
        scheduleSwitch = findViewById(R.id.material_switch);
        schedulerLayout = findViewById(R.id.schedluer);
        schedTimeText = findViewById(R.id.sched_text);
        schedTimeInfoText = findViewById(R.id.sched_time_info);

        // Load saved preferences
        boolean isScheduled = sharedPreferences.getBoolean(IS_SCHEDULED_KEY, false);
        int savedHour = sharedPreferences.getInt(SCHEDULE_HOUR_KEY, 4);
        int savedMinute = sharedPreferences.getInt(SCHEDULE_MINUTE_KEY, 0);
        String savedAmPm = sharedPreferences.getString(SCHEDULE_AM_PM_KEY, "AM");

        // Set initial state
        scheduleSwitch.setChecked(isScheduled);
        updateScheduleTimeText(savedHour, savedMinute, savedAmPm);

        // Set up click listeners
        scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(IS_SCHEDULED_KEY, isChecked);
            editor.apply();
        });

        schedulerLayout.setOnClickListener(v -> {
            if (scheduleSwitch.isChecked()) {
                showCustomTimePickerDialog();
            } else {
                Snackbar.make(v, "Please enable scheduled updates first", Snackbar.LENGTH_SHORT).show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showCustomTimePickerDialog() {
        // Get current values from preferences or default to 4:00 AM
        int hour = sharedPreferences.getInt(SCHEDULE_HOUR_KEY, 4);
        int minute = sharedPreferences.getInt(SCHEDULE_MINUTE_KEY, 0);
        String amPm = sharedPreferences.getString(SCHEDULE_AM_PM_KEY, "AM");

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
        hourPicker.setValue(hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour));

        // Set up minute picker (0-59)
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(minute);
        minutePicker.setFormatter(value -> String.format(Locale.US, "%02d", value)); // To display 01, 02, etc.

        // Set up AM/PM picker
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setValue(amPm.equals("AM") ? 0 : 1);

        // Highlight current values
        hourPicker.setValue(hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour));

        // Set up buttons
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button okButton = dialog.findViewById(R.id.okButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        okButton.setOnClickListener(v -> {
            // Convert selected time to 24-hour format for internal storage
            int selectedHour = hourPicker.getValue();
            int selectedMinute = minutePicker.getValue();
            String selectedAmPm = amPmPicker.getValue() == 0 ? "AM" : "PM";

            // Convert to 24-hour format for internal storage
            if (selectedAmPm.equals("PM") && selectedHour < 12) {
                selectedHour += 12;
            } else if (selectedAmPm.equals("AM") && selectedHour == 12) {
                selectedHour = 0;
            }

            // Save preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(SCHEDULE_HOUR_KEY, selectedHour);
            editor.putInt(SCHEDULE_MINUTE_KEY, selectedMinute);
            editor.putString(SCHEDULE_AM_PM_KEY, selectedAmPm);
            editor.apply();

            // Update UI
            int displayHour = selectedHour % 12;
            if (displayHour == 0) displayHour = 12;
            updateScheduleTimeText(displayHour, selectedMinute, selectedAmPm);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateScheduleTimeText(int hour, int minute, String amPm) {
        // Format the time string
        String formattedTime = String.format(Locale.US,
                "Scheduled update time %s %d:%02d", amPm, hour, minute);

        // Update the text view
        schedTimeInfoText.setText(formattedTime);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}