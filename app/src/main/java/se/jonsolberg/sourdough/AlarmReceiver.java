package se.jonsolberg.sourdough;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        int id = intent.getIntExtra(Timers.EXTRA_ID, -1);
        if (Timers.ACTION_FIRE.equals(action)) {
            Timers.fire(c, id);
        } else if (Timers.ACTION_CANCEL.equals(action)) {
            Timers.cancel(c, id);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Timers.restore(c);
        }
    }
}
