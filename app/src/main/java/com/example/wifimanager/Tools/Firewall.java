package com.example.wifimanager.Tools;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.example.wifimanager.R;
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
    private Button turnOffButton;
    private TextView gifTextView1, gifTextView3, textView106, textView0hr40m;
    private ConstraintLayout animationContainer;
    private Toolbar toolbar;

    private int originalBackgroundColor;
    private int originalToolbarColor;
    private int originalStatusBarColor;

    private OkHttpClient httpClient;
    private SharedPreferences sharedPreferences;

    private Handler handler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 60000; // Refresh every 1 min

    // Member variables to store the current status
    private int currentFirewallState = -1; // -1 means uninitialized
    private int currentBlockedCount = -1;
    private int currentProtectedTime = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firewall);

        // Initialize OkHttpClient
        httpClient = new OkHttpClient();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("firewall_prefs", MODE_PRIVATE);

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

        // Initialize views
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        innerImageView = findViewById(R.id.innerImageView);
        smallTextView = findViewById(R.id.smallTextView);
        turnOffButton = findViewById(R.id.turnOffButton);
        gifTextView1 = findViewById(R.id.gifTextView1);
        gifTextView3 = findViewById(R.id.gifTextView3);
        textView106 = findViewById(R.id.textView106);
        textView0hr40m = findViewById(R.id.textView0hr40m);
        animationContainer = findViewById(R.id.animationContainer);

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

        // Toggle button click listener
        turnOffButton.setOnClickListener(v -> {
            if (turnOffButton.getText().toString().equals("Turn Off")) {
                // Show confirmation dialog only if the button text is "Turn Off"
                showConfirmationDialog();
            } else {
                // If the button text is "Turn On", directly toggle the firewall state
                toggleFirewallState();
            }
        });

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
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to stop the periodic refresh when the activity is destroyed
        handler.removeCallbacks(refreshRunnable);
    }

    private void loadLastStoredData() {
        // Retrieve last stored data from SharedPreferences
        int lastFirewallState = sharedPreferences.getInt("firewall_state", 1); // Default to enabled
        int lastBlockedCount = sharedPreferences.getInt("blocked_count", 0); // Default to 0
        int lastProtectedTime = sharedPreferences.getInt("protected_time", 0); // Default to 0

        // Update UI with last stored data
        if (lastFirewallState == 1) {
            updateUIForFirewallOn(lastBlockedCount, lastProtectedTime);
        } else {
            updateUIForFirewallOff(lastBlockedCount, lastProtectedTime);
        }

        // Save the current status values
        currentFirewallState = lastFirewallState;
        currentBlockedCount = lastBlockedCount;
        currentProtectedTime = lastProtectedTime;
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

                        // Extract "Blocked" and "Protected" values from the API response
                        JSONObject statistics = jsonResponse.getJSONArray("list").getJSONObject(0).getJSONObject("statistics");
                        int blockedCount = statistics.getInt("download"); // Example: "Blocked 106"
                        int protectedTime = statistics.getInt("online");  // Example: "Protected 0hr 40m"

                        // Save the updated firewall state, blocked count, and protected time in SharedPreferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("firewall_state", enable);
                        editor.putInt("blocked_count", blockedCount);
                        editor.putInt("protected_time", protectedTime);
                        editor.apply();

                        runOnUiThread(() -> {
                            // If the state has changed, update the animation and UI
                            if (enable == 1) {
                                // Firewall is On
                                updateUIForFirewallOn(blockedCount, protectedTime);
                            } else {
                                // Firewall is Off
                                updateUIForFirewallOff(blockedCount, protectedTime);
                            }

                            // Update the Blocked and Protected count even if the animation does not change
                            updateBlockedAndProtectedCount(blockedCount, protectedTime);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Handle the error
                }
            }
        });
    }

    private void updateBlockedAndProtectedCount(int blockedCount, int protectedTime) {
        // Update "Blocked" and "Protected" text
        textView106.setText("" + blockedCount);
        textView0hr40m.setText(formatTime(protectedTime));
    }

    private void updateUIForFirewallOn(int blockedCount, int protectedTime) {
        // Update UI for firewall On state
        turnOffButton.setText("Turn Off");
        lottieAnimationView.setAnimation(R.raw.firewall_ena);
        lottieAnimationView.playAnimation();

        // Show existing views
        gifTextView1.setVisibility(View.VISIBLE);
        gifTextView3.setVisibility(View.VISIBLE);
        textView106.setVisibility(View.VISIBLE);
        textView0hr40m.setVisibility(View.VISIBLE);

        // Update "Blocked" and "Protected" text
        textView106.setText("" + blockedCount);
        textView0hr40m.setText(formatTime(protectedTime));

        // Hide shield and small text
        innerImageView.setVisibility(View.GONE);
        smallTextView.setVisibility(View.GONE);

        // Revert colors to original
        animationContainer.setBackgroundColor(originalBackgroundColor);
        toolbar.setBackgroundColor(originalToolbarColor);
        getWindow().setStatusBarColor(originalStatusBarColor);
    }

    private void updateUIForFirewallOff(int blockedCount, int protectedTime) {
        // Update UI for firewall Off state
        turnOffButton.setText("Turn On");
        lottieAnimationView.setAnimation(R.raw.firewall_dis);
        lottieAnimationView.playAnimation();

        // Hide existing views
        gifTextView1.setVisibility(View.GONE);
        gifTextView3.setVisibility(View.GONE);
        textView106.setVisibility(View.GONE);
        textView0hr40m.setVisibility(View.GONE);

        // Show shield and small text
        innerImageView.setImageResource(R.drawable.shield);
        innerImageView.setVisibility(View.VISIBLE);
        smallTextView.setText("Firewall is off");
        smallTextView.setVisibility(View.VISIBLE);

        // Change background, toolbar, and status bar color to orange
        int orangeColor = getResources().getColor(R.color.red);
        animationContainer.setBackgroundColor(orangeColor);
        toolbar.setBackgroundColor(orangeColor);
        getWindow().setStatusBarColor(orangeColor);
    }

    private String formatTime(int seconds) {
        // Convert seconds to "0hr 40m" format
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return hours + "hr " + minutes + "m";
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to turn off the firewall?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User confirmed, proceed to turn off the firewall
                toggleFirewallState();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User canceled, do nothing
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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
