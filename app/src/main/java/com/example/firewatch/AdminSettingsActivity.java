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

public class AdminSettingsActivity extends AppCompatActivity {

    private TextInputEditText edtUrl, edtKey, edtEsp;
    private MaterialButton btnSave, btnCancel;
    private SharedPreferences sp;

    private static final Pattern SUPABASE_URL =
            Pattern.compile("^https?://([a-z0-9]{20})\\.supabase\\.co/?$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Settings");
        }

        edtUrl = findViewById(R.id.edtSupabaseUrl);
        edtKey = findViewById(R.id.edtSupabaseKey);
        edtEsp = findViewById(R.id.edtEspId);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        sp = getSharedPreferences("firewatch_prefs", MODE_PRIVATE);

        // Load saved values with defaults
        String defaultUrl = "https://sdjumfnabhzfogdzgcqq.supabase.co/";
        String defaultKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkanVtZm5hYmh6Zm9nZHpnY3FxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkzNDE4ODIsImV4cCI6MjA3NDkxNzg4Mn0.NAkF3q6L2TrZ1GRTvBjdJKW3DzQjSt8m7ZIdcxoZbDw";

        edtUrl.setText(sp.getString("supabase_url", defaultUrl));
        edtKey.setText(sp.getString("supabase_key", defaultKey));
        edtEsp.setText(sp.getString("esp32_id", ""));

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String url = safe(edtUrl.getText());
            String key = safe(edtKey.getText());
            String id  = safe(edtEsp.getText());

            url = normalizeUrl(url);

            // Validation
            if (!isLikelySupabaseUrl(url)) {
                toast("Enter a valid Supabase Project URL");
                edtUrl.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(key) || key.length() < 40) {
                toast("Enter a valid Supabase anon key");
                edtKey.requestFocus();
                return;
            }

            // Save
            sp.edit()
                    .putString("supabase_url", url)
                    .putString("supabase_key", key)
                    .putString("esp32_id", id)
                    .apply();

            ApiClient.reset();

            toast("Settings saved successfully");
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private static String normalizeUrl(String url) {
        if (TextUtils.isEmpty(url)) return url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (!url.endsWith("/")) url = url + "/";
        return url;
    }

    private static boolean isLikelySupabaseUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        if (!Patterns.WEB_URL.matcher(url).matches()) return false;
        Matcher m = SUPABASE_URL.matcher(url.endsWith("/") ? url : url + "/");
        return m.matches();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}