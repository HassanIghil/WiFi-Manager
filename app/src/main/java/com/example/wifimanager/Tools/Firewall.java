package com.example.wifimanager.Tools;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.example.wifimanager.R;

public class Firewall extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firewall);

        // Set up toolbar with back arrow
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Apply window insets for full-screen view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
        turnOffButton.setOnClickListener(v -> toggleFirewallState());
    }

    private void toggleFirewallState() {
        if (turnOffButton.getText().toString().equals("Turn Off")) {
            // Switch to "Turn On" state
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
            int orangeColor = getResources().getColor(R.color.orange);
            animationContainer.setBackgroundColor(orangeColor);
            toolbar.setBackgroundColor(orangeColor);
            getWindow().setStatusBarColor(orangeColor);

            // Update button text
            turnOffButton.setText("Turn On");
        } else {
            // Switch back to "Turn Off" state
            lottieAnimationView.setAnimation(R.raw.firewall_ena);
            lottieAnimationView.playAnimation();

            // Show existing views
            gifTextView1.setVisibility(View.VISIBLE);
            gifTextView3.setVisibility(View.VISIBLE);
            textView106.setVisibility(View.VISIBLE);
            textView0hr40m.setVisibility(View.VISIBLE);

            // Hide shield and small text
            innerImageView.setVisibility(View.GONE);
            smallTextView.setVisibility(View.GONE);

            // Revert colors to original
            animationContainer.setBackgroundColor(originalBackgroundColor);
            toolbar.setBackgroundColor(originalToolbarColor);
            getWindow().setStatusBarColor(originalStatusBarColor);

            // Update button text
            turnOffButton.setText("Turn Off");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
