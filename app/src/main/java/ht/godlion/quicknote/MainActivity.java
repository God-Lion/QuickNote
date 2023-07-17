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

/**
 * The MainActivity class is responsible for managing the main screen of a note-taking app, including
 * displaying a list of notes, handling user interactions, and performing CRUD operations on the notes.
 */
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

  /**
   * The `onCreate` function sets up the main activity by initializing the toolbar, setting the layout,
   * configuring the RecyclerView, setting up a FloatingActionButton, and initializing the NotesDB DAO.
   * 
   * @param savedInstanceState The savedInstanceState parameter is a Bundle object that contains the
   * activity's previously saved state. It is used to restore the activity's state when it is
   * recreated, such as when the device is rotated or when the activity is destroyed and recreated due
   * to a configuration change.
   */
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

    /**
     * The function is used to create the options menu for an Android application.
     * 
     * @param menu The menu parameter is an object of the Menu class. It represents the menu that will
     * be displayed in the activity's action bar.
     * @return The method is returning a boolean value, which is true.
     */
    @Override
    public boolean onCreateOptionsMenu ( Menu menu ) {
        getMenuInflater().inflate( R.menu.menu_main, menu );
        return true;
    }

   /**
    * The function is an override of the onOptionsItemSelected method and returns the result of calling
    * the super class's onOptionsItemSelected method with the given MenuItem parameter.
    * 
    * @param item The parameter "item" is of type MenuItem and represents the menu item that was
    * selected by the user.
    * @return The method is returning the value of the super class's onOptionsItemSelected method.
    */
    @Override
    public boolean onOptionsItemSelected ( @NonNull MenuItem item ) { return super.onOptionsItemSelected(item); }

   /**
    * The function `onResume()` is overridden to call the `loadNotes()` method when the activity
    * resumes.
    */
    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

   /**
    * The function opens the EditNoteActivity and passes the ID of the clicked note as an extra.
    * 
    * @param note The "note" parameter is an object of the Note class. It represents a specific note
    * that the user has clicked on.
    */
    @Override
    public void onNoteClick (Note note ) {
        Intent edit = new Intent( this, EditNoteActivity.class );
        edit.putExtra(NOTE_EXTRA_Key, note.getId());
        startActivity(edit);
    }

  /**
   * The function is triggered when a note is long-clicked and it enables multi-check mode, updates the
   * checked count, and sets up the action mode callback.
   * 
   * @param note The "note" parameter is an object of the Note class, which represents a single note in
   * the application.
   */
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

   /**
    * The onActionModeFinished function sets the multi-check mode to false, sets the listener for the
    * adapter, and makes the fab (floating action button) visible.
    * 
    * @param mode The parameter "mode" is an instance of the ActionMode class, which represents the
    * contextual action mode. It provides methods to control and customize the action mode.
    */
    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        adapter.setMultiCheckMode(false);
        adapter.setListener(this);
        fab.setVisibility(View.VISIBLE);
    }

    // The `swipeToDeleteHelper` is an instance of the `ItemTouchHelper` class, which provides
    // swipe-to-delete functionality for the RecyclerView. It is initialized with a `SimpleCallback`
    // that defines the swipe directions and behavior.
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

    /**
     * The function checks if the notes list is empty and shows or hides the empty view accordingly.
     */
    private void showEmptyView () {
        if ( notes.size() == 0 ) {
            this.recyclerView.setVisibility(View.GONE);
            findViewById(R.id.empty_notes_view).setVisibility(View.VISIBLE);
        } else {
            this.recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_notes_view).setVisibility(View.GONE);
        }
    }

    /**
     * The function loads notes from a data source, sets up an adapter and listener for a RecyclerView,
     * and attaches a swipe-to-delete helper to the RecyclerView.
     */
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

    /**
     * The function starts a new activity to edit a new note.
     */
    private void onAddNewNote () { startActivity(new Intent(this, EditNoteActivity.class)); }

    /**
     * The function `swipeToDelete` displays an alert dialog asking the user to confirm the deletion of
     * a note, and if confirmed, deletes the note from the database, removes it from the list of notes,
     * updates the RecyclerView, and shows an empty view if necessary.
     * 
     * @param swipedNote The swipedNote parameter is the Note object that is being swiped and is to be
     * deleted.
     * @param viewHolder The viewHolder parameter is the ViewHolder object associated with the swiped
     * note in the RecyclerView. It contains information about the view and its position in the
     * RecyclerView.
     */
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

    /**
     * The function onDeleteMultiNotes deletes multiple notes from a list and displays a toast message
     * indicating the number of notes deleted.
     */
    private void onDeleteMultiNotes () {
        List<Note> checkedNotes = adapter.getCheckedNotes();
        if ( checkedNotes.size() != 0 ) {
            for (Note note : checkedNotes) dao.deleteNote(note);
            loadNotes();
            Toast.makeText(this, checkedNotes.size() + " Note(s) Delete successfully !", Toast.LENGTH_SHORT).show();
        } else Toast.makeText(this, "No Note(s) selected", Toast.LENGTH_SHORT).show();
    }

    /**
     * The function "onShareNote" allows the user to share a note by creating an intent with the note's
     * text, creation date, and app name, and starting the share activity.
     */
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