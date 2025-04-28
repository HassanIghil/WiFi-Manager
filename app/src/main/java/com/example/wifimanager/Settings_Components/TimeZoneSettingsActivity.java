package com.example.wifimanager.Settings_Components;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class TimeZoneSettingsActivity extends AppCompatActivity {

    private TextView timeZoneValueText;
    private TextView dateValueText;
    private TextView timeValueText;
    private Timer timer;
    private String currentTimeZone = "Africa/Casablanca";
    private TimeZoneAdapter.TimeZoneItem selectedTimeZone;
    private Calendar currentCalendar; // Calendar to track current date and time
    private boolean useCustomTime = false; // Flag to indicate if we're using custom time
    private long timeOffset = 0; // Offset from system time in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_zone_settings);

        // Initialize views
        timeZoneValueText = findViewById(R.id.time_zone_value);
        dateValueText = findViewById(R.id.date_value);
        timeValueText = findViewById(R.id.time_value);

        // Initialize calendar with current time zone
        currentCalendar = Calendar.getInstance(TimeZone.getTimeZone(currentTimeZone));

        // Set click listeners for back button
        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        // Set click listeners for settings
        findViewById(R.id.time_zone_container).setOnClickListener(v -> showTimeZonePicker());
        findViewById(R.id.date_container).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.time_container).setOnClickListener(v -> showTimePicker());

        // Initialize timezone display
        String displayName = getTimeZoneDisplayName(currentTimeZone);
        if (displayName != null) {
            timeZoneValueText.setText(displayName);
        }

        // Start timer for updating current time
        startTimeUpdater();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startTimeUpdater() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> updateCurrentTime());
            }
        }, 0, 1000); // Update every second
    }

    private void updateCurrentTime() {
        if (useCustomTime) {
            // If using custom time, increment the calendar by 1 second
            currentCalendar.add(Calendar.SECOND, 1);
        } else {
            // Otherwise use system time + any offset
            currentCalendar.setTimeInMillis(System.currentTimeMillis() + timeOffset);
        }

        // Display the current date and time
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateSdf.setTimeZone(TimeZone.getTimeZone(currentTimeZone));
        dateValueText.setText(dateSdf.format(currentCalendar.getTime()));

        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeSdf.setTimeZone(TimeZone.getTimeZone(currentTimeZone));
        timeValueText.setText(timeSdf.format(currentCalendar.getTime()));
    }

    private String getTimeZoneDisplayName(String timeZoneId) {
        List<TimeZoneAdapter.TimeZoneItem> timeZones = getTimeZoneItems();
        for (TimeZoneAdapter.TimeZoneItem item : timeZones) {
            if (item.getTimeZoneId().equals(timeZoneId)) {
                return item.getDisplayName();
            }
        }
        return null;
    }

    private void showTimeZonePicker() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_time_zone_selector);

        // Initialize views
        EditText searchEditText = dialog.findViewById(R.id.search_time_zone);
        RecyclerView recyclerView = dialog.findViewById(R.id.time_zone_recycler_view);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        Button applyButton = dialog.findViewById(R.id.apply_button);

        // Get list of time zones
        List<TimeZoneAdapter.TimeZoneItem> timeZoneItems = getTimeZoneItems();

        // Set up adapter
        TimeZoneAdapter adapter = new TimeZoneAdapter(timeZoneItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Pre-select current timezone
        for (int i = 0; i < timeZoneItems.size(); i++) {
            if (timeZoneItems.get(i).getTimeZoneId().equals(currentTimeZone)) {
                adapter.setSelectedPosition(i);
                recyclerView.scrollToPosition(i);
                selectedTimeZone = timeZoneItems.get(i);
                break;
            }
        }

        // Set up search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up item selection listener
        adapter.setOnTimeZoneSelectedListener(timeZoneItem -> {
            selectedTimeZone = timeZoneItem;
        });

        // Set up buttons
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        applyButton.setOnClickListener(v -> {
            if (selectedTimeZone != null) {
                String oldTimeZone = currentTimeZone;
                currentTimeZone = selectedTimeZone.getTimeZoneId();
                timeZoneValueText.setText(selectedTimeZone.getDisplayName());

                // Adjust time for the new timezone
                if (useCustomTime) {
                    // Keep the same absolute time but adjust for timezone
                    TimeZone oldTZ = TimeZone.getTimeZone(oldTimeZone);
                    TimeZone newTZ = TimeZone.getTimeZone(currentTimeZone);

                    // Extract the values we want to maintain
                    int year = currentCalendar.get(Calendar.YEAR);
                    int month = currentCalendar.get(Calendar.MONTH);
                    int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
                    int hour = currentCalendar.get(Calendar.HOUR_OF_DAY);
                    int minute = currentCalendar.get(Calendar.MINUTE);
                    int second = currentCalendar.get(Calendar.SECOND);

                    // Create new calendar with new timezone
                    currentCalendar = Calendar.getInstance(newTZ);

                    // Set the same time values in new timezone
                    currentCalendar.set(year, month, day, hour, minute, second);
                } else {
                    // Just adjust the timezone
                    currentCalendar.setTimeZone(TimeZone.getTimeZone(currentTimeZone));
                }

                updateCurrentTime();
                Toast.makeText(this, "Time zone changed to " + selectedTimeZone.getDisplayName(), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDatePicker() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_date_picker);

        // Initialize pickers
        NumberPicker monthPicker = dialog.findViewById(R.id.month_picker);
        NumberPicker dayPicker = dialog.findViewById(R.id.day_picker);
        NumberPicker yearPicker = dialog.findViewById(R.id.year_picker);

        // Get current date from our calendar
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        // Set up month picker
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(currentMonth);

        // Set up day picker
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(31);
        dayPicker.setValue(currentDay);

        // Set up year picker
        yearPicker.setMinValue(currentYear - 5);
        yearPicker.setMaxValue(currentYear + 5);
        yearPicker.setValue(currentYear);

        // Set up listeners to adjust days for month
        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            updateDaysInMonth(dayPicker, yearPicker.getValue(), newVal);
        });

        yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            updateDaysInMonth(dayPicker, newVal, monthPicker.getValue());
        });

        // Set up preview TextView
        TextView previewDateText = dialog.findViewById(R.id.preview_date);
        if (previewDateText != null) {
            updatePreviewDate(previewDateText, yearPicker.getValue(), monthPicker.getValue(), dayPicker.getValue());

            // Update preview when values change
            NumberPicker.OnValueChangeListener updatePreview = (picker, oldVal, newVal) -> {
                updatePreviewDate(previewDateText, yearPicker.getValue(), monthPicker.getValue(), dayPicker.getValue());
            };

            yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                updateDaysInMonth(dayPicker, newVal, monthPicker.getValue());
                updatePreviewDate(previewDateText, yearPicker.getValue(), monthPicker.getValue(), dayPicker.getValue());
            });

            monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                updateDaysInMonth(dayPicker, yearPicker.getValue(), newVal);
                updatePreviewDate(previewDateText, yearPicker.getValue(), newVal, dayPicker.getValue());
            });

            dayPicker.setOnValueChangedListener(updatePreview);
        }

        // Set up buttons
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        Button okButton = dialog.findViewById(R.id.ok_button);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            // Get the current hour, minute, second to preserve them
            int hour = currentCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = currentCalendar.get(Calendar.MINUTE);
            int second = currentCalendar.get(Calendar.SECOND);

            // Update the currentCalendar with new date values
            currentCalendar.set(Calendar.YEAR, yearPicker.getValue());
            currentCalendar.set(Calendar.MONTH, monthPicker.getValue());
            currentCalendar.set(Calendar.DAY_OF_MONTH, dayPicker.getValue());

            // Restore the time
            currentCalendar.set(Calendar.HOUR_OF_DAY, hour);
            currentCalendar.set(Calendar.MINUTE, minute);
            currentCalendar.set(Calendar.SECOND, second);

            // Enable custom time mode
            useCustomTime = true;

            // Update date value text with the new date
            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dateSdf.setTimeZone(TimeZone.getTimeZone(currentTimeZone));
            dateValueText.setText(dateSdf.format(currentCalendar.getTime()));

            Toast.makeText(this, "Date updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updatePreviewDate(TextView textView, int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        textView.setText(sdf.format(calendar.getTime()));
    }

    private void updateDaysInMonth(NumberPicker dayPicker, int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int currentDay = dayPicker.getValue();
        dayPicker.setMaxValue(maxDays);
        if (currentDay > maxDays) {
            dayPicker.setValue(maxDays);
        }
    }

    private void showTimePicker() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_time_picker);

        // Initialize pickers
        NumberPicker hourPicker = dialog.findViewById(R.id.hour_picker);
        NumberPicker minutePicker = dialog.findViewById(R.id.minute_picker);
        NumberPicker secondPicker = dialog.findViewById(R.id.second_picker);

        // Get current time from our calendar
        int currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = currentCalendar.get(Calendar.MINUTE);
        int currentSecond = currentCalendar.get(Calendar.SECOND);

        // Set up hour picker (0-23)
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(currentHour);

        // Set up minute picker (0-59)
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(currentMinute);

        // Set up second picker (0-59)
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        secondPicker.setValue(currentSecond);

        // Set up preview TextView
        TextView previewTimeText = dialog.findViewById(R.id.preview_time);
        if (previewTimeText != null) {
            updatePreviewTime(previewTimeText, hourPicker.getValue(), minutePicker.getValue(), secondPicker.getValue());

            // Update preview when values change
            NumberPicker.OnValueChangeListener updatePreview = (picker, oldVal, newVal) -> {
                updatePreviewTime(previewTimeText, hourPicker.getValue(), minutePicker.getValue(), secondPicker.getValue());
            };

            hourPicker.setOnValueChangedListener(updatePreview);
            minutePicker.setOnValueChangedListener(updatePreview);
            secondPicker.setOnValueChangedListener(updatePreview);
        }

        // Set up buttons
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        Button okButton = dialog.findViewById(R.id.ok_button);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            // Get the current date components to preserve them
            int year = currentCalendar.get(Calendar.YEAR);
            int month = currentCalendar.get(Calendar.MONTH);
            int day = currentCalendar.get(Calendar.DAY_OF_MONTH);

            // Update the currentCalendar with new time values
            currentCalendar.set(Calendar.HOUR_OF_DAY, hourPicker.getValue());
            currentCalendar.set(Calendar.MINUTE, minutePicker.getValue());
            currentCalendar.set(Calendar.SECOND, secondPicker.getValue());

            // Restore the date
            currentCalendar.set(Calendar.YEAR, year);
            currentCalendar.set(Calendar.MONTH, month);
            currentCalendar.set(Calendar.DAY_OF_MONTH, day);

            // Enable custom time mode
            useCustomTime = true;

            // Update time value text with the new time
            SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            timeSdf.setTimeZone(TimeZone.getTimeZone(currentTimeZone));
            timeValueText.setText(timeSdf.format(currentCalendar.getTime()));

            Toast.makeText(this, "Time updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updatePreviewTime(TextView textView, int hour, int minute, int second) {
        String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second);
        textView.setText(timeString);
    }

    private List<TimeZoneAdapter.TimeZoneItem> getTimeZoneItems() {
        List<TimeZoneAdapter.TimeZoneItem> timeZoneItems = new ArrayList<>();

        // Common time zones
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Pacific/Midway", "(UTC-11:00) Midway Island, Samoa"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Pacific/Honolulu", "(UTC-10:00) Hawaii"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("America/Anchorage", "(UTC-09:00) Alaska"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("America/Los_Angeles", "(UTC-08:00) Pacific Time (US & Canada)"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("America/Denver", "(UTC-07:00) Mountain Time (US & Canada)"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("America/Chicago", "(UTC-06:00) Central Time (US & Canada)"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("America/New_York", "(UTC-05:00) Eastern Time (US & Canada)"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("America/Halifax", "(UTC-04:00) Atlantic Time (Canada)"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("America/Sao_Paulo", "(UTC-03:00) Brasilia"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Atlantic/South_Georgia", "(UTC-02:00) Mid-Atlantic"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Atlantic/Azores", "(UTC-01:00) Azores"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Europe/London", "(UTC+00:00) London, Dublin, Edinburgh"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Africa/Casablanca", "(UTC+01:00) Casablanca, El Aaiun"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Europe/Paris", "(UTC+01:00) Berlin, Paris, Rome, Madrid"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Europe/Athens", "(UTC+02:00) Athens, Cairo, Istanbul"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Europe/Moscow", "(UTC+03:00) Moscow, St. Petersburg"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Asia/Dubai", "(UTC+04:00) Dubai, Abu Dhabi"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Asia/Karachi", "(UTC+05:00) Karachi, Islamabad"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Asia/Kolkata", "(UTC+05:30) Mumbai, New Delhi"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Asia/Dhaka", "(UTC+06:00) Dhaka, Almaty"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Asia/Bangkok", "(UTC+07:00) Bangkok, Jakarta"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Asia/Shanghai", "(UTC+08:00) Beijing, Hong Kong, Singapore"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Asia/Tokyo", "(UTC+09:00) Tokyo, Seoul"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Australia/Sydney", "(UTC+10:00) Sydney, Melbourne"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Pacific/Guadalcanal", "(UTC+11:00) Solomon Islands"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Pacific/Auckland", "(UTC+12:00) Auckland, Wellington"));
        timeZoneItems.add(new TimeZoneAdapter.TimeZoneItem("Pacific/Tongatapu", "(UTC+13:00) Nuku'alofa, Samoa"));

        return timeZoneItems;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}