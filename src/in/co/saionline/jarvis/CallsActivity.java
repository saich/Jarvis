package in.co.saionline.jarvis;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import greendroid.app.ActionBarActivity;
import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.LoaderActionBarItem;

public class CallsActivity extends GDActivity {

    private final String TAG = this.getClass().getSimpleName();

    private static final int VIEW_NO_DATA = 1;
    private static final int VIEW_STATS = 2;

    public static final String INTENT_CONTACT_ID = "jarvis:contact_id";
    public static final String INTENT_CONTACT_LOOKUP_KEY = "jarvis:contact_lookup_key";

    private static final int PICK_CONTACT_RESULT = 1;

    /**
     * Contains the value the last call log updated in the private database
     */
    private long mLastCallLogUpdated = 0;
    private DataHelper mDBHelper;

    private View.OnClickListener mPersonNameClickListener;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() called");
        setActionBarContentView(R.layout.calls_new);
        addActionBarItem(ActionBarItem.Type.Refresh, R.id.action_bar_view_refresh);
        mDBHelper = new DataHelper(getApplicationContext());
        mLastCallLogUpdated = getLastCallLogTime();

        mPersonNameClickListener = new View.OnClickListener() {
            public void onClick(View view) {
                TextView username = (TextView) view;
                long id = (Long) username.getTag(R.id.contact_id);
                String lookup_key = (String) username.getTag(R.id.contact_lookupkey);
                String display_name = username.getText().toString();

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Launching person dashboard with name: " + display_name +
                            ", id: " + id +
                            ", Lookup Key: " + lookup_key);
                }

