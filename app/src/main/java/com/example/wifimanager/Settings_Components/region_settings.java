package com.example.wifimanager.Settings_Components;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.wifimanager.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class region_settings extends AppCompatActivity {

    private TextView currentLanguageTextView;
    private LinearLayout languageContainer;
    private String currentLanguage = "en";
    private String currentLanguageName = "English";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_region_settings);

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

        // Initialize views
        currentLanguageTextView = findViewById(R.id.current_language);
        languageContainer = findViewById(R.id.language_container);

        // Set click listeners
        languageContainer.setOnClickListener(v -> showLanguageSelector());
    }

    private void showLanguageSelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language");

        // Create list of languages
        List<Map<String, String>> languages = getLanguages();
        String[] languageNames = new String[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            languageNames[i] = languages.get(i).get("name");
        }

        builder.setItems(languageNames, (dialog, which) -> {
            currentLanguage = languages.get(which).get("lang");
            currentLanguageName = languages.get(which).get("name");
            currentLanguageTextView.setText(currentLanguageName);
            // Here you would typically change the app's language
            Toast.makeText(region_settings.this,
                    "Language changed to " + currentLanguageName,
                    Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private List<Map<String, String>> getLanguages() {
        List<Map<String, String>> languages = new ArrayList<>();
        try {
            String jsonStr = "{ \"list\": [ " +
                    "{ \"lang\": \"cz\", \"name\": \"Čeština\" }, " +
                    "{ \"lang\": \"de\", \"name\": \"Deutsch\" }, " +
                    "{ \"lang\": \"en\", \"name\": \"English\" }, " +
                    "{ \"lang\": \"es\", \"name\": \"Español\" }, " +
                    "{ \"lang\": \"fr\", \"name\": \"Français\" }, " +
                    "{ \"lang\": \"gr\", \"name\": \"Ελληνικά\" }, " +
                    "{ \"lang\": \"hu\", \"name\": \"Magyar\" }, " +
                    "{ \"lang\": \"it\", \"name\": \"Italiano\" }, " +
                    "{ \"lang\": \"pl\", \"name\": \"Polski\" }, " +
                    "{ \"lang\": \"pt\", \"name\": \"Português(Brasil)\" }, " +
                    "{ \"lang\": \"ro\", \"name\": \"Română\" }, " +
                    "{ \"lang\": \"rs\", \"name\": \"Cрпски\" }, " +
                    "{ \"lang\": \"ru\", \"name\": \"Русский\" }, " +
                    "{ \"lang\": \"se\", \"name\": \"svenska\" }, " +
                    "{ \"lang\": \"tr\", \"name\": \"Türkçe\" }, " +
                    "{ \"lang\": \"uk\", \"name\": \"Україна\" }, " +
                    "{ \"lang\": \"zh_cn\", \"name\": \"简体中文\" }, " +
                    "{ \"lang\": \"zh_hk\", \"name\": \"香港繁體\" }, " +
                    "{ \"lang\": \"zh_tw\", \"name\": \"台湾繁體\" } " +
                    "] }";

            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray jsonArray = jsonObject.getJSONArray("list");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject language = jsonArray.getJSONObject(i);
                Map<String, String> languageMap = new HashMap<>();
                languageMap.put("lang", language.getString("lang"));
                languageMap.put("name", language.getString("name"));
                languages.add(languageMap);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return languages;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}