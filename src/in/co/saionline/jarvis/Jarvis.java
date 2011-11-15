package in.co.saionline.jarvis;

import android.net.Uri;
import android.provider.BaseColumns;

public class Jarvis {
    public static final String AUTHORITY = JarvisProvider.class.getName().toLowerCase();

    // This class cannot be instantiated
    private Jarvis() {
    }

    /**
     * Calls Table
     */
    public static final class Calls implements BaseColumns {

        // This class cannot be instantiated
        private Calls() {
        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/calls");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of calls.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.jarvis.call";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single call entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.jarvis.call";

        /**
         * The phone number on the other of the call
         * <P>Type: STRING</P>
         */
        public static final String NUMBER = "number";

        /**
         * The type of the call
         * <P>Type: INTEGER</P>
         * To identify to type, use the constants: CallLog.Calls.INCOMING_TYPE /
         * CallLog.Calls.OUTGOING_TYPE / CallLog.Calls.MISSED_TYPE
         */
        public static final String TYPE = "type";
        /**
         * The timestamp for when the call has begun
         * <P>Type: INTEGER</P>
         */
        public static final String START_TIME = "start";

        /**
         * The duration (in milliseconds) of the call
         * <P>Type: INTEGER</P>
         */
        public static final String DURATION = "duration";
    }

    /**
     * The internal Application Data table
     */
    public static class AppData implements BaseColumns {

        // This class cannot be instantiated
        private AppData() {
        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/appdata");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of Application data.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.jarvis.appdata";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single Application data entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.jarvis.appdata";

        public static final String NAME = "name";
        public static final String VALUE = "value";
        public static final String MODIFIED_AT = "modified";
    }
}
