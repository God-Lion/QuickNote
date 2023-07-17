package ht.godlion.quicknote.utils;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The NoteUtils class provides a static method to convert a long value representing a date into a
 * formatted string.
 */

public class NoteUtils {

  /**
   * The function takes a long value representing a time and returns a formatted string representing
   * the date and time.
   * 
   * @param time The "time" parameter is a long value representing a specific point in time, typically
   * measured in milliseconds since January 1, 1970, 00:00:00 GMT.
   * @return The method is returning a formatted date string.
   */
    @NonNull
    public static String dateFromLong (long time ) {
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy 'at' hh:mm aaa", Locale.US);
        return format.format(new Date(time));
    }
}
