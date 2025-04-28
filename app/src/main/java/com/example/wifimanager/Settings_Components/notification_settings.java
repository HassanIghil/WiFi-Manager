package com.example.wifimanager.Settings_Components;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class notification_settings extends AppCompatActivity {

    private TextView tvFromTime;
    private TextView tvToTime;
    private SwitchCompat switchDnd;
    private SwitchMaterial switchRouterNotifications;
    private LinearLayout dndSettingsLayout;
    private MaterialCardView dndCardView;
    private int fromHour = 23, fromMinute = 0;
    private int toHour = 7, toMinute = 0;
    
    private DatabaseHelper dbHelper;
    private static final String TABLE_NAME = DatabaseHelper.TABLE_NOTIFICATIONS;
    private static final String KEY_DND_ENABLED = "dnd_enabled";
    private static final String KEY_ROUTER_NOTIFICATIONS = "router_notifications";
    private static final String KEY_FROM_HOUR = "from_hour";
    private static final String KEY_FROM_MINUTE = "from_minute";
    private static final String KEY_TO_HOUR = "to_hour";
    private static final String KEY_TO_MINUTE = "to_minute";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        dbHelper = DatabaseHelper.getInstance(this);

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
        tvFromTime = findViewById(R.id.tv_from_time);
        tvToTime = findViewById(R.id.tv_to_time);
        switchDnd = findViewById(R.id.switch_dnd);
        switchRouterNotifications = findViewById(R.id.switch_router_notifications);
        dndSettingsLayout = findViewById(R.id.layout_dnd_settings);
        dndCardView = findViewById(R.id.dnd_card);

        // Load saved settings
        loadSavedSettings();

        // Set up click listeners
        dndSettingsLayout.setOnClickListener(v -> {
            if (switchDnd.isChecked()) {
                showTimeSelectionDialog();
            } else {
                Toast.makeText(notification_settings.this,
                        "Enable Do Not Disturb first",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Router notifications switch state change listener
        switchRouterNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDndCardState(isChecked);
            dbHelper.putBoolean(TABLE_NAME, KEY_ROUTER_NOTIFICATIONS, isChecked);
        });

        // DND switch state change listener
        switchDnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dndSettingsLayout.setEnabled(isChecked);
            dndSettingsLayout.setAlpha(isChecked ? 1.0f : 0.5f);
            dbHelper.putBoolean(TABLE_NAME, KEY_DND_ENABLED, isChecked);
        });

        // Initial state setup
        updateDndCardState(switchRouterNotifications.isChecked());
    }

    private void loadSavedSettings() {
        // Load router notifications state
        boolean routerNotifications = dbHelper.getBoolean(TABLE_NAME, KEY_ROUTER_NOTIFICATIONS, true);
        switchRouterNotifications.setChecked(routerNotifications);
        
        // Load DND state
        boolean dndEnabled = dbHelper.getBoolean(TABLE_NAME, KEY_DND_ENABLED, false);
        switchDnd.setChecked(dndEnabled);
        
        // Load DND times
        fromHour = dbHelper.getInt(TABLE_NAME, KEY_FROM_HOUR, 23);
        fromMinute = dbHelper.getInt(TABLE_NAME, KEY_FROM_MINUTE, 0);
        toHour = dbHelper.getInt(TABLE_NAME, KEY_TO_HOUR, 7);
        toMinute = dbHelper.getInt(TABLE_NAME, KEY_TO_MINUTE, 0);
        
        updateTimeDisplay();
    }

    private void updateDndCardState(boolean notificationsEnabled) {
        if (notificationsEnabled) {
            // Enable DND card
            dndCardView.setEnabled(true);
            dndCardView.setAlpha(1.0f);
            switchDnd.setEnabled(true);
            dndSettingsLayout.setEnabled(switchDnd.isChecked());
        } else {
            // Disable DND card and turn off DND switch
            dndCardView.setEnabled(false);
            dndCardView.setAlpha(0.5f);
            switchDnd.setChecked(false);
            switchDnd.setEnabled(false);
            dndSettingsLayout.setEnabled(false);
            dndSettingsLayout.setAlpha(0.5f);
        }
    }

    private void showTimeSelectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_selection, null);

        MaterialCardView fromTimeCard = dialogView.findViewById(R.id.card_from_time);
        MaterialCardView toTimeCard = dialogView.findViewById(R.id.card_to_time);
        TextView dialogFromTime = dialogView.findViewById(R.id.dialog_from_time);
        TextView dialogToTime = dialogView.findViewById(R.id.dialog_to_time);

        // Set current times
        dialogFromTime.setText(String.format(Locale.getDefault(), "%02d:%02d", fromHour, fromMinute));
        dialogToTime.setText(String.format(Locale.getDefault(), "%02d:%02d", toHour, toMinute));

        fromTimeCard.setOnClickListener(v -> showTimePickerDialog(true, dialogFromTime));
        toTimeCard.setOnClickListener(v -> showTimePickerDialog(false, dialogToTime));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Do Not Disturb Time")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Parse and save times from dialog
                    String fromTimeText = dialogFromTime.getText().toString();
                    String toTimeText = dialogToTime.getText().toString();

                    try {
                        String[] fromParts = fromTimeText.split(":");
                        String[] toParts = toTimeText.split(":");

                        fromHour = Integer.parseInt(fromParts[0]);
                        fromMinute = Integer.parseInt(fromParts[1]);
                        toHour = Integer.parseInt(toParts[0]);
                        toMinute = Integer.parseInt(toParts[1]);

                        // Save to database
                        dbHelper.putInt(TABLE_NAME, KEY_FROM_HOUR, fromHour);
                        dbHelper.putInt(TABLE_NAME, KEY_FROM_MINUTE, fromMinute);
                        dbHelper.putInt(TABLE_NAME, KEY_TO_HOUR, toHour);
                        dbHelper.putInt(TABLE_NAME, KEY_TO_MINUTE, toMinute);

                        updateTimeDisplay();
                        Toast.makeText(this, "DND times updated", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTimePickerDialog(boolean isFromTime, TextView textView) {
        int hour = isFromTime ? fromHour : toHour;
        int minute = isFromTime ? fromMinute : toMinute;

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                R.style.TimePickerTheme,
                (view, hourOfDay, minuteOfDay) -> {
                    // Update the text view with the selected time
                    textView.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfDay));
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void updateTimeDisplay() {
        tvFromTime.setText(String.format(Locale.getDefault(), "%02d:%02d", fromHour, fromMinute));
        tvToTime.setText(String.format(Locale.getDefault(), "%02d:%02d", toHour, toMinute));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}