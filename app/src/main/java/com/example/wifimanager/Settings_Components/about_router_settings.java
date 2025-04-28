package com.example.wifimanager.Settings_Components;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.wifimanager.R;
import com.example.wifimanager.database.DatabaseHelper;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class about_router_settings extends AppCompatActivity {

    private Button btnCheckUpdates;
    private Button btnSave;
    private LinearLayout locationSelector;
    private EditText nameSelector;
    private TextView locationTextView;
    private TextView routerNameTextView;
    private RequestQueue requestQueue;
    private String stok;
    private DatabaseHelper dbHelper;

    private static final String TABLE_NAME = DatabaseHelper.TABLE_PREFERENCES;
    private static final String KEY_ROUTER_NAME = "router_name";
    private static final String KEY_ROUTER_LOCATION = "router_location";

    private String currentLocation = "";
    private String currentRouterName = "";
    private String tempLocation = "";
    private String tempRouterName = "";
    private boolean hasUnsavedChanges = false;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_router_settings);

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper.getInstance(this);

        // Get STOK from intent
        stok = getIntent().getStringExtra("STOK");
        if (stok == null) {
            Toast.makeText(this, "Authentication token missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Initialize views
        initializeViews();
        
        // Set up toolbar with back arrow
        setupToolbar();

        // Handle window insets for proper layout with system UI
        setupWindowInsets();

        // Fetch current router name and location
        fetchRouterInfo();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        nameSelector = findViewById(R.id.routernameSelector);
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates);
        btnSave = findViewById(R.id.btnSave);
        locationSelector = findViewById(R.id.locationSelector);
        locationTextView = findViewById(R.id.locationText);
        routerNameTextView = findViewById(R.id.routernameSelector);
        
        // Initially disable save button
        btnSave.setEnabled(false);
    }

    private void fetchRouterInfo() {
        String url = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/misystem/router_name";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getInt("code") == 0) {
                            currentRouterName = response.getString("name");
                            currentLocation = response.getString("locale");
                            tempRouterName = currentRouterName;
                            tempLocation = currentLocation;
                            
                            // Update UI
                            routerNameTextView.setText(currentRouterName);
                            locationTextView.setText(currentLocation);
                        } else {
                            Toast.makeText(this, "Failed to fetch router info", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing router info", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error fetching router info", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void setupClickListeners() {
        // Check for updates button click listener
        btnCheckUpdates.setOnClickListener(v -> checkForUpdates());

        // Location selector click listener
        locationSelector.setOnClickListener(v -> showLocationDialog());

        // Router name text change listener
        nameSelector.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newName = s.toString().trim();
                if (!newName.equals(currentRouterName)) {
                    tempRouterName = newName;
                    hasUnsavedChanges = true;
                    btnSave.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Save button click listener
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        if (!hasUnsavedChanges) {
            return;
        }

        String url = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/misystem/set_router_name";
        
        Map<String, String> params = new HashMap<>();
        params.put("name", tempRouterName);
        params.put("locale", tempLocation);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                response -> {
                    try {
                        if (response.getInt("code") == 0) {
                            // Update current values
                            currentRouterName = tempRouterName;
                            currentLocation = tempLocation;
                            
                            // Save to database
                            dbHelper.putString(TABLE_NAME, KEY_ROUTER_NAME, currentRouterName);
                            dbHelper.putString(TABLE_NAME, KEY_ROUTER_LOCATION, currentLocation);

                            // Send broadcast to update Home screen
                            Intent intent = new Intent("ROUTER_NAME_UPDATED");
                            intent.putExtra("router_name", currentRouterName);
                            sendBroadcast(intent);

                            hasUnsavedChanges = false;
                            btnSave.setEnabled(false);
                            Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show();
                            // Revert temporary changes
                            tempRouterName = currentRouterName;
                            tempLocation = currentLocation;
                            updateUI();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error saving changes", Toast.LENGTH_SHORT).show();
                        // Revert temporary changes
                        tempRouterName = currentRouterName;
                        tempLocation = currentLocation;
                        updateUI();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error saving changes", Toast.LENGTH_SHORT).show();
                    // Revert temporary changes
                    tempRouterName = currentRouterName;
                    tempLocation = currentLocation;
                    updateUI();
                }) {
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }

            @Override
            public byte[] getBody() {
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    if (postData.length() > 0) postData.append('&');
                    postData.append(param.getKey()).append('=').append(param.getValue());
                }
                return postData.toString().getBytes(StandardCharsets.UTF_8);
            }
        };

        requestQueue.add(request);
    }

    private void updateLocationFromDialog(String newLocation) {
        if (!newLocation.equals(currentLocation)) {
            tempLocation = newLocation;
            locationTextView.setText(newLocation);
            hasUnsavedChanges = true;
            btnSave.setEnabled(true);
        }
    }

    private void updateUI() {
        routerNameTextView.setText(tempRouterName);
        locationTextView.setText(tempLocation);
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Do you want to save them before leaving?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        saveChanges();
                        super.onBackPressed();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> super.onBackPressed())
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
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
        new android.os.Handler().postDelayed(() -> {
            progressDialog.dismiss();

            // Show result dialog
            AlertDialog.Builder resultBuilder = new AlertDialog.Builder(about_router_settings.this);
            resultBuilder.setTitle("Update Check Complete");
            resultBuilder.setMessage("Your router software is up to date (Version 3.0.45)");
            resultBuilder.setPositiveButton("OK", null);
            resultBuilder.show();
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
        optionHome.setOnClickListener(v -> {
            // Update UI to show Home is selected
            updateSelectionUI(locationDialog, "Home");
            selectedLocation[0] = "Home";
        });

        optionWork.setOnClickListener(v -> {
            // Update UI to show Work is selected
            updateSelectionUI(locationDialog, "Work");
            selectedLocation[0] = "Work";
        });

        optionCustom.setOnClickListener(v -> {
            // Show custom location input dialog
            locationDialog.dismiss();
            showCustomLocationDialog();
        });

        // Set click listeners for buttons
        btnCancel.setOnClickListener(v -> locationDialog.dismiss());

        btnOk.setOnClickListener(v -> {
            // Save selected location
            updateLocationFromDialog(selectedLocation[0]);

            Toast.makeText(
                    about_router_settings.this,
                    "Location set to: " + selectedLocation[0],
                    Toast.LENGTH_SHORT
            ).show();

            locationDialog.dismiss();
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

        final EditText input = new EditText(this);
        input.setHint("Location name");
        input.setText(currentLocation);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String customLocation = input.getText().toString().trim();
            if (!customLocation.isEmpty()) {
                updateLocationFromDialog(customLocation);
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            showLocationDialog();
        });
        
        builder.show();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}