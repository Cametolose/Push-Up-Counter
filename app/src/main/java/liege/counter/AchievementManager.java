package liege.counter;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AchievementManager — manages progressive achievements with auto-completion and title rewards.
 *
 * To add a new achievement:
 *   1. Add a new AchievementDef to the ACHIEVEMENTS array below.
 *   2. Ensure the category matches one of the supported categories in getCurrentValue().
 *
 * To add a new title:
 *   1. Add a new Title to the TITLES array below with the desired name and color.
 *   2. Reference the title name in the AchievementDef's titleReward field.
 *
 * Achievement categories:
 *   "pushups"          — total push-ups (counter)
 *   "quests"           — total quests completed (all-time)
 *   "streak"           — current consecutive-day streak
 *   "bonus"            — bonus XP collections (all-time)
 *   "level"            — current level
 *   "daily_pushups"    — push-ups done today
 *   "weekly_pushups"   — push-ups done this week (last 7 days)
 *   "monthly_pushups"  — push-ups done this month (last 30 days)
 */
public class AchievementManager {

    private static final String PREFS_NAME = "AchievementPrefs";
    private static final String KEY_COMPLETED = "completedAchievements";

    private static AchievementManager instance;
    private final SharedPreferences prefs;

    // =====================================================================
    // TITLES — Add new titles here. Color is ARGB int (0xAARRGGBB).
    // =====================================================================
    public static final Title[] TITLES = {
        new Title("Neuling",      0xFF9E9E9E),  // gray
        new Title("Kämpfer",      0xFF4CAF50),  // green
        new Title("Krieger",      0xFF2196F3),  // blue
        new Title("Champion",     0xFF9C27B0),  // purple
        new Title("Meister",      0xFFFF9800),  // orange
        new Title("Legende",      0xFFFFD600),  // gold
        new Title("Titan",        0xFFF44336),  // red
        new Title("Unsterblich",  0xFFE040FB),  // pink
        new Title("Questjäger",   0xFF00BCD4),  // teal
        new Title("Questmeister", 0xFFFF5722),  // deep orange
        new Title("Ausdauernd",   0xFF8BC34A),  // light green
        new Title("Unaufhaltsam", 0xFFFF1744),  // red accent
        new Title("Marathonläufer", 0xFF3F51B5), // indigo
        new Title("Wochenkrieger", 0xFF009688), // teal dark
        new Title("Monatsmeister", 0xFFCDDC39), // lime
    };

