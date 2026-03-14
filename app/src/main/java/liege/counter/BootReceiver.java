package liege.counter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles the BOOT_COMPLETED system broadcast to reschedule the streak alarm. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationScheduler.scheduleStreakAlarm(context);
        }
    }
}
