package in.co.saionline.jarvis;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import greendroid.app.GDActivity;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;

import java.io.InputStream;
import java.util.ArrayList;

public class PersonCallsActivity extends GDActivity {

    private final String TAG = this.getClass().getSimpleName();

    private Uri mUri;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent != null) {
            long contact_id = intent.getLongExtra(CallsActivity.INTENT_CONTACT_ID, 0);
            String lookup_key = intent.getStringExtra(CallsActivity.INTENT_CONTACT_LOOKUP_KEY);
            Log.d(TAG, "Contact ID: " + contact_id + ", Lookup Key: " + lookup_key);

            Uri lookup_uri = Contacts.getLookupUri(contact_id, lookup_key);
            mUri = Contacts.getLookupUri(getContentResolver(), lookup_uri);
        }

        long contact_id = 0;

        if (mUri != null) {
            // Get contact_id from mUri
            Cursor contact = getContentResolver().query(mUri,
                    new String[]{Contacts._ID}, null, null, null);


            if (contact != null) {
                if (contact.moveToFirst()) {
                    if (contact.getCount() > 1) {
                        Log.w(TAG, "Getting multiple rows from a Contact URI: Using the 1st one only!!");
                    }
                    contact_id = contact.getLong(contact.getColumnIndex(Contacts._ID));
                    Log.d(TAG, "Raw Contact ID: " + contact_id);
                }
                contact.close();
            }
        }

        ArrayList<String> allNumbers = new ArrayList<String>();

        if (contact_id != 0) {
            // Fetch phone numbers of this contact
            Cursor numbers = getContentResolver().query(Phone.CONTENT_URI,
                    new String[]{Phone.NUMBER},
                    Phone.CONTACT_ID + "=" + contact_id,
                    null, null);

            if (numbers != null) {
                if (numbers.moveToFirst()) {
                    do {
                        allNumbers.add(numbers.getString(numbers.getColumnIndex(Phone.NUMBER)));
                    } while (numbers.moveToNext());
                }
                numbers.close();
            }
        }


        if (contact_id == 0) {
            // TODO: Error message of Invalid contact
        } else if (allNumbers.size() == 0) {
            // TODO: Error message - No calls exist
        } else {
            StringBuilder whereParams = new StringBuilder();
            for (String number : allNumbers) {
                Log.d(TAG, "Number:" + number);
                whereParams.append("\"" + number + "\",");
            }
            whereParams.deleteCharAt(whereParams.length() - 1);

            String where = Jarvis.Calls.NUMBER + " IN (" + whereParams.toString() + ")";
            DataHelper db = new DataHelper(this);
            DataHelper.TotalStats stats = db.getTotalStats(where);

            setActionBarContentView(R.layout.person_calls);
            TextView incoming = (TextView) findViewById(R.id.incoming);
            TextView outgoing = (TextView) findViewById(R.id.outgoing);
            TextView missed = (TextView) findViewById(R.id.missed);

            incoming.setText(statsString(stats.incoming_total_duration, stats.incoming_count));
            outgoing.setText(statsString(stats.outgoing_total_duration, stats.outgoing_count));
            missed.setText(stats.missed_count + " calls");

            Cursor calls = getContentResolver().query(Jarvis.Calls.CONTENT_URI, null,
                    where, null,
                    Jarvis.Calls.START_TIME + " DESC");

            startManagingCursor(calls);
            String[] columns = new String[]{
                    Jarvis.Calls.START_TIME,
                    Jarvis.Calls.DURATION
            };

            int[] to = new int[]{
                    R.id.call_time,
                    R.id.call_duration
            };

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.person_all_calls_list_entry,
                    calls, columns, to);

            // Set the image

            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

                public boolean setViewValue(View view, Cursor cursor, int col_index) {
                    TextView tv = (TextView) view;
                    String col_name = cursor.getColumnName(col_index);
                    boolean bHandled = true;
                    if (col_name.equals(Jarvis.Calls.START_TIME)) {
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

            ListView all_calls = (ListView) findViewById(R.id.all_calls);
            all_calls.setAdapter(adapter);

            ImageView img = (ImageView) findViewById(R.id.person_image);
            Uri photo_uri = getPhotoUri(contact_id);
            img.setImageURI(photo_uri);
            if(img.getDrawable() == null) {
                // Set a default image
                img.setImageResource(R.drawable.default_contact_image);
            }

            // grouptsTst(contact_id);
        }
    }

    private String statsString(long duration, long count) {
        return DateUtils.formatElapsedTime(duration) + " (" + count + " calls)";
    }

    // TODO: Check what happens when no image is there?
    // TODO: How to get thumbnail?
    private Uri getPhotoUri(long id) {
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    private void grouptsTst(long contact_id) {
        String where = GroupMembership.CONTACT_ID + "=?" + " AND " + GroupMembership.MIMETYPE + "=?";
        Cursor d = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, where,
                new String[]{
                        String.valueOf(contact_id),
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                }, null);

        StringBuilder groupWhere = new StringBuilder();

        if (d != null) {
            if (d.moveToFirst()) {
                int col_groupid = d.getColumnIndex(GroupMembership.GROUP_SOURCE_ID);
                do {
                    groupWhere.append("\"" + d.getString(col_groupid) + "\",");
                } while (d.moveToNext());

                groupWhere.deleteCharAt(groupWhere.length() - 1);
                Log.e(TAG, groupWhere.toString());
                Cursor c = getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI,
                        new String[]{
                                ContactsContract.Groups._ID,
                                ContactsContract.Groups.SOURCE_ID,
                                ContactsContract.Groups.TITLE,
                                ContactsContract.Groups.ACCOUNT_NAME,
                                ContactsContract.Groups.ACCOUNT_TYPE,
                                ContactsContract.Groups.SYSTEM_ID
                        },
                        ContactsContract.Groups.GROUP_VISIBLE + "=? AND "
                                + ContactsContract.Groups.DELETED + "=?" + " AND "
                                + ContactsContract.Groups.SOURCE_ID + " IN ("
                                + groupWhere.toString() + ")",
                        new String[]{"1", "0"},
                        null);

                if (c != null) {
                    if (c.moveToFirst()) {
                        do {
                            Log.e(TAG, DatabaseUtils.dumpCurrentRowToString(c));
                        } while (c.moveToNext());
                    }
                    c.close();
                }
            }
            d.close();
        }
    }
}