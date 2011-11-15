package in.co.saionline.jarvis;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private final String TAG = this.getClass().getSimpleName();

    private static final String DATABASE_NAME = "jarvis.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_CALLS = "calls";
    public static final String TABLE_APPDATA = "appdata";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v(TAG, "Creating the database");

        db.execSQL("CREATE TABLE " + TABLE_CALLS + " ("
                + Jarvis.Calls._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Jarvis.Calls.NUMBER + " TEXT,"
                + Jarvis.Calls.TYPE + " INTEGER,"
                + Jarvis.Calls.START_TIME + " INTEGER,"
                + Jarvis.Calls.DURATION + " INTEGER"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_APPDATA + " ("
                + Jarvis.AppData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Jarvis.AppData.NAME + " TEXT,"
                + Jarvis.AppData.VALUE + " TEXT,"
                + Jarvis.AppData.MODIFIED_AT + " INTEGER"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALLS);
        onCreate(db);
    }
}
