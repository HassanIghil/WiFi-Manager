package com.example.wifimanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {    private static final String TAG = "LoginActivity";
    private EditText passwordEditText;
    private String token;
    private View loadingOverlay;
    private com.google.android.material.textfield.TextInputLayout passwordLayout;
    private com.airbnb.lottie.LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        setContentView(R.layout.activity_login);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        passwordEditText = findViewById(R.id.password);
        passwordLayout = findViewById(R.id.passwordLayout);

        // Clear error when user starts typing
        passwordEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordLayout.setError(null);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        findViewById(R.id.loginButton).setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (!password.isEmpty()) {
                login(password);
            } else {
                passwordLayout.setError("Password is required");
            }
        });
    }

    private void showLoading() {
        runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.VISIBLE);
            findViewById(R.id.loginButton).setEnabled(false);
            passwordEditText.setEnabled(false);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.GONE);
            findViewById(R.id.loginButton).setEnabled(true);
            passwordEditText.setEnabled(true);
        });
    }

    private void login(String password) {
        showLoading();
        new Thread(() -> {
            try {                URI uri = new URI("http://192.168.31.1/cgi-bin/luci/api/xqsystem/login");
                URL url = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String data = "username=admin&password=" + password;
                connection.getOutputStream().write(data.getBytes());

                int responseCode = connection.getResponseCode();                if (responseCode != HttpURLConnection.HTTP_OK) {
                    hideLoading();
                    runOnUiThread(() -> {
                        passwordLayout.setError("Invalid password. Please check your credentials and try again.");
                    });
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

                if (loginResponse != null && loginResponse.getToken() != null) {
                    token = loginResponse.getToken();
                    fetchRouterName(token);                } else {
                    hideLoading();
                    runOnUiThread(() -> {
                        passwordLayout.setError("Login failed. Please check your password and try again.");
                    });
                }} catch (Exception e) {
                e.printStackTrace();
                hideLoading();
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }finally {
                hideLoading();
            }
        }).start();
    }

    private void fetchRouterName(String token) {
        new Thread(() -> {
            try {                URI uri = new URI("http://192.168.31.1/cgi-bin/luci/;stok=" + token + "/api/misystem/router_name");
                URL url = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();                if (responseCode != HttpURLConnection.HTTP_OK) {
                    hideLoading();
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

                    // Navigate to MainActivity with just the current token and router name
                    runOnUiThread(() -> {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("ROUTER_NAME", routerName);
                        intent.putExtra("STOK", token);
                        startActivity(intent);
                        finish();
                    });                } else {
                    hideLoading();
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Router name not found", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to fetch router name: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
