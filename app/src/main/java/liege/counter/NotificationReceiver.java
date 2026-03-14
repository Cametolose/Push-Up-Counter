package liege.counter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String ACTION_STREAK = "liege.counter.ACTION_STREAK_NOTIFICATION";

    private static final String NOTIF_PREFS              = "NotificationPrefs";
    private static final String KEY_STREAK_ENABLED        = "streakNotifEnabled";
    private static final String KEY_LAST_STREAK_NOTIF     = "lastStreakNotifDay";
    private static final int    MIN_STREAK_FOR_NOTIF      = 3;
    private static final int    MIN_PUSHUPS_FOR_STREAK_DAY = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (ACTION_STREAK.equals(action)) {
            handleStreakNotification(context);
        }
    }

    private void handleStreakNotification(Context context) {
        SharedPreferences notifPrefs = context.getSharedPreferences(NOTIF_PREFS, Context.MODE_PRIVATE);
        boolean streakEnabled = notifPrefs.getBoolean(KEY_STREAK_ENABLED, true);
        if (!streakEnabled) return;

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
        String lastNotifDay = notifPrefs.getString(KEY_LAST_STREAK_NOTIF, "");
        if (today.equals(lastNotifDay)) return;

        SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String logJson = appPrefs.getString("dailyPushupLog", "{}");
        java.util.HashMap<String, Integer> dailyLog = gson.fromJson(
                logJson, new com.google.gson.reflect.TypeToken<java.util.HashMap<String, Integer>>(){}.getType());
        if (dailyLog == null) return;

        int streak = computeStreak(dailyLog);
        if (streak > MIN_STREAK_FOR_NOTIF) {
            NotificationHelper.sendStreakNotification(context, streak);
            notifPrefs.edit().putString(KEY_LAST_STREAK_NOTIF, today).apply();
        }
    }

    private int computeStreak(java.util.HashMap<String, Integer> log) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String todayKey = keyFor(cal);
        if (log.getOrDefault(todayKey, 0) < MIN_PUSHUPS_FOR_STREAK_DAY) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        }
        int streak = 0;
        for (int i = 0; i < 365; i++) {
            if (log.getOrDefault(keyFor(cal), 0) >= MIN_PUSHUPS_FOR_STREAK_DAY) {
                streak++;
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    private String keyFor(java.util.Calendar cal) {
        return String.format(java.util.Locale.US, "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
    }
}
