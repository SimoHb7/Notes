package com.example.notes.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.notes.data.entity.Note;
import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    long insert(Note note);
    
    @Update
    void update(Note note);
    
    @Delete
    void delete(Note note);
    
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    LiveData<List<Note>> getAllNotes();
    
    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<Note> getNoteById(long id);
    
    @Query("SELECT * FROM notes WHERE id = :id")
    Note getNoteByIdSync(long id);
    
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    LiveData<List<Note>> searchNotes(String query);
} 