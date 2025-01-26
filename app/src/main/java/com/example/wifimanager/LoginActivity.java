package com.example.wifimanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordInput;
    private Button loginButton;
    private final String STOK = "761be1984b53fcb8827421af299bdf88";  // Provided token
    private final String ROUTER_URL = "http://192.168.31.1/cgi-bin/luci";  // Router URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        passwordInput = findViewById(R.id.password);  // EditText for password
        loginButton = findViewById(R.id.loginButton);  // Button to trigger login

        loginButton.setOnClickListener(view -> {
            String password = passwordInput.getText().toString().trim();
            if (!password.isEmpty()) {
                loginToRouter(password);  // Call the login method
            } else {
                Toast.makeText(this, "Enter your password", Toast.LENGTH_SHORT).show();  // Error message if password is empty
            }
        });
    }

    private void loginToRouter(String password) {
        new Thread(() -> {
            try {
                // Login API URL
                String urlStr = "http://192.168.31.1/cgi-bin/luci/api/xqsystem/login";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String params = "username=admin&password=" + password;

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = params.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed with response code: " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Gson gson = new Gson();
                LoginResponse loginResponse = gson.fromJson(response.toString(), LoginResponse.class);

                if (loginResponse.getCode() == 0) {
                    // Login successful, extract token
                    String token = loginResponse.getToken();
                    if (token != null && !token.isEmpty()) {
                        // Fetch the router name using the token
                        fetchRouterName(token);
                    } else {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to extract token", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: Invalid credentials or response error", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void fetchRouterName(String token) {
        new Thread(() -> {
            try {
                // Use the token to call the API for router name
                String urlStr = "http://192.168.31.1/cgi-bin/luci/;stok=" + token + "/api/misystem/router_name";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to fetch router name. Response code: " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response to get router name using RouterNameResponse
                Gson gson = new Gson();
                RouterNameResponse routerNameResponse = gson.fromJson(response.toString(), RouterNameResponse.class);

                if (routerNameResponse != null && routerNameResponse.getName() != null) {
                    String routerName = routerNameResponse.getName();
                    // Pass the router name to MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("ROUTER_NAME", routerName);
                    startActivity(intent);
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Router name not found", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to fetch router name: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }



}
