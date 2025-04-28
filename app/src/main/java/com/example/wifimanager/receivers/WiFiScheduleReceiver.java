package com.example.wifimanager.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class WiFiScheduleReceiver extends BroadcastReceiver {
    private static final String TAG = "WiFiScheduleReceiver";
    private static final String BASE_URL = "http://192.168.31.1/cgi-bin/luci/;stok=";
    private static final String WIFI_CONTROL_ENDPOINT = "/api/xqnetwork/set_all_wifi";
    private static RequestQueue requestQueue;
    private static final Object QUEUE_LOCK = new Object();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received broadcast intent: " + intent.getAction());
        
        String action = intent.getAction();
        String stok = intent.getStringExtra("STOK");
        
        if (stok == null) {
            Log.e(TAG, "Authentication token missing in intent");
            Toast.makeText(context, "Authentication token missing", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean turnOn = action != null && action.equals("com.example.wifimanager.WIFI_SCHEDULE_ON");
        Log.d(TAG, "Processing WiFi schedule - Action: " + action + ", Turn On: " + turnOn);
        executeWiFiControl(context, stok, turnOn);
    }

    private void executeWiFiControl(Context context, String stok, boolean turnOn) {
        // Acquire wake lock
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WiFiManager:WiFiScheduleWakeLock"
        );
        wakeLock.acquire(60000); // 60 seconds timeout

        String url = BASE_URL + stok + WIFI_CONTROL_ENDPOINT + 
                    "?on1=" + (turnOn ? "1" : "0") + 
                    "&on2=" + (turnOn ? "1" : "0");
        Log.d(TAG, "Executing WiFi control - URL: " + url);

        // Initialize RequestQueue if needed
        synchronized (QUEUE_LOCK) {
            if (requestQueue == null) {
                requestQueue = Volley.newRequestQueue(context.getApplicationContext());
            }
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    Log.d(TAG, "WiFi control response: " + response.toString());
                    if (response.getInt("code") == 0) {
                        String action = turnOn ? "enabled" : "disabled";
                        Log.d(TAG, "WiFi " + action + " successfully");
                        Toast.makeText(context, "WiFi " + action + " successfully", 
                                     Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to control WiFi - Error code: " + response.getInt("code"));
                        Toast.makeText(context, "Failed to control WiFi", 
                                     Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing WiFi control response", e);
                    Toast.makeText(context, "Error controlling WiFi", 
                                 Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                Log.e(TAG, "Network error while controlling WiFi", error);
                Toast.makeText(context, "Network error while controlling WiFi", 
                             Toast.LENGTH_SHORT).show();
                wakeLock.release();
            });

        request.setRetryPolicy(new DefaultRetryPolicy(
            30000, // 30 seconds timeout
            2, // 2 retries
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
        
        // Set up a handler to release wake lock if request takes too long
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.w(TAG, "Releasing wake lock due to timeout");
            }
        }, 30000); // 30 seconds timeout
    }
}
