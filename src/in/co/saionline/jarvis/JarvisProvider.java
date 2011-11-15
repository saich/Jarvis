package in.co.saionline.jarvis;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.database.SQLException;
import android.text.TextUtils;

import java.util.HashMap;

public class JarvisProvider extends ContentProvider {

    public static final String AUTHORITY = JarvisProvider.class.getName().toLowerCase();

    private static HashMap<String, String> sCallsProjectionMap;
    private static HashMap<String, String> sAppDataProjectionMap;

    private static final int CALLS = 1;
    private static final int CALL_ID = 2;
    private static final int APPDATA = 3;
    private static final int APPDATA_ID = 4;

    private static final UriMatcher sUriMatcher;

    private DatabaseHelper mOpenHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "calls", CALLS);
        sUriMatcher.addURI(AUTHORITY, "calls/#", CALL_ID);
        sUriMatcher.addURI(AUTHORITY, "appdata", APPDATA);
        sUriMatcher.addURI(AUTHORITY, "appdata/#", APPDATA_ID);

        sCallsProjectionMap = new HashMap<String, String>();
        sCallsProjectionMap.put(Jarvis.Calls._ID, Jarvis.Calls._ID);
        sCallsProjectionMap.put(Jarvis.Calls.NUMBER, Jarvis.Calls.NUMBER);
        sCallsProjectionMap.put(Jarvis.Calls.TYPE, Jarvis.Calls.TYPE);
        sCallsProjectionMap.put(Jarvis.Calls.START_TIME, Jarvis.Calls.START_TIME);
        sCallsProjectionMap.put(Jarvis.Calls.DURATION, Jarvis.Calls.DURATION);

        sAppDataProjectionMap = new HashMap<String, String>();
        sAppDataProjectionMap.put(Jarvis.AppData._ID, Jarvis.AppData._ID);
        sAppDataProjectionMap.put(Jarvis.AppData.NAME, Jarvis.AppData.NAME);
        sAppDataProjectionMap.put(Jarvis.AppData.VALUE, Jarvis.AppData.VALUE);
        sAppDataProjectionMap.put(Jarvis.AppData.MODIFIED_AT, Jarvis.AppData.MODIFIED_AT);
    }

    @Override
    public boolean onCreate() {
        /**
         * A content provider is created when its hosting process is created, and remains
         * around for as long as the process does, so there is no need to close the database.
         * It will get closed as part of the kernel cleaning up the process's resources
         * when the process is killed.
         *
         * @see <a href="https://groups.google.com/d/msg/android-developers/NwDRpHUXt0U/jIam4Q8-cqQJ">An Android Engineer's Reply</a>
         */
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case CALLS:
                qb.setTables(DatabaseHelper.TABLE_CALLS);
                // HACK: Removing the projection map to enable SUM, COUNT in column names
                //qb.setProjectionMap(sCallsProjectionMap);
                break;
            case CALL_ID:
                qb.setTables(DatabaseHelper.TABLE_CALLS);
                qb.setProjectionMap(sCallsProjectionMap);
                qb.appendWhere(Jarvis.Calls._ID + "=" + uri.getPathSegments().get(1));
                break;
            case APPDATA:
                qb.setTables(DatabaseHelper.TABLE_APPDATA);
                qb.setProjectionMap(sAppDataProjectionMap);
                break;
            case APPDATA_ID:
                qb.setTables(DatabaseHelper.TABLE_APPDATA);
                qb.setProjectionMap(sAppDataProjectionMap);
                qb.appendWhere(Jarvis.AppData._ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI:" + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case CALLS:
                return Jarvis.Calls.CONTENT_TYPE;
            case CALL_ID:
                return Jarvis.Calls.CONTENT_ITEM_TYPE;
            case APPDATA:
                return Jarvis.AppData.CONTENT_TYPE;
            case APPDATA_ID:
                return Jarvis.AppData.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI:" + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = sUriMatcher.match(uri);
        if (match != CALLS && match != APPDATA)
            throw new IllegalArgumentException("Invalid URI:" + uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(match == CALLS ? DatabaseHelper.TABLE_CALLS : DatabaseHelper.TABLE_APPDATA, null, values);

        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(
                    match == CALLS ? Jarvis.Calls.CONTENT_URI : Jarvis.AppData.CONTENT_URI,
                    rowId);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case CALLS:
                count = db.delete(DatabaseHelper.TABLE_CALLS, where, whereArgs);
                break;
            case CALL_ID:
                String callId = uri.getPathSegments().get(1);
                count = db.delete(DatabaseHelper.TABLE_CALLS, Jarvis.Calls._ID + "=" + callId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            case APPDATA:
                count = db.delete(DatabaseHelper.TABLE_APPDATA, where, whereArgs);
                break;
            case APPDATA_ID:
                count = db.delete(DatabaseHelper.TABLE_APPDATA,
                        Jarvis.AppData._ID + "=" + uri.getPathSegments().get(1) +
                                (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Invalid URI:" + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case CALLS:
                count = db.update(DatabaseHelper.TABLE_CALLS, values, where, whereArgs);
                break;
            case CALL_ID:
                String callId = uri.getPathSegments().get(1);
                count = db.update(DatabaseHelper.TABLE_CALLS, values, Jarvis.Calls._ID + "=" + callId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            case APPDATA:
                count = db.update(DatabaseHelper.TABLE_APPDATA, values, where, whereArgs);
                break;
            case APPDATA_ID:
                count = db.update(DatabaseHelper.TABLE_APPDATA, values,
                        Jarvis.AppData._ID + "=" + uri.getPathSegments().get(1) +
                                (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Invalid URI:" + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