    // =====================================================================
    // ACHIEVEMENTS — Add new achievements here. Easy to expand!
    // =====================================================================
    public static final AchievementDef[] ACHIEVEMENTS = {
        // --- Total Push-ups ---
        new AchievementDef("pushups_100",   "Erste Schritte",      "Mache 100 Liegestütze insgesamt",   "pushups", 100,   "💪", "Neuling",       0xFF9E9E9E),
        new AchievementDef("pushups_500",   "Halb Tausend",        "Mache 500 Liegestütze insgesamt",   "pushups", 500,   "💪", "Kämpfer",       0xFF4CAF50),
        new AchievementDef("pushups_1000",  "Tausender-Club",      "Mache 1.000 Liegestütze insgesamt", "pushups", 1000,  "💪", "Krieger",       0xFF2196F3),
        new AchievementDef("pushups_3000",  "Drei Tausend stark",  "Mache 3.000 Liegestütze insgesamt", "pushups", 3000,  "💪", "Champion",      0xFF9C27B0),
        new AchievementDef("pushups_5000",  "Fünf Tausend Power",  "Mache 5.000 Liegestütze insgesamt", "pushups", 5000,  "💪", "Meister",       0xFFFF9800),
        new AchievementDef("pushups_10000", "Zehntausend Legende", "Mache 10.000 Liegestütze insgesamt","pushups", 10000, "💪", "Legende",       0xFFFFD600),
        new AchievementDef("pushups_25000", "Unaufhaltsam",        "Mache 25.000 Liegestütze insgesamt","pushups", 25000, "💪", "Titan",         0xFFF44336),
        new AchievementDef("pushups_50000", "Unsterbliche Kraft",  "Mache 50.000 Liegestütze insgesamt","pushups", 50000, "💪", "Unsterblich",   0xFFE040FB),

        // --- Quests Completed ---
        new AchievementDef("quests_10",  "Quest-Anfänger",    "Schließe 10 Quests ab",   "quests", 10,  "📋", "Questjäger",   0xFF00BCD4),
        new AchievementDef("quests_30",  "Quest-Jäger",       "Schließe 30 Quests ab",   "quests", 30,  "📋", null,            0),
        new AchievementDef("quests_90",  "Quest-Veteran",     "Schließe 90 Quests ab",   "quests", 90,  "📋", null,            0),
        new AchievementDef("quests_180", "Quest-Champion",    "Schließe 180 Quests ab",  "quests", 180, "📋", "Questmeister", 0xFFFF5722),
        new AchievementDef("quests_365", "Quest-Meister",     "Schließe 365 Quests ab",  "quests", 365, "📋", null,            0),

        // --- Streak ---
        new AchievementDef("streak_3",   "Drei Tage stark",   "Erreiche einen 3-Tage-Streak",   "streak", 3,   "🔥", null,             0),
        new AchievementDef("streak_7",   "Eine Woche durch",  "Erreiche einen 7-Tage-Streak",   "streak", 7,   "🔥", "Ausdauernd",    0xFF8BC34A),
        new AchievementDef("streak_14",  "Zwei Wochen Feuer", "Erreiche einen 14-Tage-Streak",  "streak", 14,  "🔥", null,             0),
        new AchievementDef("streak_30",  "Monatskrieger",     "Erreiche einen 30-Tage-Streak",  "streak", 30,  "🔥", "Unaufhaltsam",  0xFFFF1744),
        new AchievementDef("streak_60",  "Zwei Monate Feuer", "Erreiche einen 60-Tage-Streak",  "streak", 60,  "🔥", null,             0),
        new AchievementDef("streak_100", "100 Tage Legende",  "Erreiche einen 100-Tage-Streak", "streak", 100, "🔥", null,             0),

        // --- Bonus Quests ---
        new AchievementDef("bonus_5",   "Bonus-Sammler",      "Sammle 5x Bonus XP",   "bonus", 5,   "⭐", null, 0),
        new AchievementDef("bonus_15",  "Bonus-Jäger",        "Sammle 15x Bonus XP",  "bonus", 15,  "⭐", null, 0),
        new AchievementDef("bonus_30",  "Bonus-Meister",      "Sammle 30x Bonus XP",  "bonus", 30,  "⭐", null, 0),
        new AchievementDef("bonus_60",  "Bonus-Champion",     "Sammle 60x Bonus XP",  "bonus", 60,  "⭐", null, 0),
        new AchievementDef("bonus_100", "Bonus-Legende",      "Sammle 100x Bonus XP", "bonus", 100, "⭐", null, 0),

        // --- Level ---
        new AchievementDef("level_5",   "Level 5",            "Erreiche Level 5",   "level", 5,   "📈", null, 0),
        new AchievementDef("level_10",  "Level 10",           "Erreiche Level 10",  "level", 10,  "📈", null, 0),
        new AchievementDef("level_25",  "Level 25",           "Erreiche Level 25",  "level", 25,  "📈", null, 0),
        new AchievementDef("level_50",  "Level 50",           "Erreiche Level 50",  "level", 50,  "📈", null, 0),
        new AchievementDef("level_100", "Level 100",          "Erreiche Level 100", "level", 100, "📈", null, 0),

        // --- Daily Push-ups ---
        new AchievementDef("daily_30",  "Tagesrekord 30",     "Mache 30 Liegestütze an einem Tag",  "daily_pushups", 30,  "📅", null, 0),
        new AchievementDef("daily_50",  "Tagesrekord 50",     "Mache 50 Liegestütze an einem Tag",  "daily_pushups", 50,  "📅", null, 0),
        new AchievementDef("daily_100", "Tagesrekord 100",    "Mache 100 Liegestütze an einem Tag", "daily_pushups", 100, "📅", null, 0),
        new AchievementDef("daily_200", "Tagesrekord 200",    "Mache 200 Liegestütze an einem Tag", "daily_pushups", 200, "📅", null, 0),
        new AchievementDef("daily_500", "Tagesrekord 500",    "Mache 500 Liegestütze an einem Tag", "daily_pushups", 500, "📅", null, 0),

        // --- Weekly Push-ups ---
        new AchievementDef("weekly_100",  "Wochenstart",        "Mache 100 Liegestütze in einer Woche",   "weekly_pushups", 100,  "📊", null,              0),
        new AchievementDef("weekly_250",  "Wochenpower",        "Mache 250 Liegestütze in einer Woche",   "weekly_pushups", 250,  "📊", "Wochenkrieger",  0xFF009688),
        new AchievementDef("weekly_500",  "Wochenchampion",     "Mache 500 Liegestütze in einer Woche",   "weekly_pushups", 500,  "📊", null,              0),
        new AchievementDef("weekly_1000", "Wochenlegende",      "Mache 1.000 Liegestütze in einer Woche", "weekly_pushups", 1000, "📊", null,              0),
        new AchievementDef("weekly_2000", "Wochen-Titan",       "Mache 2.000 Liegestütze in einer Woche", "weekly_pushups", 2000, "📊", null,              0),

        // --- Monthly Push-ups ---
        new AchievementDef("monthly_500",   "Monatsanfänger",     "Mache 500 Liegestütze in einem Monat",    "monthly_pushups", 500,   "📆", null,              0),
        new AchievementDef("monthly_1000",  "Monatskämpfer",      "Mache 1.000 Liegestütze in einem Monat",  "monthly_pushups", 1000,  "📆", "Monatsmeister",  0xFFCDDC39),
        new AchievementDef("monthly_3000",  "Monatschampion",     "Mache 3.000 Liegestütze in einem Monat",  "monthly_pushups", 3000,  "📆", null,              0),
        new AchievementDef("monthly_5000",  "Monatslegende",      "Mache 5.000 Liegestütze in einem Monat",  "monthly_pushups", 5000,  "📆", null,              0),
        new AchievementDef("monthly_10000", "Monats-Titan",       "Mache 10.000 Liegestütze in einem Monat", "monthly_pushups", 10000, "📆", null,              0),
    };

