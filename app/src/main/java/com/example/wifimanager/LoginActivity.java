package com.example.wifimanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity to full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_login);

        passwordInput = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(view -> {
            String password = passwordInput.getText().toString().trim();
            if (!password.isEmpty()) {
                loginToRouter(password);
            } else {
                Toast.makeText(this, "Enter your password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginToRouter(String password) {
        new Thread(() -> {
            try {
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
                    String token = loginResponse.getToken();
                    if (token != null && !token.isEmpty()) {
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

                Gson gson = new Gson();
                RouterNameResponse routerNameResponse = gson.fromJson(response.toString(), RouterNameResponse.class);

                if (routerNameResponse != null && routerNameResponse.getName() != null) {
                    String routerName = routerNameResponse.getName();
                    runOnUiThread(() -> {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("ROUTER_NAME", routerName);
                        intent.putExtra("STOK", token);
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