package com.example.wifimanager.smart_life_options;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;
import com.example.wifimanager.receivers.WiFiScheduleReceiver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HealthModeActivity extends AppCompatActivity {

    private static final String TABLE_NAME = DatabaseHelper.TABLE_SCHEDULES;
    private static final String KEY_SCHEDULES = "schedules";
    private static final String BASE_URL = "http://192.168.31.1/cgi-bin/luci/;stok=";
    private static final String WIFI_CONTROL_ENDPOINT = "/api/xqnetwork/set_all_wifi";
    private static final String TAG = "HealthModeActivity";

    private LinearLayout schedulesContainer;
    private ActivityResultLauncher<Intent> scheduleLauncher;
    private List<Schedule> schedules = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String stok;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_mode);

        // Initialize database helper
        dbHelper = DatabaseHelper.getInstance(this);

        // Initialize RequestQueue for API calls
        requestQueue = Volley.newRequestQueue(this);

        // Get STOK from intent
        stok = getIntent().getStringExtra("STOK");
        if (stok == null) {
            Toast.makeText(this, "Authentication token missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        schedulesContainer = findViewById(R.id.schedules_container);
        FloatingActionButton fab = findViewById(R.id.add_schedule_fab);        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }


        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Load saved schedules and update UI
        schedules = loadSchedules();
        updateSchedulesList();

        // Setup add schedule button
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(HealthModeActivity.this, WiFiScheduleActivity.class);
            intent.putExtra("STOK", stok);
            scheduleLauncher.launch(intent);
        });

        // Add debug test option with long press on fab
        fab.setOnLongClickListener(v -> {
            testImmediateSchedule();
            return true;
        });

        // Initialize activity launcher for add/edit schedule
        scheduleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String offTime = data.getStringExtra("off_time");
                    String onTime = data.getStringExtra("on_time");
                    String repeatMode = data.getStringExtra("repeat_mode");
                    ArrayList<String> selectedDays = data.getStringArrayListExtra("selected_days");
                    
                    Schedule schedule = new Schedule(offTime, onTime, 
                        repeatMode.equals("Custom") ? formatSelectedDays(selectedDays) : repeatMode);
                    
                    schedules.add(schedule);
                    saveSchedules();
                    updateSchedulesList();
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyAlarms();
    }

    private void updateSchedulesList() {
        schedulesContainer.removeAllViews();
        for (int i = 0; i < schedules.size(); i++) {
            Schedule schedule = schedules.get(i);
            View scheduleView = LayoutInflater.from(this)
                    .inflate(R.layout.item_schedule, schedulesContainer, false);

            TextView scheduleText = scheduleView.findViewById(R.id.schedule_text);
            SwitchCompat scheduleSwitch = scheduleView.findViewById(R.id.schedule_switch);

            String scheduleDescription = String.format("Off: %s\nOn: %s\n%s", 
                schedule.getOffTime(), schedule.getOnTime(), schedule.getRepeatDays());
            scheduleText.setText(scheduleDescription);
            scheduleSwitch.setChecked(schedule.isEnabled());

            final int position = i;
            scheduleView.setOnClickListener(v -> {
                // Handle edit schedule click
                // Implementation depends on your EditScheduleActivity
            });

            scheduleView.setOnLongClickListener(v -> {
                showDeleteDialog(position);
                return true;
            });

            // Update the switch change listener to schedule/cancel alarms
            scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                schedule.setEnabled(isChecked);
                if (isChecked) {
                    scheduleWiFiControl(schedule);
                } else {
                    cancelWiFiControl(schedule);
                }
                saveSchedules();
            });

            schedulesContainer.addView(scheduleView);
        }
    }

    private void saveSchedules() {
        Gson gson = new Gson();
        String json = gson.toJson(schedules);
        dbHelper.putString(TABLE_NAME, KEY_SCHEDULES, json);
    }

    private List<Schedule> loadSchedules() {
        Gson gson = new Gson();
        String json = dbHelper.getString(TABLE_NAME, KEY_SCHEDULES, null);
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
                    Schedule schedule = schedules.get(position);
                    cancelWiFiControl(schedule);
                    schedules.remove(position);
                    saveSchedules();
                    updateSchedulesList();
                    Toast.makeText(this, "Schedule deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void scheduleWiFiControl(Schedule schedule) {
        Log.d(TAG, "Scheduling WiFi control - Off time: " + schedule.getOffTime() + ", On time: " + schedule.getOnTime());
        
        if (!schedule.isEnabled()) {
            Log.d(TAG, "Schedule is disabled, cancelling alarms");
            cancelWiFiControl(schedule);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        // Create intents for both on and off times
        Intent offIntent = new Intent(this, WiFiScheduleReceiver.class);
        offIntent.setAction("com.example.wifimanager.WIFI_SCHEDULE_OFF");
        offIntent.putExtra("STOK", stok);
        
        Intent onIntent = new Intent(this, WiFiScheduleReceiver.class);
        onIntent.setAction("com.example.wifimanager.WIFI_SCHEDULE_ON");
        onIntent.putExtra("STOK", stok);

        // Create unique request codes based on schedule times
        int offRequestCode = (schedule.getOffTime() + "OFF").hashCode();
        int onRequestCode = (schedule.getOnTime() + "ON").hashCode();
        
        Log.d(TAG, "Creating PendingIntents - Off code: " + offRequestCode + ", On code: " + onRequestCode);

        PendingIntent offPendingIntent = PendingIntent.getBroadcast(
            this, offRequestCode, offIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        PendingIntent onPendingIntent = PendingIntent.getBroadcast(
            this, onRequestCode, onIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Parse times
        String[] offTimeParts = schedule.getOffTime().split(":");
        String[] onTimeParts = schedule.getOnTime().split(":");

        Calendar offCal = Calendar.getInstance();
        offCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(offTimeParts[0]));
        offCal.set(Calendar.MINUTE, Integer.parseInt(offTimeParts[1]));
        offCal.set(Calendar.SECOND, 0);

        Calendar onCal = Calendar.getInstance();
        onCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(onTimeParts[0]));
        onCal.set(Calendar.MINUTE, Integer.parseInt(onTimeParts[1]));
        onCal.set(Calendar.SECOND, 0);

        // If time has passed for today, set for tomorrow
        if (offCal.before(Calendar.getInstance())) {
            Log.d(TAG, "Off time already passed today, scheduling for tomorrow");
            offCal.add(Calendar.DAY_OF_YEAR, 1);
        }
        if (onCal.before(Calendar.getInstance())) {
            Log.d(TAG, "On time already passed today, scheduling for tomorrow");
            onCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d(TAG, "Setting alarms - Off time: " + offCal.getTime() + ", On time: " + onCal.getTime());

        // Set the alarms
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    offCal.getTimeInMillis(),
                    offPendingIntent
                );
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    onCal.getTimeInMillis(),
                    onPendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    offCal.getTimeInMillis(),
                    offPendingIntent
                );
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    onCal.getTimeInMillis(),
                    onPendingIntent
                );
            }
            Log.d(TAG, "Alarms set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarms", e);
            Toast.makeText(this, "Failed to schedule WiFi control: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelWiFiControl(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        // Cancel off schedule
        Intent offIntent = new Intent(this, WiFiScheduleReceiver.class);
        offIntent.setAction("com.example.wifimanager.WIFI_SCHEDULE_OFF");
        int offRequestCode = (schedule.getOffTime() + "OFF").hashCode();
        PendingIntent offPendingIntent = PendingIntent.getBroadcast(
            this, offRequestCode, offIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(offPendingIntent);

        // Cancel on schedule
        Intent onIntent = new Intent(this, WiFiScheduleReceiver.class);
        onIntent.setAction("com.example.wifimanager.WIFI_SCHEDULE_ON");
        int onRequestCode = (schedule.getOnTime() + "ON").hashCode();
        PendingIntent onPendingIntent = PendingIntent.getBroadcast(
            this, onRequestCode, onIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(onPendingIntent);
    }

    private void verifyAlarms() {
        Log.d(TAG, "Verifying scheduled alarms...");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        for (Schedule schedule : schedules) {
            if (!schedule.isEnabled()) continue;

            // Check OFF alarm
            Intent offIntent = new Intent(this, WiFiScheduleReceiver.class);
            offIntent.setAction("com.example.wifimanager.WIFI_SCHEDULE_OFF");
            int offRequestCode = (schedule.getOffTime() + "OFF").hashCode();
            PendingIntent offPendingIntent = PendingIntent.getBroadcast(
                this, offRequestCode, offIntent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Check ON alarm
            Intent onIntent = new Intent(this, WiFiScheduleReceiver.class);
            onIntent.setAction("com.example.wifimanager.WIFI_SCHEDULE_ON");
            int onRequestCode = (schedule.getOnTime() + "ON").hashCode();
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(
                this, onRequestCode, onIntent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            Log.d(TAG, String.format("Schedule: %s-%s, Off Alarm: %s, On Alarm: %s", 
                schedule.getOffTime(), schedule.getOnTime(),
                offPendingIntent != null ? "Set" : "Not Set",
                onPendingIntent != null ? "Set" : "Not Set"
            ));

            // Test the alarms immediately if they're not set
            if (offPendingIntent == null || onPendingIntent == null) {
                Log.w(TAG, "Some alarms not found, rescheduling...");
                scheduleWiFiControl(schedule);
            }
        }
    }

    private String formatSelectedDays(ArrayList<String> selectedDays) {
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
        if (selectedDays.size() <= 3) return String.join(", ", selectedDays);
        return selectedDays.size() + " days";
    }

    private void executeWiFiSchedule(boolean turnOn) {
        // Acquire wake lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WiFiManager:WiFiScheduleExecuteWakeLock"
        );
        wakeLock.acquire(60000); // 60 seconds timeout

        String url = BASE_URL + stok + WIFI_CONTROL_ENDPOINT + "?on1=" + (turnOn ? "1" : "0") + "&on2=" + (turnOn ? "1" : "0");
        Log.d(TAG, "Executing WiFi schedule - URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    Log.d(TAG, "WiFi control response: " + response.toString());
                    if (response.getInt("code") == 0) {
                        String action = turnOn ? "enabled" : "disabled";
                        Log.d(TAG, "WiFi " + action + " successfully");
                        Toast.makeText(this, "WiFi " + action + " successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to control WiFi - Error code: " + response.getInt("code"));
                        Toast.makeText(this, "Failed to control WiFi", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing WiFi control response", e);
                    Toast.makeText(this, "Error controlling WiFi", Toast.LENGTH_SHORT).show();
                } finally {
                    wakeLock.release();
                }
            },
            error -> {
                Log.e(TAG, "Network error while controlling WiFi", error);
                Toast.makeText(this, "Network error while controlling WiFi", Toast.LENGTH_SHORT).show();
                wakeLock.release();
            });

        request.setRetryPolicy(new DefaultRetryPolicy(
            30000, // 30 seconds timeout
            2, // 2 retries
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);

        // Set up a handler to release wake lock if request takes too long
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.w(TAG, "Releasing wake lock due to timeout");
            }
        }, 30000); // 30 seconds timeout
    }

    private void testImmediateSchedule() {
        Log.d(TAG, "Testing immediate schedule...");
        
        // Create a schedule for 1 minute from now
        Calendar calendar = Calendar.getInstance();
        
        // Off time in 1 minute
        calendar.add(Calendar.MINUTE, 1);
        String offTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), 
                                     calendar.get(Calendar.MINUTE));
        
        // On time in 2 minutes
        calendar.add(Calendar.MINUTE, 1);
        String onTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), 
                                    calendar.get(Calendar.MINUTE));
        
        Schedule testSchedule = new Schedule(offTime, onTime, "Test");
        Log.d(TAG, "Created test schedule - Off: " + offTime + ", On: " + onTime);
        
        scheduleWiFiControl(testSchedule);
        
        Toast.makeText(this, "Test schedule created. WiFi will turn off in 1 minute and on in 2 minutes", 
                      Toast.LENGTH_LONG).show();
    }

    public static class Schedule implements Serializable {
        private String offTime;
        private String onTime;
        private String repeatDays;
        private boolean enabled;

        public Schedule(String offTime, String onTime, String repeatDays) {
            this.offTime = offTime;
            this.onTime = onTime;
            this.repeatDays = repeatDays;
            this.enabled = true;
        }

        public String getOffTime() {
            return offTime;
        }

        public String getOnTime() {
            return onTime;
        }

        public String getRepeatDays() {
            return repeatDays;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}