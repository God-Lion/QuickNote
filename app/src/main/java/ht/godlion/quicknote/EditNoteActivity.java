package ht.godlion.quicknote;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Date;

import ht.godlion.quicknote.db.NotesDB;
import ht.godlion.quicknote.db.NotesDao;
import ht.godlion.quicknote.model.Note;

public class EditNoteActivity extends AppCompatActivity {
    private EditText inputNote;
    private NotesDao dao;
    private Note temp;
    public static final String NOTE_EXTRA_Key = "note_id";

    /**
     * This function sets up the activity for editing a note, including setting the layout, toolbar,
     * and retrieving the note text if it exists.
     * 
     * @param savedInstanceState The savedInstanceState parameter is a Bundle object that contains the
     * data that was saved in the onSaveInstanceState() method. It is used to restore the activity's
     * previous state, such as the values of its variables, when the activity is recreated after being
     * destroyed and then recreated.
     */
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edite_note);
        Toolbar toolbar = findViewById(R.id.edit_note_activity_toolbar);
        setSupportActionBar(toolbar);
        inputNote = findViewById(R.id.input_note);
        dao = NotesDB.getInstance(this).notesDao();
        if (getIntent().getExtras() != null) {
            int id = getIntent().getExtras().getInt(NOTE_EXTRA_Key, 0);
            temp = dao.getNoteById(id);
            inputNote.setText(temp.getNoteText());
        } else inputNote.setFocusable(true);
    }

    /**
     * The function is used to create the options menu for an activity in an Android app.
     * 
     * @param menu The menu parameter is an object of the Menu class. It represents the menu that will
     * be displayed in the activity's action bar.
     * @return The method is returning a boolean value.
     */
    @Override
    public boolean onCreateOptionsMenu ( Menu menu ) {
        getMenuInflater().inflate(R.menu.edite_note_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

  /**
   * The function checks if the selected menu item is the "save_note" item and calls the onSaveNote()
   * method if it is.
   * 
   * @param item The `item` parameter is the menu item that was selected by the user.
   * @return The method is returning a boolean value.
   */
    @Override
    public boolean onOptionsItemSelected ( MenuItem item ) {
        int id = item.getItemId();
        if (id == R.id.save_note) onSaveNote();
        return super.onOptionsItemSelected(item);
    }

    /**
     * The function saves a note by getting the text from an input field, creating a new Note object
     * with the text and current date, and either inserting it into the database or updating an
     * existing note.
     */
    private void onSaveNote () {
        String text = inputNote.getText().toString();
        if ( !text.isEmpty() ) {
            long date = new Date().getTime();
            if ( temp == null ) {
                temp = new Note(text, date);
                dao.insertNote(temp);
            } else {
                temp.setNoteText(text);
                temp.setNoteDate(date);
                dao.updateNote(temp);
            }
            finish();
        }
    }
}
