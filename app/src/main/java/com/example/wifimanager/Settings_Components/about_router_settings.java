package com.example.wifimanager.Settings_Components;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;

public class about_router_settings extends AppCompatActivity {

    private Button btnCheckUpdates;
    private LinearLayout locationSelector;
    private TextView locationTextView; // TextView showing current location

    private String currentLocation = "Home"; // Default location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_router_settings);

        // Initialize views
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates);
        locationSelector = findViewById(R.id.locationSelector);
        locationTextView = findViewById(R.id.locationText);

        // Set up toolbar with back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Set up toolbar navigation
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Handle window insets for proper layout with system UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Check for updates button click listener
        btnCheckUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForUpdates();
            }
        });

        // Location selector click listener
        locationSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocationDialog();
            }
        });
    }

    private void checkForUpdates() {
        // Show a progress dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Checking for Updates");
        builder.setMessage("Looking for system updates...");
        builder.setCancelable(false);

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        // Simulate checking for updates (replace with actual implementation)
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();

                // Show result dialog
                AlertDialog.Builder resultBuilder = new AlertDialog.Builder(about_router_settings.this);
                resultBuilder.setTitle("Update Check Complete");
                resultBuilder.setMessage("Your router software is up to date (Version 3.0.45)");
                resultBuilder.setPositiveButton("OK", null);
                resultBuilder.show();
            }
        }, 2000); // 2 second delay to simulate network request
    }

    private void showLocationDialog() {
        // Create custom dialog
        final Dialog locationDialog = new Dialog(this);
        locationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        locationDialog.setContentView(R.layout.location_dialog);

        // Set dialog width to match parent
        Window window = locationDialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        // Get views from dialog
        LinearLayout optionHome = locationDialog.findViewById(R.id.optionHome);
        LinearLayout optionWork = locationDialog.findViewById(R.id.optionWork);
        LinearLayout optionCustom = locationDialog.findViewById(R.id.optionCustom);
        Button btnCancel = locationDialog.findViewById(R.id.btnCancel);
        Button btnOk = locationDialog.findViewById(R.id.btnOk);

        // Track selected option
        final String[] selectedLocation = {currentLocation};

        // Set click listeners for options
        optionHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update UI to show Home is selected
                updateSelectionUI(locationDialog, "Home");
                selectedLocation[0] = "Home";
            }
        });

        optionWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update UI to show Work is selected
                updateSelectionUI(locationDialog, "Work");
                selectedLocation[0] = "Work";
            }
        });

        optionCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show custom location input dialog
                locationDialog.dismiss();
                showCustomLocationDialog();
            }
        });

        // Set click listeners for buttons
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationDialog.dismiss();
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save selected location
                currentLocation = selectedLocation[0];
                updateLocationDisplay();

                Toast.makeText(
                        about_router_settings.this,
                        "Location set to: " + currentLocation,
                        Toast.LENGTH_SHORT
                ).show();

                locationDialog.dismiss();
            }
        });

        // Initialize the dialog to show current selection
        updateSelectionUI(locationDialog, currentLocation);

        locationDialog.show();
    }

    private void updateSelectionUI(Dialog dialog, String selectedOption) {
        LinearLayout optionHome = dialog.findViewById(R.id.optionHome);
        LinearLayout optionWork = dialog.findViewById(R.id.optionWork);

        // Get TextViews from options
        TextView homeText = (TextView) optionHome.getChildAt(1);
        TextView workText = (TextView) optionWork.getChildAt(1);

        // Get check mark ImageView and Space
        View homeFirstChild = optionHome.getChildAt(0);
        View workFirstChild = optionWork.getChildAt(0);

        // Reset all to unselected state
        homeText.setTextColor(getResources().getColor(R.color.textPrimary));
        workText.setTextColor(getResources().getColor(R.color.textPrimary));

        // Update selection based on option
        if (selectedOption.equals("Home")) {
            homeText.setTextColor(getResources().getColor(R.color.accentColor));

            // Show checkmark for Home if it exists
            if (homeFirstChild instanceof ImageView) {
                homeFirstChild.setVisibility(View.VISIBLE);
                ((ImageView) homeFirstChild).setColorFilter(getResources().getColor(R.color.accentColor));
            }

            // Make sure Work option doesn't show checkmark
            if (workFirstChild instanceof ImageView) {
                workFirstChild.setVisibility(View.INVISIBLE);
            }
        } else if (selectedOption.equals("Work")) {
            workText.setTextColor(getResources().getColor(R.color.accentColor));

            // Hide home checkmark if it exists
            if (homeFirstChild instanceof ImageView) {
                homeFirstChild.setVisibility(View.INVISIBLE);
            }

            // Show checkmark for Work
            if (workFirstChild instanceof ImageView) {
                workFirstChild.setVisibility(View.VISIBLE);
                ((ImageView) workFirstChild).setColorFilter(getResources().getColor(R.color.accentColor));
            } else if (workFirstChild instanceof Space) {
                // If Work has Space instead of ImageView, replace it with ImageView
                optionWork.removeViewAt(0);

                ImageView checkMark = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        (int) getResources().getDimension(R.dimen.check_icon_size),
                        (int) getResources().getDimension(R.dimen.check_icon_size));
                checkMark.setLayoutParams(params);
                checkMark.setImageResource(R.drawable.right);
                checkMark.setColorFilter(getResources().getColor(R.color.accentColor));

                optionWork.addView(checkMark, 0);
            }
        }
    }

    private void showCustomLocationDialog() {
        // Show dialog with EditText for custom location name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Custom Location");

        // Create an EditText to get user input
        final EditText input = new EditText(this);
        input.setHint("Location name");
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String customLocation = input.getText().toString().trim();
                if (!customLocation.isEmpty()) {
                    currentLocation = customLocation;
                    updateLocationDisplay();

                    Toast.makeText(
                            about_router_settings.this,
                            "Location set to: " + customLocation,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // If cancelled, show main location dialog again
                showLocationDialog();
            }
        });
        builder.show();
    }

    private void updateLocationDisplay() {
        // Update the TextView that shows the current location
        if (locationTextView != null) {
            locationTextView.setText(currentLocation);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}