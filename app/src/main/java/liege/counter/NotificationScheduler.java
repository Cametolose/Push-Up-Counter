package liege.counter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

public class NotificationScheduler {

    private static final String NOTIF_PREFS    = "NotificationPrefs";
    private static final String KEY_NOTIF_HOUR = "notifHour";
    private static final String KEY_NOTIF_MIN  = "notifMinute";

    public static void scheduleStreakAlarm(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(NOTIF_PREFS, Context.MODE_PRIVATE);
        int hour   = prefs.getInt(KEY_NOTIF_HOUR, 8);
        int minute = prefs.getInt(KEY_NOTIF_MIN, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_STREAK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 100, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {
            // Exact alarm permission not granted on Android 12+; fall back to inexact alarm
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        } else {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pi);
        }
    }

    public static void cancelStreakAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_STREAK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 100, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pi);
    }

    public static void setNotificationTime(Context context, int hour, int minute) {
        SharedPreferences prefs = context.getSharedPreferences(NOTIF_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_NOTIF_HOUR, hour).putInt(KEY_NOTIF_MIN, minute).apply();
        scheduleStreakAlarm(context);
    }

    public static int getNotifHour(Context context) {
        return context.getSharedPreferences(NOTIF_PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_NOTIF_HOUR, 8);
    }

    public static int getNotifMinute(Context context) {
        return context.getSharedPreferences(NOTIF_PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_NOTIF_MIN, 0);
    }
}
