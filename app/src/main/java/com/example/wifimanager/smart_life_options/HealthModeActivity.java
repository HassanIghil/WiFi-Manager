package com.example.wifimanager.smart_life_options;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.wifimanager.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HealthModeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WiFiSchedulePrefs";
    private static final String SCHEDULES_KEY = "schedules";

    private LinearLayout schedulesContainer;
    private ActivityResultLauncher<Intent> scheduleLauncher;
    private List<Schedule> schedules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_mode);

        // Load saved schedules
        schedules = loadSchedules();

        // Initialize views
        schedulesContainer = findViewById(R.id.schedules_container);
        Toolbar toolbar = findViewById(R.id.toolbar);
        FloatingActionButton fab = findViewById(R.id.add_schedule_fab);

        // Set up toolbar
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize the activity result launcher
        scheduleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String repeatMode = data.getStringExtra("repeat_mode");
                            String offTime = data.getStringExtra("off_time");
                            String onTime = data.getStringExtra("on_time");
                            ArrayList<String> selectedDays = data.getStringArrayListExtra("selected_days");

                            Schedule schedule = new Schedule(repeatMode, offTime, onTime, selectedDays, true);
                            schedules.add(schedule);
                            updateSchedulesList();
                            saveSchedules();
                        }
                    }
                });

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(HealthModeActivity.this, WiFiScheduleActivity.class);
            scheduleLauncher.launch(intent);
        });

        updateSchedulesList();
    }

    private void updateSchedulesList() {
        schedulesContainer.removeAllViews();

        for (int i = 0; i < schedules.size(); i++) {
            Schedule schedule = schedules.get(i);
            View scheduleView = LayoutInflater.from(this).inflate(R.layout.item_schedule, schedulesContainer, false);

            TextView scheduleText = scheduleView.findViewById(R.id.schedule_text);
            SwitchCompat scheduleSwitch = scheduleView.findViewById(R.id.schedule_switch);
            View scheduleItem = scheduleView.findViewById(R.id.schedule_item);

            String scheduleDescription = String.format("Turn off Wi-Fi at %s, turn on Wi-Fi at %s\n%s",
                    schedule.getOffTime(), schedule.getOnTime(), schedule.getFormattedDays());
            scheduleText.setText(scheduleDescription);

            scheduleSwitch.setChecked(schedule.isEnabled());

            int finalI = i;
            scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                schedules.get(finalI).setEnabled(isChecked);
                saveSchedules();
            });

            scheduleItem.setOnLongClickListener(v -> {
                showDeleteDialog(finalI);
                return true;
            });

            schedulesContainer.addView(scheduleView);
        }
    }

    private void saveSchedules() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(schedules);
        editor.putString(SCHEDULES_KEY, json);
        editor.apply();
    }

    private List<Schedule> loadSchedules() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(SCHEDULES_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Schedule>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this schedule?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    schedules.remove(position);
                    updateSchedulesList();
                    saveSchedules();
                    Toast.makeText(this, "Schedule deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static class Schedule implements Serializable {
        private String repeatMode;
        private String offTime;
        private String onTime;
        private List<String> selectedDays;
        private boolean enabled;

        public Schedule() {}

        public Schedule(String repeatMode, String offTime, String onTime, List<String> selectedDays, boolean enabled) {
            this.repeatMode = repeatMode;
            this.offTime = offTime;
            this.onTime = onTime;
            this.selectedDays = selectedDays;
            this.enabled = enabled;
        }

        public String getRepeatMode() { return repeatMode; }
        public String getOffTime() { return offTime; }
        public String getOnTime() { return onTime; }
        public List<String> getSelectedDays() { return selectedDays; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getFormattedDays() {
            if (repeatMode.equals("Once")) return "Once";
            if (repeatMode.equals("Everyday")) return "Everyday";
            if (repeatMode.equals("Weekdays")) return "Weekdays";
            if (repeatMode.equals("Weekends")) return "Weekends";
            if (selectedDays.size() <= 3) return String.join(", ", selectedDays);
            return selectedDays.size() + " days";
        }
    }
}