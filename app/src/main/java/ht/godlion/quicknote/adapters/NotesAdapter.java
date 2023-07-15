package ht.godlion.quicknote.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import ht.godlion.quicknote.R;
import ht.godlion.quicknote.callbacks.NoteEventListener;
import ht.godlion.quicknote.model.Note;
import ht.godlion.quicknote.utils.NoteUtils;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteHolder> {
    private final Context context;
    private final ArrayList<Note> notes;
    private NoteEventListener listener;
    private boolean multiCheckMode = false;


    public NotesAdapter ( Context context, ArrayList<Note> notes ) {
        this.context = context;
        this.notes = notes;
    }


    @NonNull
    @Override
    public NoteHolder onCreateViewHolder ( @NonNull ViewGroup parent, int viewType ) {
        View view = LayoutInflater.from(context).inflate( R.layout.note_layout, parent, false );
        return new NoteHolder(view);
    }

    @Override
    public void onBindViewHolder (@NonNull NoteHolder holder, int position ) {
        final Note note = getNote(position);
        if ( note != null ) {
            holder.noteText.setText(note.getNoteText());
            holder.noteDate.setText(NoteUtils.dateFromLong(note.getNoteDate()));

            holder.itemView.setOnClickListener(view -> listener.onNoteClick(note));

            holder.itemView.setOnLongClickListener(view -> {
                listener.onNoteLongClick(note);
                return false;
            });

            if (multiCheckMode) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(note.isChecked());
            } else holder.checkBox.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount () {
        return notes.size();
    }

    private Note getNote ( int position ) {
        return notes.get( position );
    }

    public List<Note> getCheckedNotes () {
        List<Note> checkedNotes = new ArrayList<>();
        for (Note note : this.notes) if ( note.isChecked() ) checkedNotes.add( note );
        return checkedNotes;
    }

    public void setListener(NoteEventListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMultiCheckMode ( boolean multiCheckMode ) {
        this.multiCheckMode = multiCheckMode;
        if (!multiCheckMode) for (Note note : this.notes) note.setChecked(false);
        notifyDataSetChanged();
    }

    static class NoteHolder extends RecyclerView.ViewHolder {
        TextView noteText, noteDate;
        CheckBox checkBox;

        public NoteHolder ( View itemView ) {
            super( itemView );
            noteDate = itemView.findViewById(R.id.note_date);
            noteText = itemView.findViewById(R.id.note_text);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
