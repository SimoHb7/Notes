package com.example.notes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.notes.R;
import com.example.notes.models.Note;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private OnNoteClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public NotesAdapter(OnNoteClickListener listener) {
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.titleTextView.setText(note.getTitle());
        
        // Set summary text
        String summary = note.getSummary() != null ? note.getSummary() : "";
        holder.summaryTextView.setText(summary);
        
        // Set full content
        String content = note.getContent() != null ? note.getContent() : "";
        holder.fullContentTextView.setText(content);
        
        // Show gradient overlay only if content is longer than summary
        holder.gradientOverlay.setVisibility(
            content.length() > summary.length() ? View.VISIBLE : View.GONE
        );
        
        holder.dateTextView.setText(dateFormat.format(note.getCreatedAt()));
        
        // Set up toggle button
        holder.toggleButton.setOnClickListener(v -> {
            boolean isExpanded = holder.fullContentTextView.getVisibility() == View.VISIBLE;
            
            // Animate the transition
            Animation fadeIn = AnimationUtils.loadAnimation(v.getContext(), android.R.anim.fade_in);
            Animation fadeOut = AnimationUtils.loadAnimation(v.getContext(), android.R.anim.fade_out);
            
            if (isExpanded) {
                holder.fullContentTextView.startAnimation(fadeOut);
                holder.summaryTextView.startAnimation(fadeIn);
                holder.gradientOverlay.startAnimation(fadeIn);
            } else {
                holder.summaryTextView.startAnimation(fadeOut);
                holder.fullContentTextView.startAnimation(fadeIn);
                holder.gradientOverlay.startAnimation(fadeOut);
            }
            
            holder.fullContentTextView.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.summaryTextView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.gradientOverlay.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.toggleButton.setText(isExpanded ? "Lire la suite" : "Voir moins");
            holder.toggleButton.setIconResource(isExpanded ? 
                android.R.drawable.ic_menu_more : 
                android.R.drawable.ic_menu_close_clear_cancel);
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView summaryTextView;
        TextView fullContentTextView;
        TextView dateTextView;
        MaterialButton toggleButton;
        View gradientOverlay;

        NoteViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            summaryTextView = itemView.findViewById(R.id.summaryTextView);
            fullContentTextView = itemView.findViewById(R.id.fullContentTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            toggleButton = itemView.findViewById(R.id.toggleButton);
            gradientOverlay = itemView.findViewById(R.id.gradientOverlay);
        }
    }
} 