                Intent intent = new Intent(getApplicationContext(), PersonCallsActivity.class);
                intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, display_name);
                intent.putExtra(INTENT_CONTACT_ID, id);
                intent.putExtra(INTENT_CONTACT_LOOKUP_KEY, lookup_key);
                startActivity(intent);
            }
        };
        updateData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy() called");
    }


    private void updateData() {
        DataHelper.TotalStats stats = mDBHelper.getTotalStats();

        Cursor calls = mDBHelper.getLengthyCalls();
        startManagingCursor(calls);
        String[] columns = new String[]{
                Jarvis.Calls.NUMBER,
                Jarvis.Calls.START_TIME,
                Jarvis.Calls.DURATION
        };

        int[] to = new int[]{
                R.id.call_number,
                R.id.call_time,
                R.id.call_duration
        };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.long_calls_list_entry,
                calls, columns, to);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int col_index) {
                TextView tv = (TextView) view;
                String col_name = cursor.getColumnName(col_index);
                boolean bHandled = true;
                if (col_name.equals(Jarvis.Calls.NUMBER)) {
                    String number = cursor.getString(col_index);
                    boolean bFound = false;
                    Cursor contact = getContactFromNumber(number);
                    if (contact != null) {
                        if (contact.moveToFirst()) {
                            tv.setText(contact.getString(contact.getColumnIndex(PhoneLookup.DISPLAY_NAME)));
                            tv.setTag(R.id.contact_id, contact.getLong(contact.getColumnIndex(PhoneLookup._ID)));
                            tv.setTag(R.id.contact_lookupkey, contact.getString(contact.getColumnIndex(PhoneLookup.LOOKUP_KEY)));
                            tv.setOnClickListener(mPersonNameClickListener);
                            bFound = true;
                        }
                        contact.close();
                    }

                    if (!bFound) {
                        tv.setText(number);
                    }
                } else if (col_name.equals(Jarvis.Calls.START_TIME)) {
                    long start_time = cursor.getLong(col_index);
                    tv.setText(DateUtils.getRelativeTimeSpanString(start_time));
                } else if (col_name.equals(Jarvis.Calls.DURATION)) {
                    long duration = cursor.getLong(col_index);
                    tv.setText(DateUtils.formatElapsedTime(duration));
                } else {
                    bHandled = false;
                }

                return bHandled;
            }
        });

        ListView long_call_list = (ListView) findViewById(R.id.long_calls);
        long_call_list.setAdapter(adapter);

        // Check if we have some valid data... Else, need to switch the layout
        if (stats.incoming_count > 0 || stats.outgoing_count > 0 || stats.missed_count > 0) {

            switchLayout(VIEW_STATS);

            TextView incoming = (TextView) findViewById(R.id.incoming);
            TextView outgoing = (TextView) findViewById(R.id.outgoing);
            TextView missed = (TextView) findViewById(R.id.missed);
            //TextView longest_caller = (TextView) findViewById(R.id.longest_caller_name);
            //TextView longest_time = (TextView) findViewById(R.id.longest_call_duration);

            incoming.setText(statsString(stats.incoming_total_duration, stats.incoming_count));
            outgoing.setText(statsString(stats.outgoing_total_duration, stats.outgoing_count));
            missed.setText(stats.missed_count + " calls");
        } else {
            switchLayout(VIEW_NO_DATA);
        }
    }

    private void switchLayout(int layout) {
        View calls_no_data = findViewById(R.id.calls_no_data);
        View calls_stats = findViewById(R.id.calls_stats);

        switch (layout) {
            case VIEW_NO_DATA:
                calls_no_data.setVisibility(View.VISIBLE);
                calls_stats.setVisibility(View.GONE);
                break;

            case VIEW_STATS:
                calls_no_data.setVisibility(View.GONE);
                calls_stats.setVisibility(View.VISIBLE);
                break;

            default:
                Log.e(TAG, "Unknown layout requested for switching");
        }
    }

    @Override
    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
        Log.d(TAG, "onHandleActionBarItemClick called");
        boolean bHandled;
        switch (item.getItemId()) {
            case R.id.action_bar_view_refresh:
                Log.d(TAG, "Refresh is clicked");
                new UpdateCallsInDatabaseTask().execute();
                bHandled = true;
                break;
            default:
                bHandled = super.onHandleActionBarItemClick(item, position);
        }
        return bHandled;
    }

    private String statsString(long duration, long count) {
        return DateUtils.formatElapsedTime(duration) + " (" + count + " calls)";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.calls_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.choose_contact:
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);

                startActivityForResult(intent, PICK_CONTACT_RESULT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_CONTACT_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    String[] projection = new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts.HAS_PHONE_NUMBER
                    };
                    Cursor c = getContentResolver().query(contactData, projection, null, null, null);
                    if (c != null && c.moveToFirst()) {

                        String display_name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        long id = c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID));
                        String lookup_key = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        Intent intent = new Intent(getApplicationContext(), PersonCallsActivity.class);
                        intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, display_name);
                        intent.putExtra(INTENT_CONTACT_ID, id);
                        intent.putExtra(INTENT_CONTACT_LOOKUP_KEY, lookup_key);
                        startActivity(intent);
                    }
                    if (c != null) c.close();
                    break;
                }
        }
    }

    private long getLastCallLogTime() {
        long lastTime = mDBHelper.getLastCallTime();
        Log.d(TAG, "Retreived Last Call Log Time:" + lastTime);
        return lastTime;
    }

    private void setLastCallLogTime(long lastTime) {
        Log.d(TAG, "Setting Last Call Log Time to: " + lastTime);
        mDBHelper.setLastCallTime(lastTime);
    }

    /**
     * @param number Contact Number
     * @return Cursor from the result. Could be NULL also! Closing it is caller's responsibility
     */
    private Cursor getContactFromNumber(String number) {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        return getContentResolver().query(uri,
                new String[]{
                        PhoneLookup._ID,
                        PhoneLookup.DISPLAY_NAME,
                        PhoneLookup.NUMBER,
                        PhoneLookup.LOOKUP_KEY
                }, null, null, null);
    }

    // TODO: Result must also include last-updated-time
    // TODO: Params must take initial last-updated-time
    private class UpdateCallsInDatabaseTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ((LoaderActionBarItem) getActionBar().getItem(0)).setLoading(true);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            // TODO: Query isCancelled() in loops...
            Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                    CallLog.Calls.DATE + ">" + mLastCallLogUpdated, null,
                    CallLog.Calls.DATE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {

                // Get column indices for required columns
                int col_type = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int col_duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
                int col_start = cursor.getColumnIndex(CallLog.Calls.DATE);
                int col_number = cursor.getColumnIndex(CallLog.Calls.NUMBER);

                do {
                    mDBHelper.insert(cursor.getString(col_number),
                            cursor.getInt(col_type), cursor.getLong(col_start),
                            cursor.getInt(col_duration));

                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        String start_time = DateUtils.formatDateTime(getApplicationContext(),
                                cursor.getLong(col_start), DateUtils.FORMAT_12HOUR
                                | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);

                        String duration = DateUtils.formatElapsedTime(cursor
                                .getInt(col_duration));
                        Log.d(TAG, "Number: " + cursor.getString(col_number) + ", " + "Type: "
                                + cursor.getInt(col_type) + ", " + "At: " + start_time + ", "
                                + "Duration: " + duration);
                    }

                    mLastCallLogUpdated = cursor.getLong(col_start);
                } while (cursor.moveToNext());
            }

            int count = cursor != null ? cursor.getCount() : 0;
            if (cursor != null)
                cursor.close();
            return count;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            if (result > 0) {
                Toast.makeText(getApplicationContext(), "Updated " + result + " records",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Nothing to update...", Toast.LENGTH_SHORT).show();
            }

            // update the last call value in preferences
            setLastCallLogTime(mLastCallLogUpdated);

            // update the UI
            updateData();

            // update the icon in the Action Bar
            ((LoaderActionBarItem) getActionBar().getItem(0)).setLoading(false);
        }
    }
}