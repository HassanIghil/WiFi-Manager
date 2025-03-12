package com.example.wifimanager.Tools;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Random;

public class Optimization extends AppCompatActivity {
    private CircularProgressIndicator circularProgressBar;
    private TextView progressPercentage;
    private TextView scanningStatus;
    private CircularProgressIndicator wifiQualityProgress;
    private CircularProgressIndicator signalStrengthProgress;
    private TextView waitingText;
    private MaterialButton cancelButton;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private int currentProgress = 0;
    private Random random = new Random();
    private boolean isScanActive = true;
    private boolean wifiQualityCompleted = false;
    private boolean signalStrengthCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_optimization);

        // Set up toolbar with back arrow (using standard Toolbar now)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        circularProgressBar = findViewById(R.id.circularProgressBar);
        progressPercentage = findViewById(R.id.progress_percentage);
        scanningStatus = findViewById(R.id.scanning_status);
        wifiQualityProgress = findViewById(R.id.wifi_quality_progress);
        signalStrengthProgress = findViewById(R.id.signal_strength_progress);
        waitingText = findViewById(R.id.waiting_to_check);
        cancelButton = findViewById(R.id.cancel_button);


        // Ensure signal strength progress indicator is properly initialized
        if (signalStrengthProgress != null) {
            signalStrengthProgress.setVisibility(View.GONE);
        }

        // Set up cancel button
        cancelButton.setOnClickListener(v -> {
            // Stop the scan animation
            isScanActive = false;
            progressHandler.removeCallbacksAndMessages(null);

            // If scan is complete, the button becomes "View Results"
            if (currentProgress >= 100) {
                // Handle view results action
                // For now, just go back
                onBackPressed();
            } else {
                onBackPressed();
            }
        });

        // Start the progress animation
        startProgressAnimation();
    }

    private void startProgressAnimation() {
        // Set initial values
        circularProgressBar.setProgress(currentProgress);
        progressPercentage.setText(currentProgress + "%");
        updateScanningStatus(currentProgress);

        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isScanActive) return;

                if (currentProgress < 100) {
                    // Determine next progress value (random increment)
                    int increment = random.nextInt(5) + 1;
                    int targetProgress = Math.min(currentProgress + increment, 100);

                    // Animate to the new progress value
                    ValueAnimator animator = ValueAnimator.ofInt(currentProgress, targetProgress);
                    animator.setDuration(800);  // Animation duration
                    animator.addUpdateListener(animation -> {
                        int animatedValue = (int) animation.getAnimatedValue();
                        circularProgressBar.setProgress(animatedValue);
                        progressPercentage.setText(animatedValue + "%");

                        // Update status text based on progress
                        updateScanningStatus(animatedValue);

                        // Update the item progress indicators based on scan progress
                        updateItemProgressIndicators(animatedValue);
                    });
                    animator.start();

                    // Save the new progress value
                    currentProgress = targetProgress;

                    // Calculate delay for next update (random for natural feel)
                    int delay = random.nextInt(1500) + 500; // Between 0.5-2 seconds

                    // Schedule next update
                    progressHandler.postDelayed(this, delay);
                } else {
                    // Scan completed
                    scanningStatus.setText("Optimized !");

                    // Ensure all progress indicators are replaced with check marks
                    replaceProgressWithCheckMarks();

                    // Change the cancel button to "View Results"
                    cancelButton.setText("Finish");
                }
            }
        }, 1000); // Start first update after 1 second
    }

    // Update the scanning status text based on progress
    private void updateScanningStatus(int progress) {
        if (progress < 30) {
            scanningStatus.setText("Scanning Wi-Fi channel...");
        } else if (progress < 60) {
            scanningStatus.setText("Analyzing network signals...");
        } else if (progress < 90) {
            scanningStatus.setText("Optimizing connection...");
        } else {
            scanningStatus.setText("Finalizing optimizations...");
        }
    }

    // Update the progress indicators for each item based on scan progress
    private void updateItemProgressIndicators(int progress) {
        // Safety checks to prevent crashes
        if (wifiQualityProgress == null || signalStrengthProgress == null || waitingText == null) {
            return;
        }

        // First stage: Wi-Fi quality check
        if (progress >= 45 && !wifiQualityCompleted && wifiQualityProgress.getVisibility() == View.VISIBLE) {
            try {
                // Replace the Wi-Fi quality progress indicator with check mark
                replaceProgressWithCheckMark(wifiQualityProgress);
                wifiQualityCompleted = true;

                // Immediately start the signal strength check
                waitingText.setVisibility(View.GONE);
                signalStrengthProgress.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                // Log error but continue execution
                e.printStackTrace();
            }
        }

        // Second stage: Signal strength check
        if (wifiQualityCompleted && progress >= 85 && !signalStrengthCompleted) {
            try {
                // Replace signal strength progress with check mark
                replaceProgressWithCheckMark(signalStrengthProgress);
                signalStrengthProgress.setVisibility(View.GONE);
                signalStrengthCompleted = true;

                // Hide the waiting text completely instead of showing "Optimized"
                waitingText.setVisibility(View.GONE);
            } catch (Exception e) {
                // Log error but continue execution
                e.printStackTrace();
            }
        }
    }

    // Replace a specific progress indicator with a check mark
    private void replaceProgressWithCheckMark(CircularProgressIndicator progressIndicator) {
        try {
            ViewGroup parent = (ViewGroup) progressIndicator.getParent();
            if (parent == null) return;

            int index = parent.indexOfChild(progressIndicator);
            if (index == -1) return;

            // Create an ImageView with check mark icon
            ImageView checkMark = new ImageView(this);
            checkMark.setImageResource(R.drawable.checked);
            // Set layout parameters to match the progress indicator
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(24), dpToPx(24));
            checkMark.setLayoutParams(params);

            // Remove the progress indicator and add the check mark
            parent.removeView(progressIndicator);
            parent.addView(checkMark, index);
        } catch (Exception e) {
            // Log error but continue execution
            e.printStackTrace();
        }
    }

    // Replace the signal strength waiting text with a check mark
    private void replaceWaitingWithCheckMark() {
        try {
            if (waitingText == null) return;

            ViewGroup parent = (ViewGroup) waitingText.getParent();
            if (parent == null) return;

            int index = parent.indexOfChild(waitingText);
            if (index == -1) return;

            // Create an ImageView with check mark icon
            ImageView checkMark = new ImageView(this);
            checkMark.setImageResource(R.drawable.checked);
            // Set layout parameters similar to waiting text
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(24), dpToPx(24));
            checkMark.setLayoutParams(params);

            // Remove the waiting text and add the check mark
            parent.removeView(waitingText);
            parent.addView(checkMark, index);
        } catch (Exception e) {
            // Log error but continue execution
            e.printStackTrace();
        }
    }

    // Replace all progress indicators with check marks at scan completion
    private void replaceProgressWithCheckMarks() {
        try {
            // Replace the WiFi quality progress if it hasn't been replaced yet
            if (wifiQualityProgress != null && wifiQualityProgress.getVisibility() == View.VISIBLE) {
                replaceProgressWithCheckMark(wifiQualityProgress);
            }

            // Replace signal strength progress if it's visible
            if (signalStrengthProgress != null && signalStrengthProgress.getVisibility() == View.VISIBLE) {
                replaceProgressWithCheckMark(signalStrengthProgress);
            }

            // Replace waiting text with check mark if it's visible
            if (waitingText != null && waitingText.getVisibility() == View.VISIBLE) {
                replaceWaitingWithCheckMark();
            }
        } catch (Exception e) {
            // Log error but continue execution
            e.printStackTrace();
        }
    }

    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop animations and remove callbacks to prevent memory leaks
        isScanActive = false;
        progressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Optionally pause the animation when activity is not visible
        isScanActive = false;
        progressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Optionally resume the animation if it was paused
        if (!isScanActive && currentProgress < 100) {
            isScanActive = true;
            startProgressAnimation();
        }
    }
}