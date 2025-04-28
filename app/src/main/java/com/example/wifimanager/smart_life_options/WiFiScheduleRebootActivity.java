package com.example.wifimanager.smart_life_options;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wifimanager.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class WiFiScheduleRebootActivity extends AppCompatActivity {

    private TextView repeatValueTextView;
    private TextView rebootTimeTextView;
    private Set<String> selectedDays = new HashSet<>();
    private String currentRepeatMode = "Once";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi_schedule_reboot);

        repeatValueTextView = findViewById(R.id.repeat_value);
        rebootTimeTextView = findViewById(R.id.reboot_time);
        Button okButton = findViewById(R.id.ok_button);
        View backButton = findViewById(R.id.back_button);

        findViewById(R.id.repeat_option).setOnClickListener(v -> showRepeatOptionsDialog());
        findViewById(R.id.reboot_time_option).setOnClickListener(v -> showTimePickerDialog());

        okButton.setOnClickListener(v -> saveScheduleAndFinish());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void showRepeatOptionsDialog() {
        String[] repeatOptions = {"Once", "Everyday", "Weekdays", "Weekends", "Custom"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Repeat")
                .setItems(repeatOptions, (dialog, which) -> {
                    switch (which) {
                        case 0: currentRepeatMode = "Once";
                            selectedDays.clear();
                            repeatValueTextView.setText("Once");
                            break;
                        case 1: currentRepeatMode = "Everyday";
                            selectAllDays();
                            repeatValueTextView.setText("Everyday");
                            break;
                        case 2: currentRepeatMode = "Weekdays";
                            selectWeekdays();
                            repeatValueTextView.setText("Weekdays");
                            break;
                        case 3: currentRepeatMode = "Weekends";
                            selectWeekends();
                            repeatValueTextView.setText("Weekends");
                            break;
                        case 4: showCustomDaysDialog();
                            break;
                    }
                }).show();
    }

    private void showCustomDaysDialog() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        boolean[] checkedItems = new boolean[days.length];
        for (int i = 0; i < days.length; i++) {
            checkedItems[i] = selectedDays.contains(days[i]);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Days")
                .setMultiChoiceItems(days, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) selectedDays.add(days[which]);
                    else selectedDays.remove(days[which]);
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    if (selectedDays.isEmpty()) {
                        currentRepeatMode = "Once";
                        repeatValueTextView.setText("Once");
                    } else {
                        currentRepeatMode = "Custom";
                        repeatValueTextView.setText(formatSelectedDays());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatSelectedDays() {
        if (selectedDays.size() == 7) return "Everyday";
        if (selectedDays.size() == 5 && selectedDays.contains("Monday")
                && selectedDays.contains("Tuesday") && selectedDays.contains("Wednesday")
                && selectedDays.contains("Thursday") && selectedDays.contains("Friday")) {
            return "Weekdays";
        }
        if (selectedDays.size() == 2 && selectedDays.contains("Saturday")
                && selectedDays.contains("Sunday")) {
            return "Weekends";
        }
        ArrayList<String> daysList = new ArrayList<>(selectedDays);
        if (daysList.size() <= 3) return String.join(", ", daysList);
        return daysList.size() + " days";
    }

    private void selectAllDays() {
        selectedDays.clear();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) selectedDays.add(day);
    }

    private void selectWeekdays() {
        selectedDays.clear();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (String day : days) selectedDays.add(day);
    }

    private void selectWeekends() {
        selectedDays.clear();
        selectedDays.add("Saturday");
        selectedDays.add("Sunday");
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                R.style.TimePickerDialogTheme,
                (view, hourOfDay, selectedMinute) -> {
                    String formattedTime = String.format("%02d:%02d", hourOfDay, selectedMinute);
                    rebootTimeTextView.setText(formattedTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void saveScheduleAndFinish() {
        String rebootTime = rebootTimeTextView.getText().toString();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("repeat_mode", currentRepeatMode);
        resultIntent.putExtra("reboot_time", rebootTime);
        resultIntent.putStringArrayListExtra("selected_days", new ArrayList<>(selectedDays));

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
