package com.example.firewatch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private static final String TAG = "LoginActivity";
    private Handler sessionCheckHandler;
    private Runnable sessionCheckRunnable;
    private boolean sessionChecked = false;

    private static final String CLERK_SIGN_IN_URL = "https://teaching-sloth-78.accounts.dev/sign-in";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    showPermissionRationale();
                }
            });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences("firewatch_prefs", MODE_PRIVATE);

        // Check if already logged in
        if (prefs.getBoolean("is_logged_in", false)) {
            checkNotificationPermission();
            proceedToApp();
            return;
        }

        webView = findViewById(R.id.webViewLogin);
        progressBar = findViewById(R.id.progressBar);

        // Clear any previous Clerk sessions
        clearClerkSession();

        setupWebView();
        webView.loadUrl(CLERK_SIGN_IN_URL);

        startSessionPolling();
    }

    private void clearClerkSession() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        if (webView != null) {
            webView.clearCache(true);
            webView.clearHistory();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                if (!sessionChecked) {
                    checkForClerkSession();
                }
            }
        });
    }

    private void startSessionPolling() {
        sessionCheckHandler = new Handler(Looper.getMainLooper());
        sessionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sessionChecked) {
                    checkForClerkSession();
                    sessionCheckHandler.postDelayed(this, 2000);
                }
            }
        };
        sessionCheckHandler.postDelayed(sessionCheckRunnable, 3000);
    }

    private void checkForClerkSession() {
        if (webView == null || sessionChecked) return;

        webView.evaluateJavascript(
                "(function() { " +
                        "  try { " +
                        "    if (window.Clerk && window.Clerk.session && window.Clerk.user) { " +
                        "      return JSON.stringify({ " +
                        "        userId: window.Clerk.user.id, " +
                        "        email: window.Clerk.user.primaryEmailAddress?.emailAddress || '', " +
                        "        firstName: window.Clerk.user.firstName || '', " +
                        "        lastName: window.Clerk.user.lastName || '', " +
                        "        username: window.Clerk.user.username || '', " +
                        "        role: window.Clerk.user.publicMetadata?.role || 'user', " +
                        "        strategy: window.Clerk.user.publicMetadata?.strategy || '' " +
                        "      }); " +
                        "    } " +
                        "  } catch(e) { " +
                        "    console.error('Session check error:', e); " +
                        "  } " +
                        "  return null; " +
                        "})();",
                result -> {
                    if (result != null && !result.equals("null") && !result.equals("\"null\"")) {
                        sessionChecked = true;
                        stopSessionPolling();
                        handleClerkSession(result);
                    }
                }
        );
    }

    private void stopSessionPolling() {
        if (sessionCheckHandler != null && sessionCheckRunnable != null) {
            sessionCheckHandler.removeCallbacks(sessionCheckRunnable);
        }
    }

    private void handleClerkSession(String sessionJson) {
        try {
            sessionJson = sessionJson.replace("\\\"", "\"");
            if (sessionJson.startsWith("\"")) {
                sessionJson = sessionJson.substring(1, sessionJson.length() - 1);
            }

            Log.d(TAG, "Session JSON: " + sessionJson);

            JSONObject json = new JSONObject(sessionJson);
            String clerkUserId = json.getString("userId");
            String email = json.getString("email");
            String firstName = json.optString("firstName", "");
            String lastName = json.optString("lastName", "");
            String username = json.optString("username", "");
            String clerkStrategy = json.optString("strategy", "").toLowerCase();
            String clerkRole = json.optString("role", "user").toLowerCase();

            String finalRole = (!clerkStrategy.isEmpty()) ? clerkStrategy : clerkRole;

            if (clerkUserId.isEmpty() || email.isEmpty()) {
                Log.w(TAG, "Invalid session data");
                return;
            }

            prefs.edit()
                    .putString("clerk_user_id", clerkUserId)
                    .putString("user_email", email)
                    .putString("user_first_name", firstName)
                    .putString("user_last_name", lastName)
                    .putString("user_username", username)
                    .putString("clerk_strategy", clerkStrategy)
                    .putString("clerk_role", clerkRole)
                    .apply();

            runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

            checkOrCreateUserInSupabase(clerkUserId, email, firstName, lastName, username, finalRole);

        } catch (Exception e) {
            Log.e(TAG, "Login error: " + e.getMessage(), e);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
            sessionChecked = false;
        }
    }

    private void checkOrCreateUserInSupabase(String clerkUserId, String email, String firstName,
                                             String lastName, String username, String finalRole) {
        SupabaseApi api = ApiClient.api(this);

        api.getUserByClerkId(clerkUserId).enqueue(new Callback<List<ApiModels.UserRow>>() {
            @Override
            public void onResponse(Call<List<ApiModels.UserRow>> call, Response<List<ApiModels.UserRow>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ApiModels.UserRow user = response.body().get(0);

                    // Use Clerk role as source of truth
                    String userRole = (finalRole != null && !finalRole.isEmpty())
                            ? finalRole.toLowerCase()
                            : "user";

                    Log.d(TAG, "User exists. Using role: " + userRole);
                    saveUserAndProceed(clerkUserId, email, userRole);

                } else {
                    createUserInSupabase(clerkUserId, email, firstName, lastName, username, finalRole);
                }
            }

            @Override
            public void onFailure(Call<List<ApiModels.UserRow>> call, Throwable t) {
                Log.e(TAG, "Failed to check user: " + t.getMessage(), t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Connection error. Please try again.", Toast.LENGTH_SHORT).show();
                });
                sessionChecked = false;
            }
        });
    }
    private void createUserInSupabase(String clerkUserId, String email, String firstName,
                                      String lastName, String username, String finalRole) {
        ApiModels.CreateUserRequest request = new ApiModels.CreateUserRequest();
        request.clerk_user_id = clerkUserId;
        request.email = email;
        request.first_name = firstName;
        request.last_name = lastName;
        request.username = username;
        request.role = finalRole;

        SupabaseApi api = ApiClient.api(this);
        api.createUser(request).enqueue(new Callback<ApiModels.UserRow>() {
            @Override
            public void onResponse(Call<ApiModels.UserRow> call, Response<ApiModels.UserRow> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String role = (response.body().role != null && !response.body().role.isEmpty())
                            ? response.body().role : finalRole;
                    saveUserAndProceed(clerkUserId, email, role);
                } else {
                    Log.w(TAG, "User creation failed, using Clerk role");
                    saveUserAndProceed(clerkUserId, email, finalRole);
                }
            }

            @Override
            public void onFailure(Call<ApiModels.UserRow> call, Throwable t) {
                Log.e(TAG, "Failed to create user: " + t.getMessage(), t);
                saveUserAndProceed(clerkUserId, email, finalRole);
            }
        });
    }

    private void saveUserAndProceed(String clerkUserId, String email, String role) {
        String normalizedRole = (role != null) ? role.toLowerCase().trim() : "user";

        Log.d(TAG, "Saving session - Email: " + email + ", Role: " + normalizedRole);

        prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("clerk_user_id", clerkUserId)
                .putString("user_email", email)
                .putString("user_role", normalizedRole)
                .putString("clerk_role", normalizedRole)
                .apply();

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Login successful as " + normalizedRole, Toast.LENGTH_SHORT).show();
            checkNotificationPermission();
            proceedToApp();
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission Required")
                .setMessage("FireWatch needs notification permission to alert you about fire emergencies. " +
                        "Please grant this permission in Settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void proceedToApp() {
        String role = prefs.getString("user_role", "user").toLowerCase().trim();

        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSessionPolling();
    }
}