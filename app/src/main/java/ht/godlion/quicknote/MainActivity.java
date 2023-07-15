package ht.godlion.quicknote;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ht.godlion.quicknote.adapters.NotesAdapter;
import ht.godlion.quicknote.callbacks.MainActionModeCallback;
import ht.godlion.quicknote.callbacks.NoteEventListener;
import ht.godlion.quicknote.db.NotesDB;
import ht.godlion.quicknote.db.NotesDao;
import ht.godlion.quicknote.model.Note;
import ht.godlion.quicknote.utils.NoteUtils;

import static ht.godlion.quicknote.EditNoteActivity.NOTE_EXTRA_Key;

public class MainActivity extends AppCompatActivity implements NoteEventListener {
    private RecyclerView recyclerView;
    private ArrayList<Note> notes;
    private NotesAdapter adapter;
    private NotesDao dao;
    private MainActionModeCallback actionModeCallback;
    private int checkedCount = 0;
    private FloatingActionButton fab;
    public static final String THEME_Key = "app_theme";
    public static final String APP_PREFERENCES="notepad_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.notes_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> onAddNewNote());

        dao = NotesDB.getInstance(this).notesDao();
    }

    @Override
    public boolean onCreateOptionsMenu ( Menu menu ) {
        getMenuInflater().inflate( R.menu.menu_main, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected ( @NonNull MenuItem item ) { return super.onOptionsItemSelected(item); }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    @Override
    public void onNoteClick (Note note ) {
        Intent edit = new Intent( this, EditNoteActivity.class );
        edit.putExtra(NOTE_EXTRA_Key, note.getId());
        startActivity(edit);
    }

    @Override
    public void onNoteLongClick ( Note note ) {
        note.setChecked(true);
        checkedCount = 1;
        adapter.setMultiCheckMode(true);
        adapter.setListener( new NoteEventListener () {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onNoteClick ( Note note ) {
                note.setChecked( !note.isChecked() );
                if ( note.isChecked () ) checkedCount++;
                else checkedCount--;
                actionModeCallback.changeShareItemVisible(checkedCount <= 1);
                if (checkedCount == 0) actionModeCallback.getAction().finish();
                actionModeCallback.setCount(checkedCount + "/" + notes.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNoteLongClick ( Note note ) {}
        });

        actionModeCallback = new MainActionModeCallback() {
            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if ( menuItem.getItemId() == R.id.action_delete_notes ) onDeleteMultiNotes();
                else if (menuItem.getItemId() == R.id.action_share_note) onShareNote();
                actionMode.finish();
                return false;
            }
        };
        startActionMode(actionModeCallback);
        fab.setVisibility(View.GONE);
        actionModeCallback.setCount(checkedCount + "/" + notes.size());
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        adapter.setMultiCheckMode(false);
        adapter.setListener(this);
        fab.setVisibility(View.VISIBLE);
    }

    private final ItemTouchHelper swipeToDeleteHelper = new ItemTouchHelper ( new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

        @Override
        public void onSwiped ( @NonNull RecyclerView.ViewHolder viewHolder, int direction ) {
            if (notes != null) {
                Note swipedNote = notes.get(viewHolder.getAdapterPosition());
                if (swipedNote != null) swipeToDelete(swipedNote, viewHolder);
            }
        }
    });

    private void showEmptyView () {
        if ( notes.size() == 0 ) {
            this.recyclerView.setVisibility(View.GONE);
            findViewById(R.id.empty_notes_view).setVisibility(View.VISIBLE);
        } else {
            this.recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_notes_view).setVisibility(View.GONE);
        }
    }

    private void loadNotes () {
        this.notes = new ArrayList<>();
        List<Note> list = dao.getNotes();
        this.notes.addAll( list );
        this.adapter = new NotesAdapter( this, this.notes );

        this.adapter.setListener(this);
        this.recyclerView.setAdapter(adapter);
        showEmptyView();
        swipeToDeleteHelper.attachToRecyclerView(recyclerView);
    }

    private void onAddNewNote () { startActivity(new Intent(this, EditNoteActivity.class)); }

    private void swipeToDelete ( final Note swipedNote, final RecyclerView.ViewHolder viewHolder ) {
        new AlertDialog.Builder( MainActivity.this )
                .setMessage("Delete Note ?")
                .setPositiveButton("Delete", ( dialogInterface, i ) -> {
                    dao.deleteNote(swipedNote);
                    notes.remove(swipedNote);
                    adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    showEmptyView();
                })
                .setNegativeButton("Cancel", ( dialogInterface, i ) -> Objects.requireNonNull(recyclerView.getAdapter()).notifyItemChanged(viewHolder.getAdapterPosition() ))
                .setCancelable(false)
                .create().show();
    }

    private void onDeleteMultiNotes () {
        List<Note> checkedNotes = adapter.getCheckedNotes();
        if ( checkedNotes.size() != 0 ) {
            for (Note note : checkedNotes) dao.deleteNote(note);
            loadNotes();
            Toast.makeText(this, checkedNotes.size() + " Note(s) Delete successfully !", Toast.LENGTH_SHORT).show();
        } else Toast.makeText(this, "No Note(s) selected", Toast.LENGTH_SHORT).show();
    }

    private void onShareNote () {
        Note note = adapter.getCheckedNotes().get(0);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        String notetext = note.getNoteText() + "\n\n Create on : " +
                NoteUtils.dateFromLong(note.getNoteDate()) + "\n  By :" +
                getString(R.string.app_name);
        share.putExtra(Intent.EXTRA_TEXT, notetext);
        startActivity(share);
    }
}