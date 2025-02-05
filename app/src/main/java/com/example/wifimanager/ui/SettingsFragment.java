package com.example.wifimanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.example.wifimanager.R;
import com.example.wifimanager.Tools.Optimization;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Find the LinearLayout and set a click listener
        LinearLayout wifiOptimizationButton = view.findViewById(R.id.wifi_optimization_button);
        wifiOptimizationButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Optimization.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "SettingsFragment destroyed");
    }
}