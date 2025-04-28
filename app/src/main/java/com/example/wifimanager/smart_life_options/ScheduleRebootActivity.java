package com.example.wifimanager.smart_life_options;

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

import com.airbnb.lottie.LottieAnimationView;
import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ScheduleRebootActivity extends AppCompatActivity {

    private static final String TABLE_NAME = DatabaseHelper.TABLE_SCHEDULES;
    private static final String KEY_REBOOT_SCHEDULES = "reboot_schedules";

    private LinearLayout schedulesContainer;
    private ActivityResultLauncher<Intent> scheduleLauncher;
    private List<RebootSchedule> schedules = new ArrayList<>();
    private LottieAnimationView lottieAnimationView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_reboot);

        dbHelper = DatabaseHelper.getInstance(this);

        // Initialize Lottie animation
        lottieAnimationView = findViewById(R.id.lotti_animation);
        setupLottieAnimation();

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
                            String rebootTime = data.getStringExtra("reboot_time");
                            ArrayList<String> selectedDays = data.getStringArrayListExtra("selected_days");

                            RebootSchedule schedule = new RebootSchedule(repeatMode, rebootTime, selectedDays, true);
                            schedules.add(schedule);
                            updateSchedulesList();
                            saveSchedules();

                            // Play animation when new schedule is added
                            lottieAnimationView.playAnimation();
                        }
                    }
                });

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleRebootActivity.this, WiFiScheduleRebootActivity.class);
            scheduleLauncher.launch(intent);
        });

        updateSchedulesList();
    }

    private void setupLottieAnimation() {
        // Animation setup code remains the same
        lottieAnimationView.setAnimation(R.raw.reboot_animation);
        lottieAnimationView.setRepeatCount(0);
    }

    private void updateSchedulesList() {
        schedulesContainer.removeAllViews();

        for (int i = 0; i < schedules.size(); i++) {
            RebootSchedule schedule = schedules.get(i);
            View scheduleView = LayoutInflater.from(this).inflate(R.layout.item_schedule, schedulesContainer, false);

            TextView scheduleText = scheduleView.findViewById(R.id.schedule_text);
            SwitchCompat scheduleSwitch = scheduleView.findViewById(R.id.schedule_switch);
            View scheduleItem = scheduleView.findViewById(R.id.schedule_item);

            String scheduleDescription = String.format("Reboot at %s\n%s",
                    schedule.getRebootTime(), schedule.getFormattedDays());
            scheduleText.setText(scheduleDescription);

            scheduleSwitch.setChecked(schedule.isEnabled());

            int finalI = i;
            scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                schedules.get(finalI).setEnabled(isChecked);
                saveSchedules();

                // Optional: Play animation when switch is toggled
                if (isChecked) {
                    lottieAnimationView.playAnimation();
                }
            });

            scheduleItem.setOnClickListener(v -> {
                // Handle edit functionality if needed
            });

            scheduleItem.setOnLongClickListener(v -> {
                showDeleteDialog(finalI);
                return true;
            });

            schedulesContainer.addView(scheduleView);
        }
    }

    private void saveSchedules() {
        Gson gson = new Gson();
        String json = gson.toJson(schedules);
        dbHelper.putString(TABLE_NAME, KEY_REBOOT_SCHEDULES, json);
    }

    private List<RebootSchedule> loadSchedules() {
        Gson gson = new Gson();
        String json = dbHelper.getString(TABLE_NAME, KEY_REBOOT_SCHEDULES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<RebootSchedule>>() {}.getType();
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

                    // Play animation when schedule is deleted
                    lottieAnimationView.playAnimation();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume animation when activity resumes
        if (!lottieAnimationView.isAnimating()) {
            lottieAnimationView.resumeAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause animation when activity pauses to save resources
        if (lottieAnimationView.isAnimating()) {
            lottieAnimationView.pauseAnimation();
        }
    }

    public static class RebootSchedule implements Serializable {
        private String repeatMode;
        private String rebootTime;
        private ArrayList<String> selectedDays;
        private boolean enabled;

        public RebootSchedule(String repeatMode, String rebootTime, ArrayList<String> selectedDays, boolean enabled) {
            this.repeatMode = repeatMode;
            this.rebootTime = rebootTime;
            this.selectedDays = selectedDays;
            this.enabled = enabled;
        }

        public String getRebootTime() {
            return rebootTime;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

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