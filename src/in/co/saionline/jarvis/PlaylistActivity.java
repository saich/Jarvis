package in.co.saionline.jarvis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import greendroid.app.GDActivity;

import java.io.File;

public class PlaylistActivity extends GDActivity {

    private final String TAG = this.getClass().getSimpleName();

    private static final int DIALOG_DELETE_PLAYLIST_CONFIRM = 1;
    private static final int DIALOG_DELETE_PLAYLIST_OTHER = 2;
    private static final int DIALOG_DELETE_PLAYLIST_PROGRESS = 3;
    private static final int DIALOG_REMOVE_DUPLICATES_PROGRESS = 4;

    private static final String PLAYLIST_WHITELISTED = "del";

    private Spinner mPlaylists;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the layout
        setActionBarContentView(R.layout.music);
        mPlaylists = (Spinner) findViewById(R.id.playlist);
        Button deleteSongs = (Button) findViewById(R.id.delete_songs);
        Button removeDuplicates = (Button) findViewById(R.id.remove_duplicates);
        Button removeAllDuplicates = (Button) findViewById(R.id.remove_all_duplicates);

        // Handler for "Delete Songs"
        deleteSongs.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "'Delete Songs' button clicked!");
                Cursor selected = (Cursor) mPlaylists.getSelectedItem();
                if (selected != null) {
                    showDialog(DIALOG_DELETE_PLAYLIST_CONFIRM);
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.delete_songs_select_playlist, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Handler for "Remove Duplicates"
        removeDuplicates.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "'Remove Duplicates' button clicked!");
                Cursor selected = (Cursor) mPlaylists.getSelectedItem();
                if (selected != null) {
                    int id = selected.getInt(selected
                            .getColumnIndex(Playlists._ID));
                    new PlaylistRemoveDuplicatesTask().execute(id);
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.remove_duplicates_select_playlist, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Handler for "Remove duplicate entries from all playlists"
        removeAllDuplicates.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "'Remove duplicates from all playlists' button clicked!");
                new AllPlaylistDuplicatesRemovalTask().execute();
            }
        });

        // Get the playlists & convert it to an adapter
        Cursor cursor = getPlaylists();
        String[] columns = new String[]{Playlists.NAME};
        int[] to = new int[]{android.R.id.text1};
        SimpleCursorAdapter playlistAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, cursor, columns, to);
        playlistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Populate playlists in the UI
        mPlaylists.setAdapter(playlistAdapter);
    }

    private Cursor getPlaylists() {
        String[] projection = {Playlists._ID, Playlists.NAME};
        return managedQuery(Playlists.EXTERNAL_CONTENT_URI,
                projection, null, null, Playlists._ID + " ASC");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_DELETE_PLAYLIST_CONFIRM: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure you want to delete all songs in the playlist?")
                        .setTitle(R.string.confirm_title)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Cursor selected = (Cursor) mPlaylists.getSelectedItem();

                                if (selected != null) {
                                    String name = selected.getString(selected
                                            .getColumnIndex(Playlists.NAME));

                                    if (name.equals(PLAYLIST_WHITELISTED)) {
                                        int id = selected.getInt(selected
                                                .getColumnIndex(Playlists._ID));
                                        new PlaylistDeleteTask().execute(id);
                                    } else {
                                        showDialog(DIALOG_DELETE_PLAYLIST_OTHER);
                                    }
                                }
                            }
                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // DO nothing on cancel....
                    }
                });
                dialog = builder.create();
                break;
            }

            case DIALOG_DELETE_PLAYLIST_OTHER: {
                // Create the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                        .setMessage(
                                "Fool! Already forgot what you did?\nNo, you can't delete songs from any playlist other than 'del'")
                        .setTitle("I can but I won't")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing....
                            }
                        });
                dialog = builder.create();
                break;
            }

            case DIALOG_DELETE_PLAYLIST_PROGRESS: {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setMessage("Deleting... Please wait..");
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setCancelable(false);
                dialog = mProgressDialog;
                break;
            }

            case DIALOG_REMOVE_DUPLICATES_PROGRESS: {
                ProgressDialog progress = new ProgressDialog(this);
                progress.setMessage("Removing duplicates.. Please wait..");
                progress.setCancelable(false);
                dialog = progress;
                break;
            }

        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(final int id, final Dialog dialog) {
        switch (id) {
            case DIALOG_DELETE_PLAYLIST_CONFIRM: {
                Cursor selected = (Cursor) mPlaylists.getSelectedItem();
                if (selected != null) {
                    String name = selected.getString(selected
                            .getColumnIndex(Playlists.NAME));
                    ((AlertDialog) dialog)
                            .setMessage("Are you sure you want to delete all songs in the playlist: '"
                                    + name + "' ?");
                }
                break;
            }

            case DIALOG_DELETE_PLAYLIST_PROGRESS: {
                // Reset the progress to 0/0 ...
                ProgressDialog pDialog = ((ProgressDialog) dialog);
                pDialog.setProgress(0);
                pDialog.setMax(0);
                break;
            }

        }
    }

    private class AllPlaylistDuplicatesRemovalTask extends AsyncTask<Void, Void, Integer> {

        private final String TAG = this.getClass().getSimpleName();

        @Override
        protected void onPreExecute() {
            // Show a progress dialog
            showDialog(DIALOG_REMOVE_DUPLICATES_PROGRESS);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int duplicates = 0;

            String[] playlist_projection = new String[]{
                    Playlists._ID,
                    Playlists.NAME};

            Cursor playlists = getContentResolver().query(
                    Playlists.EXTERNAL_CONTENT_URI, playlist_projection, null,
                    null, null);

            if (playlists != null && playlists.moveToFirst()) {
                int col_pl_id = playlists.getColumnIndex(Playlists._ID);
                int col_pl_name = playlists.getColumnIndex(Playlists.NAME);

                do {
                    int id = playlists.getInt(col_pl_id);
                    Log.d(TAG, "Processing playlist: " + playlists.getString(col_pl_name)
                            + " to remove duplicates");

                    // Get the songlist in the Playlist
                    String[] projection = {
                            Playlists.Members._ID,
                            Playlists.Members.DATA,
                            Playlists.Members.TITLE};

                    String selection = "1=1) GROUP BY "
                            + Playlists.Members.DATA + " HAVING (count("
                            + Playlists.Members.DATA + ") > 1";

                    Uri playlist_uri = Playlists.Members.getContentUri(
                            "external", id);

                    Cursor songs = getContentResolver().query(playlist_uri, projection,
                            selection, null, null);

                    if (songs != null && songs.moveToFirst()) {
                        int col_data = songs
                                .getColumnIndex(Playlists.Members.DATA);
                        do {
                            String path = songs.getString(col_data);

                            // Query to get duplicate entries
                            Cursor entries = getContentResolver().query(playlist_uri,
                                    projection,
                                    Playlists.Members.DATA + "=\"" + path + "\"",
                                    null, Playlists.Members.PLAY_ORDER + " ASC");

                            if (entries != null && entries.getCount() > 1
                                    && entries.moveToFirst()) {
                                int col_id = entries
                                        .getColumnIndex(Playlists.Members._ID);
                                int col_title = entries
                                        .getColumnIndex(Playlists.Members.TITLE);

                                // This will skip the 1st entry, as desired
                                while (!isCancelled() && entries.moveToNext()) {
                                    long entry_id = entries.getLong(col_id);
                                    String title = entries.getString(col_title);

                                    // Remove the entry from the playlist
                                    int rows_deleted = 0;
                                    rows_deleted = getContentResolver()
                                            .delete(
                                                    playlist_uri,
                                                    Playlists.Members._ID + "="
                                                            + entry_id, null);

                                    Log.d(TAG, "Removed Entry - ID:" + entry_id + ", Title:"
                                            + title + ", Rows deleted:" + rows_deleted);

                                    duplicates++;
                                }
                            }
                            if (entries != null)
                                entries.close();

                        } while (!isCancelled() && songs.moveToNext());
                    }
                    if (songs != null)
                        songs.close();
                } while (!isCancelled() && playlists.moveToNext());
            }
            if (playlists != null)
                playlists.close();

            return duplicates;
        }

        @Override
        protected void onPostExecute(Integer result) {
            dismissDialog(DIALOG_REMOVE_DUPLICATES_PROGRESS);
            Toast.makeText(getApplicationContext(), "Duplicates Removed: " + result,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private class PlaylistRemoveDuplicatesTask extends AsyncTask<Integer, Void, Integer> {

        private final String TAG = this.getClass().getSimpleName();

        @Override
        protected void onPreExecute() {
            // Show a progress dialog
            showDialog(DIALOG_REMOVE_DUPLICATES_PROGRESS);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int id = params[0]; // Playlist id
            int duplicates = 0;

            // Get the songlist in the Playlist
            String[] projection = {
                    Playlists.Members._ID,
                    Playlists.Members.DATA,
                    Playlists.Members.TITLE};

            String selection = "1=1) GROUP BY " + Playlists.Members.DATA
                    + " HAVING (count(" + Playlists.Members.DATA + ") > 1";

            Uri playlist_uri = Playlists.Members.getContentUri("external", id);

            Cursor songs = getContentResolver().query(playlist_uri, projection, selection,
                    null, null);

            if (songs != null && songs.moveToFirst()) {
                int col_data = songs.getColumnIndex(Playlists.Members.DATA);
                do {
                    String path = songs.getString(col_data);

                    // Query to get duplicate entries
                    Cursor entries = getContentResolver().query(playlist_uri, projection,
                            Playlists.Members.DATA + "=\"" + path + "\"", null,
                            Playlists.Members.PLAY_ORDER + " ASC");

                    if (entries != null && entries.getCount() > 1 && entries.moveToFirst()) {
                        int col_id = entries
                                .getColumnIndex(Playlists.Members._ID);
                        int col_title = entries
                                .getColumnIndex(Playlists.Members.TITLE);

                        // This will skip the 1st entry, as desired
                        while (!isCancelled() && entries.moveToNext()) {
                            long entry_id = entries.getLong(col_id);
                            String title = entries.getString(col_title);

                            // Remove the entry from the playlist
                            int rows_deleted = 0;
                            rows_deleted = getContentResolver().delete(playlist_uri,
                                    Playlists.Members._ID + "=" + entry_id, null);

                            Log.d(TAG, "Removed Entry - ID:" + entry_id + ", Title:" + title
                                    + ", Rows deleted:" + rows_deleted);

                            duplicates++;
                        }
                    }
                    if (entries != null)
                        entries.close();

                } while (!isCancelled() && songs.moveToNext());
            }
            if (songs != null)
                songs.close();

            return duplicates;
        }

        @Override
        protected void onPostExecute(Integer result) {
            dismissDialog(DIALOG_REMOVE_DUPLICATES_PROGRESS);
            Toast.makeText(getApplicationContext(), "Duplicates Removed: " + result,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private class PlaylistDeleteTask extends AsyncTask<Integer, Integer, int[]> {

        private final String TAG = this.getClass().getSimpleName();

        private int mTotal = 0;
        private boolean bMaxUpdated = false;

        @Override
        protected void onPreExecute() {
            // Show a progress dialog
            showDialog(DIALOG_DELETE_PLAYLIST_PROGRESS);
        }

        @Override
        protected int[] doInBackground(Integer... ids) {

            int id = ids[0];
            int success = 0;
            int failure = 0;
            int completed = 0;

            // Get the songlist in the Playlist
            String[] projection = {
                    Playlists.Members.ALBUM,
                    Playlists.Members.TITLE,
                    Playlists.Members.DATA};

            Cursor songs = getContentResolver().query(
                    Playlists.Members.getContentUri("external", id), projection,
                    null, null, null);

            mTotal = songs.getCount();

            if (songs != null && songs.moveToFirst()) {
                int col_data = songs.getColumnIndex(Playlists.Members.DATA);
                do {
                    File file = new File(songs.getString(col_data));
                    if (file.exists()) {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted: " + file.getPath());
                            success++;
                        } else {
                            Log.e(TAG, "Failed to delete: " + file.getPath());
                            failure++;
                        }
                    }
                    completed++;

                    // Publish progress incrementally..
                    publishProgress(completed);

                } while (!isCancelled() && songs.moveToNext());
            }

            if (songs != null)
                songs.close(); // Close the cursor...

            if (success > 0) {
                // Send the broadcast to update the media library
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                        + Environment.getExternalStorageDirectory())));
            }

            return new int[]{success, failure};
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (!bMaxUpdated) {
                bMaxUpdated = true;
                mProgressDialog.setMax(mTotal);
            }

            // Update the progress of the progress bar
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(int[] result) {
            int success = result[0];
            int failure = result[1];
            // boolean bCancelled = isCancelled();

            dismissDialog(DIALOG_DELETE_PLAYLIST_PROGRESS);

            if (success == 0 && failure == 0) {
                // Show Toast of "Empty Playlist"
                Toast
                        .makeText(getApplicationContext(), "Empty playlist...", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // Show a toast of "Successful & failed removals"
                String s = "Deleted " + success + " songs..";
                if (failure > 0)
                    s += "Failed to delete " + failure + " songs..";

                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        }
    }
}
