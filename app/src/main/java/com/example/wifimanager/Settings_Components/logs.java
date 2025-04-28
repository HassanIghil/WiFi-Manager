package com.example.wifimanager.Settings_Components;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class logs extends AppCompatActivity {

    // Permission codes
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_CODE = 101;

    // Views
    private Button downloadButton;
    private CircularProgressIndicator circularProgress;
    private LinearProgressIndicator linearProgress;
    private TextView progressText;
    private LinearLayout progressContainer;

    // Network
    private String stok;
    private HttpURLConnection connection;

    // Dialog reference
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

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

        initViews();
        checkAuthToken();
        setupToolbar();
    }

    private void initViews() {
        downloadButton = findViewById(R.id.downloadLogsButton);
        circularProgress = findViewById(R.id.circularProgress);
        linearProgress = findViewById(R.id.linearProgress);
        progressText = findViewById(R.id.progressText);
        progressContainer = findViewById(R.id.progressContainer);

        downloadButton.setOnClickListener(v -> verifyPermissions());
    }

    private void checkAuthToken() {
        Bundle extras = getIntent().getExtras();
        if (extras == null || extras.getString("STOK") == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_LONG).show();
            finish();
        } else {
            stok = extras.getString("STOK");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void verifyPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                startDownload();
            } else {
                requestManageStorage();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    private void requestManageStorage() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, MANAGE_STORAGE_CODE);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                startDownload();
            } else {
                showPermissionDialog();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                showPermissionDialog();
            }
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Needed")
                .setMessage("Storage access is required to save log files")
                .setPositiveButton("Settings", (d, w) -> openSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + getPackageName())));
    }

    private void startDownload() {
        progressContainer.setVisibility(View.VISIBLE);
        circularProgress.setVisibility(View.VISIBLE);
        linearProgress.setVisibility(View.GONE);
        progressText.setText("Requesting log file...");
        downloadButton.setEnabled(false);

        new Thread(() -> {
            String logFilePath = null;
            HttpURLConnection apiConnection = null;

            try {
                // Step 1: Get the log file path from the API
                String apiUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/misystem/sys_log";
                apiConnection = (HttpURLConnection) new URL(apiUrl).openConnection();
                apiConnection.setRequestMethod("GET");
                apiConnection.connect();

                if (apiConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("API request failed: " + apiConnection.getResponseCode());
                }

                // Read the JSON response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(apiConnection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Parse the JSON to get the log file path
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("path") && jsonResponse.getInt("code") == 0) {
                    logFilePath = jsonResponse.getString("path");
                } else {
                    throw new IOException("Invalid API response: " + response);
                }

                // Step 2: Download the actual log file
                runOnUiThread(() -> progressText.setText("Starting download..."));

                // Construct URL for the actual log file
                String fileUrl = "http://" + logFilePath;
                downloadLogFile(fileUrl);

            } catch (IOException | JSONException e) {
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> showError(errorMsg));
            } finally {
                if (apiConnection != null) apiConnection.disconnect();
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void downloadLogFile(String fileUrl) {
        try {
            connection = (HttpURLConnection) new URL(fileUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("File download failed: " + connection.getResponseCode());
            }

            // Switch to determinate progress
            runOnUiThread(() -> {
                circularProgress.setVisibility(View.GONE);
                linearProgress.setVisibility(View.VISIBLE);
                linearProgress.setProgressCompat(0, true);
            });

            // Create output file with .tar.gz extension
            File outputFile = createLogFile();
            int fileSize = connection.getContentLength();

            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    if (isFinishing()) break;

                    output.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    if (fileSize > 0) {
                        final int progress = (int) (totalRead * 100 / fileSize);
                        runOnUiThread(() -> updateProgress(progress));
                    }
                }
            }

            runOnUiThread(() -> showModernDownloadComplete(outputFile));
        } catch (Exception e) {
            runOnUiThread(() -> showError(e.getMessage()));
        } finally {
            if (connection != null) connection.disconnect();
            runOnUiThread(() -> downloadButton.setEnabled(true));
        }
    }

    private File createLogFile() throws IOException {
        File dir = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) :
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File logsDir = new File(dir, "RouterLogs");
        if (!logsDir.exists()) logsDir.mkdirs();

        // Add timestamp to filename in readable format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault());
        String timeStamp = sdf.format(new Date());

        // Use .tar.gz extension since that's what the router provides
        return new File(logsDir, "router_logs_" + timeStamp + ".tar.gz");
    }

    private void updateProgress(int progress) {
        linearProgress.setProgressCompat(progress, true);
        progressText.setText(String.format(Locale.getDefault(), "Downloading... %d%%", progress));
    }

    private void showModernDownloadComplete(File file) {
        progressContainer.setVisibility(View.GONE);
        downloadButton.setText("Download Complete");

        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_complete, null);

        // Alternatively, if you don't want to create a new layout file, create the dialog programmatically:
        if (dialogView == null) {
            dialogView = createDialogViewProgrammatically(file);
        } else {
            // Setup views from the layout
            TextView titleText = dialogView.findViewById(R.id.dialogTitle);
            TextView messageText = dialogView.findViewById(R.id.dialogMessage);
            TextView filePathText = dialogView.findViewById(R.id.filePath);
            TextView fileSizeText = dialogView.findViewById(R.id.fileSize);
            MaterialButton openButton = dialogView.findViewById(R.id.openButton);
            MaterialButton shareButton = dialogView.findViewById(R.id.shareButton);
            MaterialButton closeButton = dialogView.findViewById(R.id.closeButton);

            // Set content
            filePathText.setText(file.getAbsolutePath());
            fileSizeText.setText(formatFileSize(file.length()));

            // Setup buttons
            openButton.setOnClickListener(v -> {
                openLogFile(file);
                if (dialog != null) dialog.dismiss();
            });

            shareButton.setOnClickListener(v -> {
                shareLogFile(file);
                if (dialog != null) dialog.dismiss();
            });

            closeButton.setOnClickListener(v -> {
                if (dialog != null) dialog.dismiss();
            });
        }

        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private View createDialogViewProgrammatically(File file) {
        // Create the main container
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(40, 40, 40, 24);

        // Create MaterialCardView as container
        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setRadius(16);
        cardView.setCardElevation(10);

        // Create linear layout for card content
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(24, 24, 24, 24);

        // Title
        TextView titleText = new TextView(this);
        titleText.setText("Download Complete");
        titleText.setTextSize(20);
        titleText.setTextColor(getResources().getColor(android.R.color.black));
        titleText.setPadding(0, 0, 0, 16);

        // Message
        TextView messageText = new TextView(this);
        messageText.setText("Log file saved successfully:");
        messageText.setTextSize(16);
        messageText.setPadding(0, 0, 0, 16);

        // File details container
        LinearLayout detailsContainer = new LinearLayout(this);
        detailsContainer.setOrientation(LinearLayout.VERTICAL);
        detailsContainer.setPadding(16, 16, 16, 16);
        detailsContainer.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // File path
        TextView filePathTitle = new TextView(this);
        filePathTitle.setText("Location:");
        filePathTitle.setTextSize(14);
        filePathTitle.setTextColor(Color.parseColor("#757575"));

        TextView filePathText = new TextView(this);
        filePathText.setText(file.getAbsolutePath());
        filePathText.setTextSize(14);
        filePathText.setPadding(0, 4, 0, 16);

        // File size
        TextView fileSizeTitle = new TextView(this);
        fileSizeTitle.setText("Size:");
        fileSizeTitle.setTextSize(14);
        fileSizeTitle.setTextColor(Color.parseColor("#757575"));

        TextView fileSizeText = new TextView(this);
        fileSizeText.setText(formatFileSize(file.length()));
        fileSizeText.setTextSize(14);
        fileSizeText.setPadding(0, 4, 0, 0);

        // Add file details to container
        detailsContainer.addView(filePathTitle);
        detailsContainer.addView(filePathText);
        detailsContainer.addView(fileSizeTitle);
        detailsContainer.addView(fileSizeText);

        // Buttons container
        LinearLayout buttonsContainer = new LinearLayout(this);
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonsContainer.setPadding(0, 24, 0, 0);

        // Open button
        MaterialButton openButton = new MaterialButton(this);
        openButton.setText("Open");
        openButton.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        openParams.setMarginEnd(8);
        openButton.setLayoutParams(openParams);
        openButton.setOnClickListener(v -> {
            openLogFile(file);
            if (dialog != null) dialog.dismiss();
        });

        // Share button
        MaterialButton shareButton = new MaterialButton(this);
        shareButton.setText("Share");
        shareButton.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        shareParams.setMarginStart(8);
        shareParams.setMarginEnd(8);
        shareButton.setLayoutParams(shareParams);
        shareButton.setOnClickListener(v -> {
            shareLogFile(file);
            if (dialog != null) dialog.dismiss();
        });

        // Close button
        MaterialButton closeButton = new MaterialButton(this);
        closeButton.setText("Close");
        closeButton.setTextColor(Color.BLACK);
        closeButton.setBackgroundColor(Color.parseColor("#E0E0E0"));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        closeParams.setMarginStart(8);
        closeButton.setLayoutParams(closeParams);
        closeButton.setOnClickListener(v -> {
            if (dialog != null) dialog.dismiss();
        });

        // Add buttons to container
        buttonsContainer.addView(openButton);
        buttonsContainer.addView(shareButton);
        buttonsContainer.addView(closeButton);

        // Add all views to content layout
        contentLayout.addView(titleText);
        contentLayout.addView(messageText);
        contentLayout.addView(detailsContainer);
        contentLayout.addView(buttonsContainer);

        // Add content layout to card
        cardView.addView(contentLayout);

        // Add card to main container
        mainContainer.addView(cardView);

        return mainContainer;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void shareLogFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/gzip");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Router Logs"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String error) {
        progressContainer.setVisibility(View.GONE);
        Toast.makeText(this, "Download failed: " + error, Toast.LENGTH_LONG).show();
        downloadButton.setText("Download Failed");
    }

    private void openLogFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", file);

            // Use the correct MIME type for .tar.gz files
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/gzip")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app available to open file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (connection != null) connection.disconnect();
        super.onDestroy();
    }
}