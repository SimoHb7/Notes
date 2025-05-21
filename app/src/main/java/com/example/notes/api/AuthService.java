package com.example.notes.api;

import com.example.notes.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("auth/register")
    Call<User> register(@Body User user);

    @POST("auth/login")
    Call<User> login(@Body User user);
} 