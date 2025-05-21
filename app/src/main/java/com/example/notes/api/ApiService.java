package com.example.notes.api;

import com.example.notes.models.Note;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    @GET("api/notes")
    Call<List<Note>> getNotes();

    @GET("api/notes/{id}")
    Call<Note> getNote(@Path("id") String id);

    @POST("api/notes")
    Call<Note> createNote(@Body Note note);

    @PUT("api/notes/{id}")
    Call<Note> updateNote(@Path("id") String id, @Body Note note);

    @DELETE("api/notes/{id}")
    Call<Void> deleteNote(@Path("id") String id);
} 