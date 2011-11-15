package in.co.saionline.jarvis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallEventsReceiver extends BroadcastReceiver {
	
	private final String TAG = this.getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "onReceive() called!");
		
		String action = intent.getAction();

		if(action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
			Log.v(TAG, "PhoneStateChanged Event received");
		}
	}

}