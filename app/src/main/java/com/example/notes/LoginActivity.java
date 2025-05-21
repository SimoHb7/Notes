package com.example.notes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notes.api.AuthService;
import com.example.notes.api.RetrofitClient;
import com.example.notes.models.User;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout emailLayout;
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        emailLayout = findViewById(R.id.emailLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);

        // Initialize API service
        authService = RetrofitClient.getInstance().getAuthService();

        // Set up click listeners
        loginButton.setOnClickListener(v -> handleLogin());
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Add email validation on text change
        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });

        // Check if we have an email from registration
        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            emailInput.setText(email);
            // Move focus to password field
            passwordInput.requestFocus();
        }
    }

    private boolean validateEmail() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            return false;
        }

        // More strict email validation
        String emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailPattern)) {
            emailLayout.setError("Invalid email format");
            return false;
        }

        // Check for valid domain
        String[] parts = email.split("@");
        if (parts.length != 2) {
            emailLayout.setError("Invalid email format");
            return false;
        }

        String domain = parts[1];
        // Check if domain has at least one dot
        if (!domain.contains(".")) {
            emailLayout.setError("Invalid email domain");
            return false;
        }

        // Get TLD (the part after the last dot)
        String tld = domain.substring(domain.lastIndexOf(".") + 1).toLowerCase();
        
        // List of common valid TLDs
        String[] validTlds = {
            "com", "net", "org", "edu", "gov", "mil", "int", "io", "co", "ai", "app",
            "dev", "me", "info", "biz", "name", "pro", "xyz", "online", "site", "tech",
            "store", "shop", "blog", "live", "cloud", "digital", "email", "email", "gmail",
            "yahoo", "hotmail", "outlook", "mail", "inbox", "contact", "support", "help",
            "service", "solutions", "systems", "network", "hosting", "web", "website",
            "online", "digital", "media", "marketing", "design", "studio", "agency",
            "consulting", "services", "solutions", "group", "team", "company", "inc",
            "ltd", "llc", "corp", "co", "org", "net", "edu", "gov", "mil", "int"
        };

        // Check if TLD is in the list of valid TLDs
        boolean isValidTld = false;
        for (String validTld : validTlds) {
            if (tld.equals(validTld)) {
                isValidTld = true;
                break;
            }
        }

        if (!isValidTld) {
            emailLayout.setError("Invalid email domain. Please use a valid domain like .com, .net, .org, etc.");
            return false;
        }

        // Additional common email validation
        if (email.length() > 254) { // RFC 5321
            emailLayout.setError("Email is too long");
            return false;
        }

        if (email.startsWith(".") || email.endsWith(".")) {
            emailLayout.setError("Invalid email format");
            return false;
        }

        // Check for consecutive dots in domain
        if (domain.contains("..")) {
            emailLayout.setError("Invalid email domain");
            return false;
        }

        emailLayout.setError(null);
        return true;
    }

    private void handleLogin() {
        // Reset errors
        emailLayout.setError(null);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateEmail()) {
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        User user = new User("", email, password, "");
        
        authService.login(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Get token from response headers
                    String token = response.headers().get("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7); // Remove "Bearer " prefix
                        // Save token
                        RetrofitClient.saveToken(token);
                        
                        // Navigate to main activity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid server response", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "Login failed";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            if (errorBody.contains("error")) {
                                errorMessage = new org.json.JSONObject(errorBody).getString("error");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Error parsing error response", e);
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
} 