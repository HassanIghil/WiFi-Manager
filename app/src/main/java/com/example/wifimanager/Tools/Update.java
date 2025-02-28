package com.example.wifimanager.Tools;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.example.wifimanager.R;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Update extends AppCompatActivity {
    private static String STOK;
    private static int clicks = 0;
    private String routerName;
    private TextView versionTextView;
    private LottieAnimationView lottieAnimationView;
    private FrameLayout routerInfoSection;
    private Button updateButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Log the start of the activity
        Log.d("UpdateActivity", "Update activity started");

        // Retrieve the router's name from the Intent
        routerName = getIntent().getStringExtra("ROUTER_NAME");

        setContentView(R.layout.activity_update);
        updateButton = findViewById(R.id.update_button);
        versionTextView = findViewById(R.id.versionText);
        STOK = getIntent().getStringExtra("STOK"); // Retrieve the token

        // Initialize LottieAnimationView and sections
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        routerInfoSection = findViewById(R.id.routerInfoSection);

        // Log the STOK token for debugging
        Log.d("UpdateActivity", "STOK Token: " + STOK);

        // Set up toolbar with back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Display the router's name if available
        if (routerName != null) {
            TextView routerNameTextView = findViewById(R.id.routername); // Ensure this ID exists in your layout
            routerNameTextView.setText(routerName);
        }

        // Fetch the router version automatically when the activity is opened
        new FetchRouterVersionTask().execute();

        // Set button click listener to fetch update details
        updateButton.setOnClickListener(v -> {
            // Hide routerInfoSection and updateButton when the button is clicked
            routerInfoSection.setVisibility(View.GONE);
            updateButton.setVisibility(View.GONE);

            // Show Lottie animation
            lottieAnimationView.setVisibility(View.VISIBLE);

            // Call the API to get the router version
            new FetchRouterVersionTask().execute();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void update_click() {
        Toast.makeText(this, "Still no Updates...", Toast.LENGTH_SHORT).show();
    }

    public void update_click_more() {
        Toast.makeText(this, "Relax, if there's an update, we'll let you know", Toast.LENGTH_SHORT).show();
    }

    private class FetchRouterVersionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show Lottie animation and hide routerInfoSection and updateButton while loading
            lottieAnimationView.setVisibility(View.VISIBLE);
            routerInfoSection.setVisibility(View.GONE);
            updateButton.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection urlConnection = null;
            try {
                // Define the API URL
                String apiUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/xqsystem/check_rom_update";
                Log.d("UpdateActivity", "API URL: " + apiUrl); // Log the API URL

                // Open connection to the API
                URL url = new URL(apiUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                // Check if the connection is successful
                int responseCode = urlConnection.getResponseCode();
                Log.d("UpdateActivity", "Response Code: " + responseCode); // Log the response code

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the API response
                    InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    int data;
                    while ((data = inputStreamReader.read()) != -1) {
                        response.append((char) data);
                    }

                    // Log the raw JSON response for debugging
                    Log.d("UpdateActivity", "API Response: " + response.toString());

                    // Parse the JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());

                    // Check for code 1504
                    if (jsonResponse.has("code") && jsonResponse.getInt("code") == 1504) {
                        return "1504";
                    }

                    if (jsonResponse.has("version")) {
                        return jsonResponse.getString("version");
                    } else {
                        Log.e("UpdateActivity", "Version field not found in JSON response");
                        return null;
                    }
                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Handle invalid token error
                    Log.e("UpdateActivity", "Invalid token (401)");
                    return "401";
                } else {
                    Log.e("UpdateActivity", "HTTP Error: " + responseCode);
                    return null;
                }
            } catch (Exception e) {
                Log.e("UpdateActivity", "Error fetching router version", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect(); // Close the connection
                }
            }
        }


        @Override
        protected void onPostExecute(String result) {
            // Hide Lottie animation
            lottieAnimationView.setVisibility(View.GONE);

            if (result != null) {
                if (result.equals("401")) {
                    // Handle invalid token error
                    Toast.makeText(Update.this, "Invalid token. Please check your connection.", Toast.LENGTH_SHORT).show();
                } else if (result.equals("1504")) {
                    // If code is 1504, show the message and set the version to the latest available
                    Toast.makeText(Update.this, "Couldn't check for updates", Toast.LENGTH_SHORT).show();
                    versionTextView.setText("Version 3.0.45 | stable");
                    routerInfoSection.setVisibility(View.VISIBLE);
                    updateButton.setVisibility(View.VISIBLE);
                } else {
                    // Display the fetched version in the UI
                    versionTextView.setText("Version: " + result + " | stable");

                    // Show routerInfoSection and updateButton
                    routerInfoSection.setVisibility(View.VISIBLE);
                    updateButton.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(Update.this, "Failed to fetch version", Toast.LENGTH_SHORT).show();
            }
        }

    }
}
