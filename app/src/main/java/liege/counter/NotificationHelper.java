package liege.counter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class NotificationHelper {

    public static final String CHANNEL_STREAK      = "streak_channel";
    public static final String CHANNEL_LEADERBOARD = "leaderboard_channel";

    private static final String[] STREAK_MESSAGES = {
        "\uD83D\uDD25 Du bist schon {streak} Tage am St\u00fcck dabei! Bleib dran!",
        "\uD83D\uDCAA {streak} Tage Streak! Du bist unaufhaltsam!",
        "\uD83C\uDFC6 Schon {streak} Tage in Folge! Mach heute wieder deine Push-Ups!",
        "\u26A1 Dein {streak}-Tage-Streak wartet auf dich! Nicht jetzt aufh\u00f6ren!",
        "\uD83C\uDFAF {streak} Tage Streak \u2013 du machst das gro\u00dfartig! Weiter so!",
        "\uD83D\uDD25 Feuer am brennen! {streak} Tage Streak \u2013 lass ihn nicht erl\u00f6schen!",
        "\uD83D\uDCA5 Stark! {streak} Tage am St\u00fcck! Push-Ups warten auf dich!",
    };

    private static final String[] LEADERBOARD_MESSAGES = {
        "\uD83D\uDCCA Jemand hat dich auf dem Leaderboard \u00fcberholt! Zeit f\u00fcr mehr Push-Ups!",
        "\uD83C\uDFC3 Du wurdest \u00fcberholt! Zeig, was du kannst!",
        "\uD83D\uDCC8 Ein anderer Spieler ist an dir vorbeigezogen. Zeit zu trainieren!",
        "\uD83D\uDCAA Du bist auf dem Leaderboard zur\u00fcckgefallen \u2013 hol auf!",
        "\uD83C\uDFAF Jemand hat deinen Platz \u00fcbernommen. Zur\u00fcckschlagen!",
    };

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);

            NotificationChannel streakChannel = new NotificationChannel(
                    CHANNEL_STREAK, "Streak Erinnerungen",
                    NotificationManager.IMPORTANCE_DEFAULT);
            streakChannel.setDescription("T\u00e4gliche Erinnerung an deinen Push-Up Streak");
            nm.createNotificationChannel(streakChannel);

            NotificationChannel lbChannel = new NotificationChannel(
                    CHANNEL_LEADERBOARD, "Leaderboard Updates",
                    NotificationManager.IMPORTANCE_DEFAULT);
            lbChannel.setDescription("Benachrichtigung wenn jemand dich \u00fcberholt");
            nm.createNotificationChannel(lbChannel);
        }
    }

    public static void sendStreakNotification(Context context, int streak) {
        String message = STREAK_MESSAGES[new Random().nextInt(STREAK_MESSAGES.length)]
                .replace("{streak}", String.valueOf(streak));

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_STREAK)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Push-Up Streak!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(1001, builder.build());
        } catch (SecurityException e) {
            // Permission not granted
        }
    }

    public static void sendLeaderboardNotification(Context context) {
        String message = LEADERBOARD_MESSAGES[new Random().nextInt(LEADERBOARD_MESSAGES.length)];

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_LEADERBOARD)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Leaderboard Update")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(1002, builder.build());
        } catch (SecurityException e) {
            // Permission not granted
        }
    }
}
