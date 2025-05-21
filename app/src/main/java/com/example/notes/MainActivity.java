package com.example.notes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.notes.adapter.NotesAdapter;
import com.example.notes.api.RetrofitClient;
import com.example.notes.models.Note;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NotesAdapter.OnNoteClickListener {
    private static final String TAG = "MainActivity";
    private RecyclerView notesRecyclerView;
    private FloatingActionButton fabAddNote;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NotesAdapter adapter;
    private List<Note> notesList;
    private List<Note> filteredNotesList;
    private boolean isActivityActive = false;
    private SearchView searchView;
    private Toolbar toolbar;
    private Call<List<Note>> currentCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Log.d(TAG, "onCreate: Activity starting");

            if (!RetrofitClient.isNetworkAvailable(this)) {
                Toast.makeText(this, "Pas de connexion Internet. L'application fonctionnera en mode limité.", Toast.LENGTH_LONG).show();
            }

            setupToolbar();
            initializeViews();
            setupRecyclerView();
            setupClickListeners();
            setupSwipeRefresh();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error initializing activity", e);
            Toast.makeText(this, "Erreur lors de l'initialisation de l'application", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupToolbar() {
        try {
            toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    getSupportActionBar().setTitle("Mes Notes");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "setupToolbar: Error setting up toolbar", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            MenuItem searchItem = menu.findItem(R.id.action_search);
            if (searchItem != null) {
                searchView = (SearchView) searchItem.getActionView();
                if (searchView != null) {
                    searchView.setQueryHint("Rechercher une note...");
                    setupSearchView();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreateOptionsMenu: Error creating menu", e);
        }
        return true;
    }

    private void setupSearchView() {
        if (searchView == null) return;

        try {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterNotes(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterNotes(newText);
                    return true;
                }
            });

            searchView.setOnCloseListener(() -> {
                if (adapter != null) {
                    adapter.setNotes(notesList);
                }
                return false;
            });
        } catch (Exception e) {
            Log.e(TAG, "setupSearchView: Error setting up search view", e);
        }
    }

    private void filterNotes(String query) {
        try {
            if (adapter == null || notesList == null) return;

            if (query == null || query.isEmpty()) {
                adapter.setNotes(notesList);
                return;
            }

            filteredNotesList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Note note : notesList) {
                String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
                String content = note.getContent() != null ? note.getContent().toLowerCase() : "";
                String summary = note.getSummary() != null ? note.getSummary().toLowerCase() : "";

                if (title.contains(lowerCaseQuery) ||
                    content.contains(lowerCaseQuery) ||
                    summary.contains(lowerCaseQuery)) {
                    filteredNotesList.add(note);
                }
            }

            adapter.setNotes(filteredNotesList);
        } catch (Exception e) {
            Log.e(TAG, "filterNotes: Error filtering notes", e);
        }
    }

    private void initializeViews() {
        try {
            notesRecyclerView = findViewById(R.id.notesRecyclerView);
            fabAddNote = findViewById(R.id.fabAddNote);
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        } catch (Exception e) {
            Log.e(TAG, "initializeViews: Error initializing views", e);
        }
    }

    private void setupRecyclerView() {
        try {
            if (notesRecyclerView == null) return;

            notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            notesList = new ArrayList<>();
            filteredNotesList = new ArrayList<>();
            adapter = new NotesAdapter(this);
            notesRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "setupRecyclerView: Error setting up recycler view", e);
        }
    }

    private void setupClickListeners() {
        try {
            if (fabAddNote == null) return;

            fabAddNote.setOnClickListener(view -> {
                if (!isFinishing() && !isDestroyed()) {
                    try {
                        Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "setupClickListeners: Error starting CreateNoteActivity", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "setupClickListeners: Error setting up click listeners", e);
        }
    }

    private void setupSwipeRefresh() {
        try {
            if (swipeRefreshLayout == null) return;

            swipeRefreshLayout.setOnRefreshListener(this::loadNotes);
            swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        } catch (Exception e) {
            Log.e(TAG, "setupSwipeRefresh: Error setting up swipe refresh", e);
        }
    }
    
    private void loadNotes() {
        try {
            if (!isActivityActive || isFinishing() || isDestroyed()) {
                Log.d(TAG, "loadNotes: Activity not active, skipping load");
                return;
            }

            if (!RetrofitClient.isNetworkAvailable(this)) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentCall != null && !currentCall.isCanceled()) {
                currentCall.cancel();
            }

            Log.d(TAG, "loadNotes: Starting to load notes");
            currentCall = RetrofitClient.getInstance().getApiService().getNotes();
            currentCall.enqueue(new Callback<List<Note>>() {
                @Override
                public void onResponse(Call<List<Note>> call, Response<List<Note>> response) {
                    try {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        
                        if (!isActivityActive || isFinishing() || isDestroyed()) {
                            Log.d(TAG, "onResponse: Activity not active, skipping update");
                            return;
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "onResponse: Successfully loaded " + response.body().size() + " notes");
                            notesList.clear();
                            notesList.addAll(response.body());
                            
                            if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
                                filterNotes(searchView.getQuery().toString());
                            } else if (adapter != null) {
                                adapter.setNotes(notesList);
                            }
                        } else {
                            String errorMessage = "Erreur lors du chargement des notes";
                            try {
                                if (response.errorBody() != null) {
                                    errorMessage += ": " + response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                            
                            Log.e(TAG, "onResponse: " + errorMessage + " Code: " + response.code());
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse: Error processing response", e);
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(MainActivity.this, "Erreur lors du traitement de la réponse", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Note>> call, Throwable t) {
                    try {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        
                        if (!isActivityActive || isFinishing() || isDestroyed()) {
                            Log.d(TAG, "onFailure: Activity not active, skipping error handling");
                            return;
                        }
                        
                        if (!call.isCanceled()) {
                            Log.e(TAG, "onFailure: Error loading notes", t);
                            String errorMessage;
                            if (t instanceof IOException) {
                                errorMessage = "Erreur de connexion. Vérifiez votre connexion Internet.";
                            } else if (t.getMessage() != null && t.getMessage().contains("failed to connect")) {
                                errorMessage = "Impossible de se connecter au serveur. Vérifiez que le serveur est en cours d'exécution et que vous êtes sur le même réseau.";
                            } else {
                                errorMessage = "Erreur inattendue: " + t.getMessage();
                            }
                            
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onFailure: Error handling failure", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadNotes: Error loading notes", e);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "Erreur lors du chargement des notes", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        try {
            isActivityActive = true;
            if (RetrofitClient.isNetworkAvailable(this)) {
                loadNotes();
            } else {
                Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onResume: Error resuming activity", e);
        }
    }

    @Override
    protected void onPause() {
        try {
            super.onPause();
            isActivityActive = false;
            if (currentCall != null && !currentCall.isCanceled()) {
                currentCall.cancel();
            }
        } catch (Exception e) {
            Log.e(TAG, "onPause: Error pausing activity", e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            isActivityActive = false;
            
            if (currentCall != null) {
                currentCall.cancel();
                currentCall = null;
            }
            
            if (adapter != null) {
                adapter = null;
            }
            
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Error destroying activity", e);
        }
    }

    @Override
    public void onNoteClick(Note note) {
        try {
            if (!isFinishing() && !isDestroyed()) {
                Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
                intent.putExtra("note_id", String.valueOf(note.getId()));
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "onNoteClick: Error opening note", e);
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "Erreur lors de l'ouverture de la note", Toast.LENGTH_SHORT).show();
            }
        }
    }
}