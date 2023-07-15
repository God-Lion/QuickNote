package ht.godlion.quicknote.callbacks;

import ht.godlion.quicknote.model.Note;

public interface NoteEventListener {
    void onNoteClick ( Note note );

    void onNoteLongClick ( Note note );
}
