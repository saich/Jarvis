package in.co.saionline.jarvis;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;

public class DashboardActivity extends GDActivity {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() called");
        setActionBarContentView(R.layout.dashboard);
        addActionBarItem(ActionBarItem.Type.Info, R.id.action_bar_view_info);
        DashboardClickListener listener = new DashboardClickListener();
        findViewById(R.id.dashboard_button_calls).setOnClickListener(listener);
        findViewById(R.id.dashboard_button_music).setOnClickListener(listener);
    }

    @Override
    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
        boolean bHandled = false;
        int id = item.getItemId();
        switch (id) {
            case R.id.action_bar_view_info:
                startActivity(new Intent(this, AboutActivity.class));
                bHandled = true;
                break;

            default:
                super.onHandleActionBarItemClick(item, position);
        }
        return bHandled;
    }

    private class DashboardClickListener implements OnClickListener {
        public void onClick(View v) {
            Intent i = null;
            switch (v.getId()) {
                case R.id.dashboard_button_calls:
                    i = new Intent(DashboardActivity.this, CallsActivity.class);
                    break;
                case R.id.dashboard_button_music:
                    i = new Intent(DashboardActivity.this, PlaylistActivity.class);
                    break;
            }
            if (i != null) {
                startActivity(i);
            }
        }
    }
}