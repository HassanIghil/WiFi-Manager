package com.example.wifimanager.Settings_Components;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.wifimanager.R;

import org.json.JSONException;
import org.json.JSONObject;

public class reboot_settings extends AppCompatActivity {

    private RequestQueue requestQueue;
    private String stok;
    private LottieAnimationView rebootAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reboot_settings);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Get STOK from intent - add null check
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            stok = extras.getString("STOK");
            if (stok == null || stok.isEmpty()) {
                Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
                finish(); // Close activity if no token
                return;
            }
        } else {
            Toast.makeText(this, "Authentication token is missing", Toast.LENGTH_LONG).show();
            finish(); // Close activity if no extras
            return;
        }

        // Set up toolbar with back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize and setup Lottie animation
        setupLottieAnimation();

        Button rebootButton = findViewById(R.id.rebootButton);
        rebootButton.setOnClickListener(v -> showConfirmationDialog());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupLottieAnimation() {
        rebootAnimationView = findViewById(R.id.rebootAnimationView);

        // Load animation directly from raw resources like in Firewall activity
        rebootAnimationView.setAnimation(R.raw.reboot);
        rebootAnimationView.playAnimation();
    }

    private void showConfirmationDialog() {
        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.reboot_dialog, null);

        // Initialize dialog components
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogCancelButton = dialogView.findViewById(R.id.dialogCancelButton);
        Button dialogConfirmButton = dialogView.findViewById(R.id.dialogConfirmButton);

        // Customize the dialog content
        dialogTitle.setText("Confirm Reboot");
        dialogMessage.setText("Are you sure you want to reboot the router? All connected devices will be temporarily disconnected.");

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Transparent background
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation; // Apply animation
        dialog.show();

        // Set click listeners for the buttons
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());

        dialogConfirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            rebootRouter(); // Proceed with reboot
        });
    }

    private void rebootRouter() {
        // Double-check stok is available
        if (stok == null || stok.isEmpty()) {
            Toast.makeText(this, "Router authentication failed - no token", Toast.LENGTH_SHORT).show();
            return;
        }

        String rebootUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/xqsystem/reboot?client=web";

        StringRequest rebootRequest = new StringRequest(Request.Method.GET, rebootUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getInt("code") == 0) {
                            Toast.makeText(this, "Router is rebooting...", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Reboot command sent but may not have succeeded", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Reboot command sent but response was invalid", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Reboot command sent but connection was interrupted", Toast.LENGTH_LONG).show();
                });

        requestQueue.add(rebootRequest);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }

        // Make sure to cancel the animation when the activity stops
        if (rebootAnimationView != null && rebootAnimationView.isAnimating()) {
            rebootAnimationView.cancelAnimation();
        }
    }
}