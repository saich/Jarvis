package in.co.saionline.jarvis;

import android.os.Bundle;
import greendroid.app.GDActivity;

public class HelpActivity extends GDActivity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarContentView(R.layout.help);
    }
}
