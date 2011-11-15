package in.co.saionline.jarvis;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;
import android.util.Log;

import java.util.Date;

public class DataHelper {

    public static class TotalStats {
        public long incoming_count;
        public long outgoing_count;
        public long missed_count;
        public long incoming_total_duration;
        public long outgoing_total_duration;

        public TotalStats() {
            incoming_count = 0;
            outgoing_count = 0;
            missed_count = 0;
            incoming_total_duration = 0;
            outgoing_total_duration = 0;
        }

        public void append(TotalStats stats) {
            if (stats != null) {
                this.incoming_count += stats.incoming_count;
                this.outgoing_count += stats.outgoing_count;
                this.missed_count += stats.missed_count;
                this.incoming_total_duration += stats.incoming_total_duration;
                this.outgoing_total_duration += stats.outgoing_total_duration;
            }
        }

        public String dumpToString() {
            return "Incoming: " + this.incoming_count + " / " + this.incoming_total_duration +
                    ", Outgoing: " + this.outgoing_count + " / " + this.outgoing_total_duration +
                    ", Missed: " + this.missed_count;
        }
    }

    private final String TAG = this.getClass().getSimpleName();

    private Context mContext;
    private static final String LAST_CALL_LOG_TIME = "last_call_log_time";

    public DataHelper(Context context) {
        mContext = context;
        if (mContext == null) throw new IllegalArgumentException("Context cannot be NULL");
    }

    public long getLastCallTime() {
        long last_call_time = 0;
        Cursor cursor = mContext.getContentResolver().query(Jarvis.AppData.CONTENT_URI, null,
                Jarvis.AppData.NAME + "=?", new String[]{LAST_CALL_LOG_TIME},
                Jarvis.AppData._ID + " DESC");
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                last_call_time = Long.parseLong(cursor.getString(cursor.getColumnIndex(Jarvis.AppData.VALUE)));
            }
            cursor.close();
        }
        return last_call_time;
    }

    public boolean setLastCallTime(long timestamp) {
        boolean bSuccess = false;
        long id_to_update = -1;
        Cursor cursor = mContext.getContentResolver().query(Jarvis.AppData.CONTENT_URI, null,
                Jarvis.AppData.NAME + "=?", new String[]{LAST_CALL_LOG_TIME},
                Jarvis.AppData._ID + " DESC");
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id_to_update = Long.parseLong(cursor.getString(cursor.getColumnIndex(Jarvis.AppData._ID)));
            }
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(Jarvis.AppData.NAME, LAST_CALL_LOG_TIME);
        values.put(Jarvis.AppData.VALUE, String.valueOf(timestamp));
        values.put(Jarvis.AppData.MODIFIED_AT, new Date().getTime());

        if (id_to_update > -1) {
            // Update the id
            int rows_affected = mContext.getContentResolver().update(Jarvis.AppData.CONTENT_URI, values,
                    Jarvis.AppData._ID + '=' + id_to_update, null);
            if (rows_affected <= 0) {
                Log.e(TAG, "Updating the last row for the 'Last call update time' failed!");
            } else {
                bSuccess = true;
            }
        } else {
            // Create a new entry
            try {
                mContext.getContentResolver().insert(Jarvis.AppData.CONTENT_URI, values);
                bSuccess = true;
            } catch (SQLException e) {
                Log.e(TAG, "Creating new row for the 'Last call update time' failed!");
            }
        }
        return bSuccess;
    }

    public Uri insert(String number, int type, long start_time, long duration) {
        ContentValues values = createContentValues(number, type, start_time, duration);
        return mContext.getContentResolver().insert(Jarvis.Calls.CONTENT_URI, values);
    }

    public Cursor getLengthyCalls() {
        return mContext.getContentResolver().query(Jarvis.Calls.CONTENT_URI,
                null, null, null,
                Jarvis.Calls.DURATION + " DESC LIMIT 10"); // HACK: Limit Usage
    }

    private static ContentValues createContentValues(String number, int type, long start_time, long duration) {
        ContentValues values = new ContentValues();
        values.put(Jarvis.Calls.NUMBER, number);
        values.put(Jarvis.Calls.TYPE, type);
        values.put(Jarvis.Calls.START_TIME, start_time);
        values.put(Jarvis.Calls.DURATION, duration);
        return values;
    }

    public TotalStats getTotalStats() {
        return getTotalStats(null);
    }

    public TotalStats getTotalStats(String where) {
        TotalStats stats = new TotalStats();
        try {
            // Group the results by calltype
            String newWhere = TextUtils.isEmpty(where) ? "1=1" : where;
            newWhere += ") GROUP BY (" + Jarvis.Calls.TYPE; // HACK: GROUP BY usage

            Cursor cursor = mContext.getContentResolver().query(Jarvis.Calls.CONTENT_URI,
                    new String[]{
                            "SUM(" + Jarvis.Calls.DURATION + ")",
                            "COUNT(*)",
                            Jarvis.Calls.TYPE
                    }, newWhere, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        long sum = cursor.getLong(0);
                        long count = cursor.getLong(1);
                        int type = cursor.getInt(2);
                        switch (type) {
                            case CallLog.Calls.INCOMING_TYPE:
                                stats.incoming_count = count;
                                stats.incoming_total_duration = sum;
                                break;
                            case CallLog.Calls.OUTGOING_TYPE:
                                stats.outgoing_count = count;
                                stats.outgoing_total_duration = sum;
                                break;
                            case CallLog.Calls.MISSED_TYPE:
                                stats.missed_count = count;
                                break;
                            default:
                                Log.w(TAG, "An unknown call type is found in the database. Ignoring it!");
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error occured while querying the db for Stats. Exception: " + ex.getMessage());
        }
        return stats;
    }
}
