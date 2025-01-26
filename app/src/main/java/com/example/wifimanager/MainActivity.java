package com.example.wifimanager;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.miwifi.DO.MiWifiDeviceDO;
import com.example.wifimanager.miwifi.DO.MiWifiDevicelistDO;
import com.example.wifimanager.miwifi.DO.MiWifiRouterNameDO;
import com.example.wifimanager.miwifi.impl.MiWifiApiDefaultImpl;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String STOK;
    private MiWifiApiDefaultImpl miWifiApi;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String routerName = getIntent().getStringExtra("ROUTER_NAME");

        // Display the router name in a TextView
        TextView routerNameTextView = findViewById(R.id.routername);
        if (routerName != null) {
            routerNameTextView.setText(routerName);
        } else {
            routerNameTextView.setText("Router name not available");
        }
    }


}
