package com.example.notes.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

import com.example.notes.utils.DeviceUtils;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static final String PREF_NAME = "NotesPrefs";
    private static final String KEY_TOKEN = "auth_token";
    
    // For Android Emulator, use 10.0.2.2 which points to host machine's localhost
    private static final String EMULATOR_BASE_URL = "http://10.0.2.2:3000/";
    // For physical device, use your computer's IP address
    private static final String PHYSICAL_DEVICE_BASE_URL = "http://192.168.137.27:3000/";
    
    private static RetrofitClient instance;
    private final ApiService apiService;
    private final AuthService authService;
    private static Retrofit retrofit = null;
    private static Context appContext;

    public static void init(Context context) {
        try {
            Log.d(TAG, "Initializing RetrofitClient with context");
            appContext = context.getApplicationContext();
            Log.d(TAG, "Context initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing RetrofitClient: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize RetrofitClient", e);
        }
    }

    private RetrofitClient() {
        Log.d(TAG, "Creating RetrofitClient instance");
        if (appContext == null) {
            Log.e(TAG, "Context is null in RetrofitClient constructor");
            throw new IllegalStateException("RetrofitClient must be initialized with context first");
        }

        try {
            // Create OkHttpClient with logging and auth interceptor
            Log.d(TAG, "Setting up OkHttpClient");
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                Log.d(TAG, "OkHttp: " + message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Log.d(TAG, "Making request to: " + original.url());
                        
                        // Get token from SharedPreferences
                        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        String token = prefs.getString(KEY_TOKEN, null);
                        Log.d(TAG, "Token available: " + (token != null));
                        
                        // If we have a token, add it to the request
                        Request.Builder builder = original.newBuilder();
                        if (token != null) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        
                        Request request = builder.build();
                        return chain.proceed(request);
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            Log.d(TAG, "OkHttpClient created successfully");

            // Create Retrofit instance
            Log.d(TAG, "Creating Retrofit instance");
            String baseUrl = getBaseUrl();
            Log.d(TAG, "Using base URL: " + baseUrl);
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Log.d(TAG, "Retrofit instance created successfully");

            // Initialize services
            Log.d(TAG, "Initializing API services");
            apiService = retrofit.create(ApiService.class);
            authService = retrofit.create(AuthService.class);
            Log.d(TAG, "API services initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating RetrofitClient: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create RetrofitClient", e);
        }
    }

    // Save token to SharedPreferences
    public static void saveToken(String token) {
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_TOKEN, token).apply();
        }
    }

    // Clear token from SharedPreferences (for logout)
    public static void clearToken() {
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_TOKEN).apply();
        }
    }

    private static String getBaseUrl() {
        if (appContext == null) {
            Log.e(TAG, "RetrofitClient not initialized with context");
            throw new IllegalStateException("RetrofitClient must be initialized with context first");
        }
        try {
            boolean isEmulator = DeviceUtils.isEmulator(appContext);
            String baseUrl = isEmulator ? EMULATOR_BASE_URL : PHYSICAL_DEVICE_BASE_URL;
            Log.d(TAG, "Device type: " + (isEmulator ? "Emulator" : "Physical Device"));
            Log.d(TAG, "Using base URL: " + baseUrl);
            return baseUrl;
        } catch (Exception e) {
            Log.e(TAG, "Error getting base URL: " + e.getMessage(), e);
            Log.w(TAG, "Falling back to emulator URL: " + EMULATOR_BASE_URL);
            return EMULATOR_BASE_URL;
        }
    }

    public static synchronized RetrofitClient getInstance() {
        Log.d(TAG, "Getting RetrofitClient instance");
        if (instance == null) {
            Log.d(TAG, "Creating new RetrofitClient instance");
            instance = new RetrofitClient();
        }
        Log.d(TAG, "Returning RetrofitClient instance");
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Create OkHttpClient with logging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                Log.d(TAG, "OkHttp: " + message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                Log.d(TAG, "Network available: " + isConnected);
                return isConnected;
            }
            Log.d(TAG, "Network not available - ConnectivityManager is null");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage());
            return false;
        }
    }
} 