    // =====================================================================
    // Data classes
    // =====================================================================

    public static class Title {
        public final String name;
        public final int color; // ARGB

        public Title(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    public static class AchievementDef {
        public final String id;
        public final String name;
        public final String description;
        public final String category;
        public final int target;
        public final String icon;
        public final String titleReward;  // title earned (null if none)
        public final int titleColor;      // color of the title reward

        public AchievementDef(String id, String name, String description, String category,
                              int target, String icon, String titleReward, int titleColor) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.target = target;
            this.icon = icon;
            this.titleReward = titleReward;
            this.titleColor = titleColor;
        }
    }

    // =====================================================================
    // Singleton
    // =====================================================================

    private AchievementManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AchievementManager getInstance(Context context) {
        if (instance == null) {
            instance = new AchievementManager(context);
        }
        return instance;
    }

    // =====================================================================
    // Completion tracking
    // =====================================================================

    public Set<String> getCompletedIds() {
        return new HashSet<>(prefs.getStringSet(KEY_COMPLETED, new HashSet<>()));
    }

    public boolean isCompleted(String achievementId) {
        return getCompletedIds().contains(achievementId);
    }

    private void markCompleted(String achievementId) {
        Set<String> completed = getCompletedIds();
        completed.add(achievementId);
        prefs.edit().putStringSet(KEY_COMPLETED, completed).apply();
    }

    /**
     * Checks all achievements against current values and returns newly completed ones.
     * Call this after every state change.
     */
    public List<AchievementDef> checkAndComplete(MainActivity activity) {
        List<AchievementDef> newlyCompleted = new ArrayList<>();
        Set<String> completed = getCompletedIds();

        for (AchievementDef ach : ACHIEVEMENTS) {
            if (completed.contains(ach.id)) continue;
            int currentValue = getCurrentValue(ach.category, activity);
            if (currentValue >= ach.target) {
                markCompleted(ach.id);
                newlyCompleted.add(ach);
            }
        }

        return newlyCompleted;
    }

    /**
     * Returns the current value for the given category.
     */
    public int getCurrentValue(String category, MainActivity activity) {
        switch (category) {
            case "pushups":         return activity.getCounter();
            case "quests":          return activity.getTotalQuestsCompleted();
            case "streak":          return activity.getStreak();
            case "bonus":           return activity.getBonusXpCollected();
            case "level":           return activity.getLevel();
            case "daily_pushups":   return activity.getDailyPushups();
            case "weekly_pushups":  return activity.getWeeklyPushups();
            case "monthly_pushups": return activity.getMonthlyPushups();
            default:                return 0;
        }
    }

    // =====================================================================
    // Title management
    // =====================================================================

    /**
     * Returns the highest-prestige title the user has earned, or null if none.
     * Prestige is determined by the order in the ACHIEVEMENTS array (later = higher).
     */
    public String getActiveTitle() {
        Set<String> completed = getCompletedIds();
        String bestTitle = null;

        // Iterate through achievements; later ones override earlier ones (higher prestige)
        for (AchievementDef ach : ACHIEVEMENTS) {
            if (completed.contains(ach.id) && ach.titleReward != null && !ach.titleReward.isEmpty()) {
                bestTitle = ach.titleReward;
            }
        }

        return bestTitle;
    }

    /**
     * Returns the color for a given title name, or gray (#9E9E9E) if not found.
     */
    public static int getTitleColor(String titleName) {
        for (Title t : TITLES) {
            if (t.name.equals(titleName)) return t.color;
        }
        return 0xFF9E9E9E; // default gray
    }

    /**
     * Returns the progress percentage (0-100) for a given achievement.
     */
    public int getProgress(AchievementDef ach, MainActivity activity) {
        if (isCompleted(ach.id)) return 100;
        int current = getCurrentValue(ach.category, activity);
        return Math.min(100, (int) ((current * 100L) / ach.target));
    }
}
