package in.co.saionline.jarvis;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;

public class CallListAdapter extends SimpleCursorAdapter{

    public CallListAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        super.bindView(view, context, cursor);
    }
}
