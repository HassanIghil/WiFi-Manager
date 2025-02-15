package com.example.wifimanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText passwordEditText;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        passwordEditText = findViewById(R.id.password);

        // Handle login
        findViewById(R.id.loginButton).setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (!password.isEmpty()) {
                login(password);
            } else {
                Toast.makeText(LoginActivity.this, "Password is required", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void login(String password) {
        new Thread(() -> {
            try {
                String urlStr = "http://192.168.31.1/cgi-bin/luci/api/xqsystem/login";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                // Sending login data
                String data = "username=admin&password=" + password;
                connection.getOutputStream().write(data.getBytes());

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed. Response code: " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parsing response to extract token
                Gson gson = new Gson();
                LoginResponse loginResponse = gson.fromJson(response.toString(), LoginResponse.class);

                if (loginResponse != null && loginResponse.getToken() != null) {
                    token = loginResponse.getToken();
                    fetchRouterName(token);
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed. No token received.", Toast.LENGTH_SHORT).show());
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

                // Parsing router name from the response
                Gson gson = new Gson();
                RouterNameResponse routerNameResponse = gson.fromJson(response.toString(), RouterNameResponse.class);

                if (routerNameResponse != null && routerNameResponse.getName() != null) {
                    String routerName = routerNameResponse.getName();
                    runOnUiThread(() -> {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("ROUTER_NAME", routerName); // Pass router name
                        intent.putExtra("STOK", token); // Pass token
                        startActivity(intent);
                        finish(); // Finish LoginActivity to prevent going back to it
                    });
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
