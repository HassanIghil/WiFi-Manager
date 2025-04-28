package com.example.wifimanager.Tools;

import static android.app.PendingIntent.getActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.airbnb.lottie.LottieAnimationView;
import com.example.wifimanager.R;
import com.example.wifimanager.Settings_Components.BlockedDevicesActivity;
import com.example.wifimanager.Settings_Components.Menu_Blocklist;
import com.example.wifimanager.Settings_Components.Security;
import com.example.wifimanager.Settings_Components.pass_reset;
import com.example.wifimanager.Settings_Components.wifi_settings;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class Firewall extends AppCompatActivity {
    private static String STOK;

    private LottieAnimationView lottieAnimationView;
    private ImageView innerImageView;
    private TextView smallTextView;
    private MaterialButton turnOffButton;
    private TextView gifTextView3, textView0hr40m;
    private View statusCard; // Changed from MaterialCardView to View to match FrameLayout in XML
    private Toolbar toolbar;
    private ImageView menuButton; // Added to match XML

    private int originalBackgroundColor;
    private int originalToolbarColor;
    private int originalStatusBarColor;

    private OkHttpClient httpClient;
    private SharedPreferences sharedPreferences;

    private static final String PREF_TOTAL_TIME = "total_accumulated_time";
    private static final String PREF_SESSION_START = "session_start_time";
    private Handler handler;
    private Runnable refreshRunnable;

    private RelativeLayout blocklist;
    private RelativeLayout passwordStrength;
    private RelativeLayout netSecurity;
    private RelativeLayout security;
    private static final int REFRESH_INTERVAL = 60000; // Refresh every 1 min

    // Member variables to store the current status
    private int currentFirewallState = -1; // -1 means uninitialized
    private int currentBlockedCount = -1;
    private int currentProtectedTime = -1;

    private static final int SECURITY_ACTIVITY_REQUEST_CODE = 1001;
    private TextView tvSecurityLevel, tvSecurityDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firewall);

        // Initialize OkHttpClient
        httpClient = new OkHttpClient();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("firewall_prefs", MODE_PRIVATE);

        //Initialize TextViews
        tvSecurityLevel = findViewById(R.id.tvSecurityLevel);
        tvSecurityDesc = findViewById(R.id.tvSecurityDesc);


        // Set up toolbar with back arrow
        toolbar = findViewById(R.id.toolbar);

        // Stok Token
        STOK = getIntent().getStringExtra("STOK");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize menu button from updated layout
        menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(Firewall.this, Menu_Blocklist.class);
            intent.putExtra("STOK", STOK);
            startActivity(intent);
        });

        // Initialize views from the updated layout
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        innerImageView = findViewById(R.id.innerImageView);
        smallTextView = findViewById(R.id.smallTextView);
        turnOffButton = findViewById(R.id.turnOffButton);
        gifTextView3 = findViewById(R.id.gifTextView3);
        textView0hr40m = findViewById(R.id.textView0hr40m);
        statusCard = findViewById(R.id.statusCard); // Now a FrameLayout instead of MaterialCardView

        // Store original background, toolbar, and status bar colors
        originalBackgroundColor = getResources().getColor(R.color.topColor);
        originalToolbarColor = getResources().getColor(R.color.topColor);
        originalStatusBarColor = getWindow().getStatusBarColor();

        // Load initial Lottie animation from local JSON
        lottieAnimationView.setAnimation(R.raw.firewall_ena);
        lottieAnimationView.playAnimation();

        // Set initial visibility
        innerImageView.setVisibility(View.GONE);
        smallTextView.setVisibility(View.GONE);

        //set onclick to view blocklist
        blocklist = findViewById(R.id.blocklist);
        blocklist.setOnClickListener(v -> {
            Intent intent = new Intent(Firewall.this, BlockedDevicesActivity.class);
            intent.putExtra("STOK", STOK);
            startActivity(intent);
        });

        //set onclick to view PassStrength
        passwordStrength = findViewById(R.id.passStrenght);
        passwordStrength.setOnClickListener(v -> {
            Intent intent = new Intent(Firewall.this, wifi_settings.class);
            intent.putExtra("STOK", STOK);
            startActivity(intent);
        });

        //set onclick to view network Security
        netSecurity = findViewById(R.id.networkSecurity);
        netSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(Firewall.this, pass_reset.class);
            intent.putExtra("STOK", STOK);
            startActivity(intent);
        });

        //set onclick to view Security
        security = findViewById(R.id.Security);
        security.setOnClickListener(v -> {
            Intent intent = new Intent(Firewall.this, Security.class);
            intent.putExtra("STOK", STOK);
            startActivityForResult(intent, SECURITY_ACTIVITY_REQUEST_CODE);
        });

        // Load initial security level
        loadSecurityLevel();





        // Toggle button click listener
        turnOffButton.setOnClickListener(v -> {
            if (turnOffButton.getText().toString().equals("Turn Off")) {
                showConfirmationDialog(); // Show the custom confirmation dialog
            } else {
                toggleFirewallState(); // Directly toggle the firewall state
            }
        });

        // Set up click listeners for the settings cards
        setupSettingsCardListeners();

        // Step 1: Load last stored data from SharedPreferences
        loadLastStoredData();

        // Step 2: Fetch latest data from API
        fetchFirewallStatus();

        // Initialize the Handler and Runnable for periodic refresh
        handler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Refresh firewall status every minute
                fetchFirewallStatus();
                handler.postDelayed(this, REFRESH_INTERVAL); // Re-run this task after the specified interval
            }
        };

        // Start periodic refresh
        handler.post(refreshRunnable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SECURITY_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            String selectedLevel = data.getStringExtra("selected_level");
            updateSecurityLevelUI(selectedLevel);
            saveSecurityLevel(selectedLevel);
        }
    }

    private void loadSecurityLevel() {
        SharedPreferences prefs = getSharedPreferences("SecurityPrefs", MODE_PRIVATE);
        String level = prefs.getString("security_level", "high");
        updateSecurityLevelUI(level);
    }

    private void saveSecurityLevel(String level) {
        SharedPreferences.Editor editor = getSharedPreferences("SecurityPrefs", MODE_PRIVATE).edit();
        editor.putString("security_level", level);
        editor.apply();
    }

    private void updateSecurityLevelUI(String level) {
        // Add animation
        tvSecurityLevel.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        tvSecurityDesc.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        switch (level) {
            case "high":
                tvSecurityLevel.setText("High");
                tvSecurityLevel.setTextColor(Color.parseColor("#4CAF50")); // Green
                tvSecurityDesc.setText("Block suspicious devices and blacklist, notify of new devices");
                break;
            case "medium":
                tvSecurityLevel.setText("Medium");
                tvSecurityLevel.setTextColor(Color.parseColor("#FF9800")); // Orange
                tvSecurityDesc.setText("Block blacklist, notify of suspicious devices");
                break;
            case "low":
                tvSecurityLevel.setText("Low");
                tvSecurityLevel.setTextColor(Color.parseColor("#F44336")); // Red
                tvSecurityDesc.setText("Block blacklist only");
                break;
        }
    }
    private void setupSettingsCardListeners() {
        // Find all the card views in the settings section
        MaterialCardView securityCard = findViewById(R.id.securityCard);
        MaterialCardView blocklistCard = findViewById(R.id.blocklistCard);
        MaterialCardView passwordCard = findViewById(R.id.passwordCard);
        MaterialCardView networkCard = findViewById(R.id.networkCard);

        // Set click listeners for each card
        securityCard.setOnClickListener(v -> {
            // Handle security card click
            // This can be implemented later with the specific navigation logic
        });

        blocklistCard.setOnClickListener(v -> {
            // Handle blocklist card click
            // This can be implemented later with the specific navigation logic
        });

        passwordCard.setOnClickListener(v -> {
            // Handle password strength card click
            // This can be implemented later with the specific navigation logic
        });

        networkCard.setOnClickListener(v -> {
            // Handle network security card click
            // This can be implemented later with the specific navigation logic
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to stop the periodic refresh when the activity is destroyed
        handler.removeCallbacks(refreshRunnable);
    }

    private void loadLastStoredData() {
        int lastFirewallState = sharedPreferences.getInt("firewall_state", 1);
        int lastBlockedCount = sharedPreferences.getInt("blocked_count", 0);
        long totalAccumulated = sharedPreferences.getLong(PREF_TOTAL_TIME, 0);

        if (lastFirewallState == 1) {
            updateUIForFirewallOn(lastBlockedCount, (int) totalAccumulated);
        } else {
            updateUIForFirewallOff(lastBlockedCount, (int) totalAccumulated);
        }

        currentFirewallState = lastFirewallState;
        currentBlockedCount = lastBlockedCount;
        currentProtectedTime = (int) totalAccumulated;
    }

    private void fetchFirewallStatus() {
        String url = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/xqnetwork/wifi_macfilter_info";

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        int enable = jsonResponse.getInt("enable");

                        JSONObject statistics = jsonResponse.getJSONArray("list").getJSONObject(0).getJSONObject("statistics");
                        int blockedCount = statistics.getInt("download");

                        runOnUiThread(() -> {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            long currentTime = System.currentTimeMillis();
                            long totalAccumulated = sharedPreferences.getLong(PREF_TOTAL_TIME, 0);
                            long sessionStart = sharedPreferences.getLong(PREF_SESSION_START, -1);

                            editor.putInt("firewall_state", enable);
                            editor.putInt("blocked_count", blockedCount);

                            if (enable == 1) {
                                if (sessionStart == -1) {
                                    sessionStart = currentTime;
                                    editor.putLong(PREF_SESSION_START, sessionStart);
                                } else {
                                    long elapsedTime = currentTime - sessionStart;
                                    totalAccumulated += elapsedTime / 1000;
                                    sessionStart = currentTime;
                                    editor.putLong(PREF_TOTAL_TIME, totalAccumulated);
                                    editor.putLong(PREF_SESSION_START, sessionStart);
                                }
                            } else {
                                if (sessionStart != -1) {
                                    long elapsedTime = currentTime - sessionStart;
                                    totalAccumulated += elapsedTime / 1000;
                                    editor.putLong(PREF_TOTAL_TIME, totalAccumulated);
                                    editor.remove(PREF_SESSION_START);
                                }
                            }

                            editor.apply();

                            if (enable == 1) {
                                updateUIForFirewallOn(blockedCount, (int) totalAccumulated);
                            } else {
                                updateUIForFirewallOff(blockedCount, (int) totalAccumulated);
                            }

                            updateBlockedAndProtectedCount(blockedCount, (int) totalAccumulated);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void updateBlockedAndProtectedCount(int blockedCount, int protectedTime) {
        textView0hr40m.setText(formatTime(protectedTime));
    }

    private void updateUIForFirewallOn(int blockedCount, int protectedTime) {
        // Update UI for firewall On state
        turnOffButton.setText("Turn Off");
        lottieAnimationView.setAnimation(R.raw.firewall_ena);
        lottieAnimationView.playAnimation();

        // Show existing views
        gifTextView3.setVisibility(View.VISIBLE);
        textView0hr40m.setVisibility(View.VISIBLE);

        // Update "Protected" text
        textView0hr40m.setText(formatTime(protectedTime));

        // Hide shield and small text
        innerImageView.setVisibility(View.GONE);
        smallTextView.setVisibility(View.GONE);

        // Revert colors to original
        statusCard.setBackgroundColor(originalBackgroundColor); // Changed from setCardBackgroundColor to setBackgroundColor
        toolbar.setBackgroundColor(originalToolbarColor);
        getWindow().setStatusBarColor(originalStatusBarColor);
    }

    private void updateUIForFirewallOff(int blockedCount, int protectedTime) {
        // Update UI for firewall Off state
        turnOffButton.setText("Turn On");
        lottieAnimationView.setAnimation(R.raw.firewall_dis);
        lottieAnimationView.playAnimation();

        // Hide existing views
        gifTextView3.setVisibility(View.GONE);
        textView0hr40m.setVisibility(View.GONE);

        // Show shield and small text
        innerImageView.setImageResource(R.drawable.shield);
        innerImageView.setVisibility(View.VISIBLE);
        smallTextView.setText("Firewall is off");
        smallTextView.setVisibility(View.VISIBLE);

        // Change background, toolbar, and status bar color to red
        int redColor = getResources().getColor(R.color.red);
        statusCard.setBackgroundColor(redColor); // Changed from setCardBackgroundColor to setBackgroundColor
        toolbar.setBackgroundColor(redColor);
        getWindow().setStatusBarColor(redColor);
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        return hours + "hr " + minutes + "m";
    }

    private void showConfirmationDialog() {
        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm, null);

        // Initialize dialog components
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogCancelButton = dialogView.findViewById(R.id.dialogCancelButton);
        Button dialogConfirmButton = dialogView.findViewById(R.id.dialogConfirmButton);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Transparent background
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation; // Apply animation
        dialog.show();

        // Set click listeners for the buttons
        dialogCancelButton.setOnClickListener(v -> {
            dialog.dismiss(); // Close the dialog
        });

        dialogConfirmButton.setOnClickListener(v -> {
            dialog.dismiss(); // Close the dialog
            toggleFirewallState(); // Proceed with the action
        });
    }

    private void toggleFirewallState() {
        int enable = turnOffButton.getText().toString().equals("Turn Off") ? 0 : 1;
        executeApiCall(enable);
    }

    private void executeApiCall(int enable) {
        String url = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/xqnetwork/set_wifi_macfilter?model=0&enable=" + enable;

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle the successful response
                    String responseBody = response.body().string();

                    // Save the updated firewall state in SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("firewall_state", enable);
                    editor.apply();

                    // Fetch the updated firewall status after the API call
                    fetchFirewallStatus();
                } else {
                    // Handle the error
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}