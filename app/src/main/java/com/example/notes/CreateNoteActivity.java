package com.example.notes;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.notes.api.RetrofitClient;
import com.example.notes.models.Note;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNoteActivity extends AppCompatActivity {
    private static final String TAG = "CreateNoteActivity";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/summarize";
    private static final String COHERE_API_KEY = "tZg7bj2rRrUHSUL6uWepa0klO7dKugPzKG7VKlCq"; // clé API gratuite
    private static final String CACHE_PREFS_NAME = "NoteSummaries";
    
    private EditText titleEditText;
    private EditText contentEditText;
    private Button recordButton;
    private Button saveButton;
    private Button deleteButton;
    private boolean isProcessingSpeech = false;
    private Note existingNote = null;
    private String noteId;
    private OkHttpClient client;
    private AlertDialog progressDialog;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);

            // Assign class-level noteId from intent extra
            noteId = getIntent().getStringExtra("note_id");

        setContentView(R.layout.activity_create_note);
            
            // Vérifier la connexion réseau
            if (!RetrofitClient.isNetworkAvailable(this)) {
                Toast.makeText(this, "Pas de connexion Internet. Les modifications ne seront pas sauvegardées.", Toast.LENGTH_LONG).show();
            }

        setupToolbar();
        initializeViews();
        setupClickListeners();

            // Charger la note existante si nécessaire
            if (noteId != null && !noteId.isEmpty()) {
            loadNote(noteId);
        }

        // Initialize SharedPreferences for caching
        prefs = getSharedPreferences(CACHE_PREFS_NAME, MODE_PRIVATE);

        // Initialize OkHttpClient with longer timeouts
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error initializing activity", e);
            Toast.makeText(this, "Erreur lors de l'initialisation de l'application: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        } else if (id == R.id.action_summarize) {
            showSummaryForContent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSummaryForContent() {
        String content = contentEditText.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Aucun contenu à résumer", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Génération du résumé...");

        generateSummaryAsync(content, new SummaryCallback() {
            @Override
            public void onSummaryGenerated(String summary) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    showSummaryDialog(summary);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(CreateNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String detectLanguage(String text) {
        // Détection améliorée basée sur les caractères et mots communs
        if (text == null || text.isEmpty()) return "fr";
        
        // Comptage des caractères et mots spécifiques au français
        int frenchScore = 0;
        int englishScore = 0;
        
        // Caractères spécifiques au français
        String frenchChars = "éèêëàâçîïôöûüù";
        // Mots communs en français
        String[] frenchWords = {"le", "la", "les", "un", "une", "des", "et", "est", "dans", "pour", "avec", "sans", "sur", "sous", "par", "que", "qui", "quoi", "où", "quand", "comment", "pourquoi"};
        // Mots communs en anglais
        String[] englishWords = {"the", "a", "an", "and", "is", "in", "for", "with", "without", "on", "under", "by", "that", "which", "what", "where", "when", "how", "why"};
        
        // Vérifier les caractères spéciaux
        for (char c : text.toLowerCase().toCharArray()) {
            if (frenchChars.indexOf(c) != -1) {
                frenchScore += 2; // Donner plus de poids aux caractères spéciaux
            }
        }
        
        // Vérifier les mots communs
        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            word = word.replaceAll("[^a-zéèêëàâçîïôöûüù]", "");
            if (word.length() > 1) { // Ignorer les mots trop courts
                for (String frenchWord : frenchWords) {
                    if (word.equals(frenchWord)) {
                        frenchScore++;
                        break;
                    }
                }
                for (String englishWord : englishWords) {
                    if (word.equals(englishWord)) {
                        englishScore++;
                        break;
                    }
                }
            }
        }
        
        // Ajouter un biais vers le français si le texte contient des caractères spéciaux
        if (frenchScore > 0) {
            frenchScore += 5;
        }
        
        return frenchScore >= englishScore ? "fr" : "en";
    }

    private void generateSummaryAsync(String content, SummaryCallback callback) {
        if (content == null || content.trim().isEmpty()) {
            callback.onSummaryGenerated("");
            return;
        }

        String trimmedContent = content.trim();
        String detectedLanguage = detectLanguage(trimmedContent);
        String cacheKey = trimmedContent.hashCode() + "_" + detectedLanguage;

        // Check cache
        String cachedSummary = prefs.getString(cacheKey, null);
        if (cachedSummary != null) {
            callback.onSummaryGenerated(cachedSummary);
            return;
        }

        // For all texts, use local summary only (API disabled due to unavailability)
        String summary = generateSimpleSummary(trimmedContent);
        prefs.edit().putString(cacheKey, summary).apply();
        callback.onSummaryGenerated(summary);
    }

    private String generateSimpleSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Pour les textes très courts
        if (content.length() <= 50) {
            return content;
        }

        // Diviser en phrases
        String[] sentences = content.split("[.!?]+");
        if (sentences.length <= 1) {
            // Pour une seule phrase, prendre le début et la fin
            String[] words = content.split("\\s+");
            if (words.length <= 10) {
                return content;
            }
            return String.join(" ", 
                String.join(" ", Arrays.copyOfRange(words, 0, 3)),
                "...",
                String.join(" ", Arrays.copyOfRange(words, words.length - 3, words.length))
            );
        }

        // Pour plusieurs phrases
        StringBuilder summary = new StringBuilder();
        
        // Première phrase
        summary.append(sentences[0].trim());
        
        // Si plus de 2 phrases, ajouter une phrase du milieu
        if (sentences.length > 2) {
            int midIndex = sentences.length / 2;
            String midSentence = sentences[midIndex].trim();
            if (midSentence.length() > 20) {
                summary.append(". ").append(midSentence);
            }
        }
        
        // Dernière phrase si différente
        if (sentences.length > 1) {
            String lastSentence = sentences[sentences.length - 1].trim();
            if (!lastSentence.equals(sentences[0].trim())) {
                summary.append(". ").append(lastSentence);
            }
        }

        return summary.toString().trim();
    }

    private String cleanSummary(String summary) {
        // Supprimer les préfixes communs
        summary = summary.replaceAll("(?i)^(voici|here is|here's|this is|aquí está|hier ist|ecco|resumen|zusammenfassung).*?:\\s*", "");
        summary = summary.replaceAll("(?i)^(le texte|the text|el texto|der text).*?\\s", "");
        
        // Supprimer les espaces multiples
        summary = summary.replaceAll("\\s+", " ");
        
        return summary.trim();
    }

    private void showProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = new AlertDialog.Builder(this)
            .setTitle("Veuillez patienter")
            .setMessage(message)
            .setCancelable(false)
            .create();
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showSummaryDialog(String summary) {
        new AlertDialog.Builder(this)
            .setTitle("Résumé généré")
            .setMessage(summary)
            .setPositiveButton("Copier", (dialog, which) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Résumé", summary);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Résumé copié", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Fermer", null)
            .show();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer la note")
            .setMessage("Êtes-vous sûr de vouloir supprimer cette note ?")
            .setPositiveButton("Oui", (dialog, which) -> deleteNote())
            .setNegativeButton("Non", null)
            .show();
    }

    private void deleteNote() {
        if (existingNote != null) {
            RetrofitClient.getInstance().getApiService().deleteNote(String.valueOf(existingNote.getId()))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(CreateNoteActivity.this, 
                                "Note supprimée", 
                                Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CreateNoteActivity.this, 
                                "Erreur de suppression: " + response.code(), 
                                Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(CreateNoteActivity.this, 
                            "Erreur de connexion: " + t.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
        }
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Le titre est requis");
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer du contenu", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Génération du résumé...");

        generateSummaryAsync(content, new SummaryCallback() {
            @Override
            public void onSummaryGenerated(String summary) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Date currentDate = new Date();

                    Note note;
                    if (existingNote != null) {
                        // Update existing note
                        existingNote.setTitle(title);
                        existingNote.setContent(content);
                        existingNote.setSummary(summary);
                        existingNote.setUpdatedAt(currentDate);
                        note = existingNote;
                    } else {
                        // Create new note
                        note = new Note(title, content, summary);
                    }

                    // Show progress dialog for saving
                    progressDialog = new AlertDialog.Builder(CreateNoteActivity.this)
                        .setTitle("Enregistrement")
                        .setMessage("Veuillez patienter...")
                        .setCancelable(false)
                        .create();
                    progressDialog.show();

                    // Save to API
                    if (existingNote != null) {
                        updateNote(note);
                    } else {
                        createNote(note);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(CreateNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                    // Fallback to simple summary
                    String fallbackSummary = generateSimpleSummary(content);
                    Date currentDate = new Date();

                    Note note;
                    if (existingNote != null) {
                        existingNote.setTitle(title);
                        existingNote.setContent(content);
                        existingNote.setSummary(fallbackSummary);
                        existingNote.setUpdatedAt(currentDate);
                        note = existingNote;
                    } else {
                        note = new Note(title, content, fallbackSummary);
                    }

                    // Show progress dialog for saving
                    progressDialog = new AlertDialog.Builder(CreateNoteActivity.this)
                        .setTitle("Enregistrement")
                        .setMessage("Veuillez patienter...")
                        .setCancelable(false)
                        .create();
                    progressDialog.show();

                    if (existingNote != null) {
                        updateNote(note);
                    } else {
                        createNote(note);
                    }
                });
            }
        });
    }

    private void createNote(Note note) {
        if (!RetrofitClient.isNetworkAvailable(this)) {
            Toast.makeText(this, "Pas de connexion Internet. Impossible de créer la note.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            progressDialog.show();
        RetrofitClient.getInstance().getApiService().createNote(note).enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                        }

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                if (response.isSuccessful()) {
                    Toast.makeText(CreateNoteActivity.this, 
                        "Note enregistrée", 
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                            String errorMessage = "Erreur d'enregistrement";
                            try {
                                if (response.errorBody() != null) {
                                    errorMessage += ": " + response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                            
                    Log.e(TAG, "Error creating note: " + response.code());
                            Toast.makeText(CreateNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse: Error handling response", e);
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(CreateNoteActivity.this, "Erreur lors du traitement de la réponse", Toast.LENGTH_SHORT).show();
                        }
                }
            }

            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!call.isCanceled()) {
                            Log.e(TAG, "Error creating note: " + t.getMessage());
                            String errorMessage;
                            if (t instanceof IOException) {
                                errorMessage = "Erreur de connexion. Vérifiez votre connexion Internet.";
                            } else if (t.getMessage() != null && t.getMessage().contains("failed to connect")) {
                                errorMessage = "Impossible de se connecter au serveur. Vérifiez que le serveur est en cours d'exécution.";
                            } else {
                                errorMessage = "Erreur inattendue: " + t.getMessage();
                            }
                            Toast.makeText(CreateNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onFailure: Error handling failure", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "createNote: Error creating note", e);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "Erreur lors de la création de la note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateNote(Note note) {
        if (!RetrofitClient.isNetworkAvailable(this)) {
            Toast.makeText(this, "Pas de connexion Internet. Impossible de mettre à jour la note.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Log.d(TAG, "Updating note with ID: " + note.getId());
            progressDialog.show();
            
        RetrofitClient.getInstance().getApiService().updateNote(String.valueOf(note.getId()), note).enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                        }

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                if (response.isSuccessful()) {
                    Toast.makeText(CreateNoteActivity.this, 
                        "Note mise à jour", 
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                            String errorMessage = "Erreur de mise à jour";
                            try {
                                if (response.errorBody() != null) {
                                    errorMessage += ": " + response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                            
                    Log.e(TAG, "Error updating note: " + response.code());
                            Toast.makeText(CreateNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse: Error handling response", e);
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(CreateNoteActivity.this, "Erreur lors du traitement de la réponse", Toast.LENGTH_SHORT).show();
                        }
                }
            }

            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!call.isCanceled()) {
                            Log.e(TAG, "Error updating note: " + t.getMessage());
                            String errorMessage;
                            if (t instanceof IOException) {
                                errorMessage = "Erreur de connexion. Vérifiez votre connexion Internet.";
                            } else if (t.getMessage() != null && t.getMessage().contains("failed to connect")) {
                                errorMessage = "Impossible de se connecter au serveur. Vérifiez que le serveur est en cours d'exécution.";
                            } else {
                                errorMessage = "Erreur inattendue: " + t.getMessage();
                            }
                            Toast.makeText(CreateNoteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onFailure: Error handling failure", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "updateNote: Error updating note", e);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "Erreur lors de la mise à jour de la note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Remove the old generateSummary method since we now use async summary generation
    // private String generateSummary(String content) {
    //     if (content == null || content.isEmpty()) {
    //         return "";
    //     }

    //     // Remove extra whitespace and newlines
    //     content = content.trim().replaceAll("\\s+", " ");

    //     // If content is already short, return it as is
    //     if (content.length() <= 100) {
    //         return content;
    //     }

    //     // Find the last complete sentence within 100 characters
    //     int endIndex = 100;
    //     while (endIndex < content.length() && endIndex < 150) {
    //         char c = content.charAt(endIndex);
    //         if (c == '.' || c == '!' || c == '?') {
    //             endIndex++;
    //             break;
    //         }
    //         endIndex++;
    //     }

    //     // Get the summary and add ellipsis if needed
    //     String summary = content.substring(0, endIndex).trim();
    //     if (endIndex < content.length()) {
    //         summary += "...";
    //     }

    //     return summary;
    // }

    private void initializeViews() {
        try {
        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        recordButton = findViewById(R.id.recordButton);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);

            // Activer le bouton de reconnaissance vocale par défaut
            if (recordButton != null) {
                recordButton.setVisibility(View.VISIBLE);
                recordButton.setEnabled(true);
            }
        
        // Hide delete button for new notes
            if (noteId == null && deleteButton != null) {
                deleteButton.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void setupClickListeners() {
        try {
            if (recordButton != null) {
        recordButton.setOnClickListener(v -> {
            if (!isProcessingSpeech) {
                if (checkPermission()) {
                    startSpeechRecognition();
                } else {
                    requestPermission();
                }
            }
        });
            }

            if (saveButton != null) {
        saveButton.setOnClickListener(v -> saveNote());
            }

            if (deleteButton != null) {
                deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    private void startSpeechRecognition() {
        if (!checkPermission()) {
            requestPermission();
            return;
        }

        try {
            isProcessingSpeech = true;
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                          RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, 
                          "Parlez maintenant...");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            isProcessingSpeech = false;
            Log.e(TAG, "Error starting speech recognition", e);
            Toast.makeText(this, 
                "Erreur de reconnaissance vocale. Veuillez réessayer.", 
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE) {
            isProcessingSpeech = false;
            
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String spokenText = results.get(0);
                    if (spokenText != null && !spokenText.isEmpty()) {
                        // Insérer le texte à la position actuelle du curseur
                        int start = contentEditText.getSelectionStart();
                        String currentText = contentEditText.getText().toString();
                        String newText;
                        
                        if (start >= 0) {
                            newText = currentText.substring(0, start) + 
                                     spokenText + 
                                     currentText.substring(start);
                        } else {
                            newText = currentText + 
                                     (currentText.isEmpty() ? "" : "\n") + 
                                     spokenText;
                        }
                        
                        contentEditText.setText(newText);
                        
                        // Placer le curseur après le texte inséré
                        if (start >= 0) {
                            contentEditText.setSelection(start + spokenText.length());
                        } else {
                            contentEditText.setSelection(newText.length());
                        }
                        
                        Toast.makeText(this, "Texte ajouté", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Reconnaissance vocale annulée", 
                             Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, 
                    "Erreur de reconnaissance vocale. Veuillez réessayer.", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.RECORD_AUDIO},
            PERMISSION_REQUEST_RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                Toast.makeText(this, 
                    "La permission d'enregistrement audio est nécessaire pour la reconnaissance vocale", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (hasChanges()) {
            new AlertDialog.Builder(this)
                .setTitle("Quitter")
                .setMessage("Voulez-vous enregistrer les modifications ?")
                .setPositiveButton("Oui", (dialog, which) -> saveNote())
                .setNegativeButton("Non", (dialog, which) -> finish())
                .setNeutralButton("Annuler", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasChanges() {
        if (existingNote == null) {
            return !titleEditText.getText().toString().trim().isEmpty() ||
                   !contentEditText.getText().toString().trim().isEmpty();
        }
        return !titleEditText.getText().toString().equals(existingNote.getTitle()) ||
               !contentEditText.getText().toString().equals(existingNote.getContent());
    }

    private void loadNote(String noteId) {
        try {
            if (noteId == null || noteId.isEmpty()) {
                Log.e(TAG, "loadNote: Invalid note ID");
                Toast.makeText(this, "ID de note invalide", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

        RetrofitClient.getInstance().getApiService().getNote(noteId).enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                if (response.isSuccessful() && response.body() != null) {
                    existingNote = response.body();
                    titleEditText.setText(existingNote.getTitle());
                    contentEditText.setText(existingNote.getContent());
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Modifier la note");
                    }
                } else {
                    Log.e(TAG, "Error loading note: " + response.code());
                    Toast.makeText(CreateNoteActivity.this, "Erreur lors du chargement de la note", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                Log.e(TAG, "Error loading note: " + t.getMessage());
                Toast.makeText(CreateNoteActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        } catch (Exception e) {
            Log.e(TAG, "loadNote: Error loading note", e);
            Toast.makeText(this, "Erreur lors du chargement de la note", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(noteId != null ? "Modifier la note" : "Nouvelle note");
        }
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Error destroying activity", e);
        }
    }

    private interface SummaryCallback {
        void onSummaryGenerated(String summary);
        void onError(String errorMessage);
    }
}
