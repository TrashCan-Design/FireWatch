package com.example.firewatch;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText edtUrl, edtKey, edtEsp;
    private MaterialButton btnSave, btnCancel;
    private SharedPreferences sp;

    // Require: https://<20-char-ref>.supabase.co/
    private static final Pattern SUPABASE_URL =
            Pattern.compile("^https?://([a-z0-9]{20})\\.supabase\\.co/?$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edtUrl = findViewById(R.id.edtSupabaseUrl);
        edtKey = findViewById(R.id.edtSupabaseKey);
        edtEsp = findViewById(R.id.edtEspId);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        sp = getSharedPreferences("firewatch_prefs", MODE_PRIVATE);

        // Load saved values
        edtUrl.setText(sp.getString("supabase_url", ""));
        edtKey.setText(sp.getString("supabase_key", ""));
        edtEsp.setText(sp.getString("esp32_id", ""));

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String url = safe(edtUrl.getText());
            String key = safe(edtKey.getText());
            String id  = safe(edtEsp.getText());

            url = normalizeUrl(url);

            // --- Validation ---
            if (!isLikelySupabaseUrl(url)) {
                toast("Enter a valid Supabase Project URL (Settings → API → Project URL)");
                edtUrl.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(key) || key.length() < 40) {
                toast("Paste your Supabase anon public key");
                edtKey.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(id)) {
                toast("Enter an ESP32 Device ID (e.g., ESP32_001)");
                edtEsp.requestFocus();
                return;
            }

            // Save
            sp.edit()
                    .putString("supabase_url", url)
                    .putString("supabase_key", key)
                    .putString("esp32_id", id)
                    .apply();

            // Make ApiClient pick up the new settings immediately
            ApiClient.reset();

            toast("Settings saved");
            finish();
        });
    }

    // Helpers
    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private static String normalizeUrl(String url) {
        if (TextUtils.isEmpty(url)) return url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        // Retrofit requires trailing slash
        if (!url.endsWith("/")) url = url + "/";
        return url;
    }

    private static boolean isLikelySupabaseUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        // Quick sanity: is it a valid web URL?
        if (!Patterns.WEB_URL.matcher(url).matches()) return false;
        // Strict match: project-ref + domain
        Matcher m = SUPABASE_URL.matcher(url.endsWith("/") ? url : url + "/");
        return m.matches();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}