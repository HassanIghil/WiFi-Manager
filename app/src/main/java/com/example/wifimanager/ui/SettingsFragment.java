package com.example.wifimanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.example.wifimanager.R;
import com.example.wifimanager.Tools.Optimization;
import com.example.wifimanager.Tools.Update;
import com.example.wifimanager.Tools.Firewall;
import com.example.wifimanager.Tools.parametres;
import com.example.wifimanager.Tools.Smart_life;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private static String STOK = "";
    private String routerName;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Retrieve the router's name from the arguments and token
        // Inside onCreateView
        if (getArguments() != null) {
            routerName = getArguments().getString("ROUTER_NAME");
            STOK = getArguments().getString("STOK"); // Retrieve the token
        }

        // Find the LinearLayout and set a click listener
        LinearLayout wifiOptimizationButton = view.findViewById(R.id.wifi_optimization_button);
        wifiOptimizationButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Optimization.class);
            startActivity(intent);
        });

        // Add click listener for the update grid item
        LinearLayout updateButton = view.findViewById(R.id.update_button);
        updateButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Update.class);
            intent.putExtra("ROUTER_NAME", routerName); // Pass the router's name
            intent.putExtra("STOK", STOK); // Pass the token
            startActivity(intent);
        });

        // Add click listener for the firewall grid item
        LinearLayout firewallButton = view.findViewById(R.id.firewall_button);
        firewallButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Firewall.class);
            startActivity(intent);
        });

        // Add click listener for the settings grid item
        LinearLayout settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), parametres.class);
            startActivity(intent);
        });

        // Add click listener for the more tools item
        LinearLayout moreToolsButton = view.findViewById(R.id.more_tools_button);
        moreToolsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Smart_life.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}