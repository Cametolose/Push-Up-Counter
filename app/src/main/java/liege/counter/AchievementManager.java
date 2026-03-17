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

    private static final String PREFS_NAME      = "AchievementPrefs";
    private static final String KEY_COMPLETED   = "completedAchievements";
    private static final String KEY_SELECTED_TITLE = "selectedTitle";

    private static AchievementManager instance;
    private final SharedPreferences prefs;

    // =====================================================================
    // TITLES — Add new titles here. Color is ARGB int (0xAARRGGBB).
    // =====================================================================
    public static final Title[] TITLES = {
            new Title("Neuling",       0xFF9E9E9E),  // gray
            new Title("Kämpfer",       0xFF4CAF50),  // green
            new Title("Krieger",       0xFF2196F3),  // blue
            new Title("Champion",      0xFF9C27B0),  // purple
            new Title("Meister",       0xFFFF9800),  // orange
            new Title("Legende",       0xFFFFD600),  // gold
            new Title("Titan",         0xFFF44336),  // red
            new Title("Unsterblich",   0xFFE040FB),  // pink
            new Title("Questjäger",    0xFF00BCD4),  // teal
            new Title("Chef",          0xFF0D47A1),  // deep navy
            new Title("Grinder",       0xFFD84315),  // burnt orange
            new Title("Unaufhaltsam",  0xFFFF1744),  // red accent
            new Title("Mystisch",      0xFFAD1457),  // dark magenta (mythic pink)
            new Title("Pionier",       0xFF1B5E20),  // dark forest green
            new Title("Huiuiui",       0xFF00E5FF),  // electric cyan
            new Title("Striker",       0xFF039BE5),  // sky blue
            new Title("Macher",        0xFFBF360C),  // deep red-orange
            new Title("Chiller",       0xFF80D8FF),  // very light blue
            new Title("Bauarbeiter",   0xFFF57F17),  // construction amber
            new Title("Handlanger",    0xFF8D6E63),  // medium brown
            new Title("Bot",           0xFF90A4AE),  // blue-gray silver
            new Title("Noob",          0xFF69F0AE),  // mint green
            new Title("Haferbrei",     0xFFD7B896),  // oatmeal beige
            new Title("Bananenbieger", 0xFFFFEE58),  // banana yellow
            new Title("Cheater",       0xFF7F0000),  // dark maroon (dunkleres Rot)
            new Title("Rüpel",         0xFF827717),  // dirty olive
            new Title("Unfall",        0xFFDD2C00),  // accident orange-red
            new Title("Draufgänger",   0xFF7C4DFF),  // bold purple
            new Title("Gym Bro",       0xFFFF4081),  // pink accent
            new Title("Insider",       0xFFEF9A9A),  // light rose (hellrot)
            new Title("Pro",           0xFFE57373),  // medium-light red (dunkleres hellrot)
            new Title("InsiderPro",    0xFFEF5350),  // medium red (noch dunkleres hellrot)
            new Title("Cooler Typ",    0xFFFF6F00),  // amber orange
            new Title("Regenwurm",     0xFFEC407A),  // earthworm pink
            new Title("Sniper",        0xFF0277BD),  // dark steel blue (Blau)
            new Title("Monke",         0xFF795548),  // medium brown (Braun)
            new Title("Fake-Burger",   0xFFD32F2F),  // medium red (Rot)
            new Title("Angeber",       0xFFFFCCBC),  // light peach/skin (Hautfarbe)
            new Title("Muskelprotz",   0xFFFF8A65),  // deep tan/skin (Hautfarbe, darker)
            new Title("Gorilla",       0xFF78909C),  // blue-gray mix (Braun/Grau)
            new Title("Dreikäsehoch",  0xFFFFD180),  // cheese yellow (Käsegelb)
            new Title("Crook",         0xFF607D8B),  // slate gray (scheiß grau)
            new Title("Godzilla",      0xFF4E342E),  // dark brown (Braun dunkel)
            new Title("Mafia Boss",    0xFF4A148C),  // dark purple (Lila dunkel)
            new Title("Lauch",         0xFF7CB342),  // leek green (Lauch grün)
            new Title("Flex God",      0xFFB8860B),  // dark goldenrod (Gold glitzernd)
            new Title("Gigachad",      0xFF424242),  // dark gray (Grau dunkel)
            new Title("Discord Kitten",0xFFFF80AB),  // light pink (hell)
            new Title("Pushups > Situps",0xFF6CF000), // light green
            new Title("Situps > Pushups",0xFFFF0A1F), // dark red
            new Title("Nyakuza",0xFFA937DA), // Purple
            new Title("Demon",  0xFF400301), // Dark Red)

            new Title("The Gambler",   0xFFF50057),  // deep pink — rare 1% from lucky wheel
    };

    // =====================================================================
    // ACHIEVEMENTS — Add new achievements here. Easy to expand!
    // =====================================================================
    public static final AchievementDef[] ACHIEVEMENTS = {
            // --- Total Push-ups ---
            // Prestige titles scale with volume. The main title progression lives here.
            new AchievementDef("pushups_100",   "Erste Schritte",      "Mache 100 Push-Ups insgesamt",    "pushups", 100,   "💪", "Neuling",       0xFF9E9E9E),
            new AchievementDef("pushups_500",   "Halb Tausend",        "Mache 500 Push-Ups insgesamt",    "pushups", 500,   "💪", "Kämpfer",       0xFF4CAF50),
            new AchievementDef("pushups_1000",  "Tausender-Club",      "Mache 1.000 Push-Ups insgesamt",  "pushups", 1000,  "💪", "Krieger",       0xFF2196F3),
            new AchievementDef("pushups_3000",  "Drei Tausend stark",  "Mache 3.000 Push-Ups insgesamt",  "pushups", 3000,  "💪", "Champion",      0xFF9C27B0),
            new AchievementDef("pushups_5000",  "Fünf Tausend Power",  "Mache 5.000 Push-Ups insgesamt",  "pushups", 5000,  "💪", "Meister",       0xFFFF9800),
            new AchievementDef("pushups_10000", "Zehntausend Legende", "Mache 10.000 Push-Ups insgesamt", "pushups", 10000, "💪", "Legende",       0xFFFFD600),
            new AchievementDef("pushups_25000", "Unaufhaltsam",        "Mache 25.000 Push-Ups insgesamt", "pushups", 25000, "💪", "Titan",         0xFFF44336),
            new AchievementDef("pushups_50000", "Unsterbliche Kraft",  "Mache 50.000 Push-Ups insgesamt", "pushups", 50000, "💪", "Unsterblich",   0xFFE040FB),
            new AchievementDef("pushups_100000","Krank",               "Mache 100.000 Push-Ups insgesamt","pushups", 100000,"💪", "Gigachad",      0xFF424242),

            // --- Quests Completed ---
            // Quest-based titles reward dedication & long-term grind.
            new AchievementDef("quests_10",   "Quest-Anfänger",    "Schließe 10 Quests ab",   "quests", 10,  "📋", "Questjäger",  0xFF00BCD4),
            new AchievementDef("quests_30",   "Quest-Jäger",       "Schließe 30 Quests ab",   "quests", 30,  "📋", "Macher",      0xFFBF360C),
            new AchievementDef("quests_90",   "Quest-Veteran",     "Schließe 90 Quests ab",   "quests", 90,  "📋", "Grinder",     0xFFD84315),
            new AchievementDef("quests_180",  "Quest-Champion",    "Schließe 180 Quests ab",  "quests", 180, "📋", "Situps > Pushups",        0xFFFF0A1F ),// "Huiuiui" 0xFF00E5FF
            new AchievementDef("quests_365",  "Quest-Meister",     "Schließe 365 Quests ab",  "quests", 365, "📋", "Pushups > Situps",       0xFF6CF000 ),
            new AchievementDef("quests_666",  "Quest-Legende",     "Schließe 666 Quests ab",  "quests", 666, "📋", "Demon",  0xFF400301),
            new AchievementDef("quests_1000", "Quest-Grinder",     "Schließe 1000 Quests ab", "quests", 1000,"📋", "Flex God",    0xFFB8860B),

            // --- Streak ---
            // Consistency is rewarded with increasingly powerful titles.
            new AchievementDef("streak_3",   "Drei Tage stark",   "Erreiche einen 3-Tage-Streak",   "streak", 3,   "🔥", "Pionier",      0xFF1B5E20),
            new AchievementDef("streak_7",   "Eine Woche durch",  "Erreiche einen 7-Tage-Streak",   "streak", 7,   "🔥", "Draufgänger",  0xFF7C4DFF),
            new AchievementDef("streak_14",  "Zwei Wochen Feuer", "Erreiche einen 14-Tage-Streak",  "streak", 14,  "🔥", "Striker",      0xFF039BE5),
            new AchievementDef("streak_30",  "Monatskrieger",     "Erreiche einen 30-Tage-Streak",  "streak", 30,  "🔥", "Nyakuza", 0xFFA937DA),
            new AchievementDef("streak_60",  "Zwei Monate Feuer", "Erreiche einen 60-Tage-Streak",  "streak", 60,  "🔥", "Gorilla",      0xFF78909C),
            new AchievementDef("streak_90",  "Drei Monate Meta",  "Erreiche einen 90-Tage-Streak",  "streak", 90,  "🔥", "Sniper",     0xFF0277BD),
            new AchievementDef("streak_180", "Halbe Legende",     "Erreiche einen 180-Tage-Streak", "streak", 180, "🔥", "Mystisch",     0xFFAD1457),
            new AchievementDef("streak_365", "Volle Legende",     "Erreiche einen 365-Tage-Streak", "streak", 365, "🔥", "Godzilla",     0xFF4E342E  ),

            // --- Bonus / Gamble ---
            // Lucky wheel usage. Titles get progressively shadier.
            new AchievementDef("bonus_5",   "Bonus-Sammler",      "Gamble 5x mal",   "bonus", 5,   "⭐", "Noob",   0xFF69F0AE ),
            new AchievementDef("bonus_15",  "Bonus-Jäger",        "Gamble 15x mal",  "bonus", 15,  "⭐", "Chiller",    0xFF80D8FF),
            new AchievementDef("bonus_30",  "Bonus-Meister",      "Gamble 30x mal",  "bonus", 30,  "⭐", "Cooler Typ", 0xFFFF6F00),
            new AchievementDef("bonus_60",  "Bonus-Champion",     "Gamble 60x mal",  "bonus", 60,  "⭐", "Pro",    0xFFE57373),
            new AchievementDef("bonus_100", "Bonus-Legende",      "Gamble 100x mal", "bonus", 100, "⭐", "Insider", 0xFFEF9A9A),
            new AchievementDef("bonus_300", "Bonus-Gambler",      "Gamble 300x mal", "bonus", 300, "⭐", "InsiderPro",     0xFFEF5350 ),

            // --- Level ---
            // Level milestones. Starts humbling, ends flexing.
            new AchievementDef("level_5",   "Level 5",            "Erreiche Level 5",   "level", 5,   "📈", "Crook",     0xFF607D8B  ),
            new AchievementDef("level_10",  "Level 10",           "Erreiche Level 10",  "level", 10,  "📈", "Handlanger", 0xFF8D6E63),
            new AchievementDef("level_25",  "Level 25",           "Erreiche Level 25",  "level", 25,  "📈", "Bauarbeiter",0xFFF57F17),
            new AchievementDef("level_50",  "Level 50",           "Erreiche Level 50",  "level", 50,  "📈", "Rüpel",   0xFF827717 ),
            new AchievementDef("level_100", "Level 100",          "Erreiche Level 100", "level", 100, "📈", "Mafia Boss", 0xFF4A148C),

            // --- Daily Push-ups ---
            // Starts with mockery, earns respect as numbers get absurd.
            new AchievementDef("daily_30",   "Tagesrekord 30",    "Mache 30 Push-Ups an einem Tag",    "daily_pushups", 30,   "📅", "Lauch",    0xFF7CB342),
            new AchievementDef("daily_50",   "Tagesrekord 50",    "Mache 50 Push-Ups an einem Tag",    "daily_pushups", 50,   "📅", "Bot",      0xFF90A4AE),
            new AchievementDef("daily_100",  "Tagesrekord 100",   "Mache 100 Push-Ups an einem Tag",   "daily_pushups", 100,  "📅", "Gym Bro",  0xFFFF4081),
            new AchievementDef("daily_200",  "Tagesrekord 200",   "Mache 200 Push-Ups an einem Tag",   "daily_pushups", 200,  "📅", "Monke",    0xFF795548),
            new AchievementDef("daily_500",  "Tagesrekord 500",   "Mache 500 Push-Ups an einem Tag",   "daily_pushups", 500,  "📅", "Angeber",  0xFFFFCCBC),
            new AchievementDef("daily_1000", "Tagesrekord 1000",  "Mache 1000 Push-Ups an einem Tag",  "daily_pushups", 1000, "📅", "Cheater",  0xFF7F0000),
            // --- Weekly Push-ups ---
            // Funny/quirky titles early; blank for harder milestones.
            new AchievementDef("weekly_100",  "Wochenstart",        "Mache 100 Push-Ups in einer Woche",   "weekly_pushups", 100,  "📊", "Bananenbieger", 0xFFFFEE58),
            new AchievementDef("weekly_250",  "Wochenpower",        "Mache 250 Push-Ups in einer Woche",   "weekly_pushups", 250,  "📊", "Dreikäsehoch",  0xFFFFD180),
            new AchievementDef("weekly_500",  "Wochenchampion",     "Mache 500 Push-Ups in einer Woche",   "weekly_pushups", 500,  "📊", "Haferbrei",     0xFFD7B896),
            new AchievementDef("weekly_1000", "Wochenlegende",      "Mache 1.000 Push-Ups in einer Woche", "weekly_pushups", 1000, "📊", "Fake-Burger",   0xFFD32F2F),
            new AchievementDef("weekly_3000", "Wochen-Titan",       "Mache 3.000 Push-Ups in einer Woche", "weekly_pushups", 3000, "📊", "Chef",  0xFF0D47A1),

            // --- Monthly Push-ups ---
            // Monthly grind; big titles for big numbers.
            new AchievementDef("monthly_500",   "Monatsanfänger",     "Mache 500 Push-Ups in einem Monat",    "monthly_pushups", 500,   "📆", "Unfall",         0xFFDD2C00),
            new AchievementDef("monthly_1000",  "Monatskämpfer",      "Mache 1.000 Push-Ups in einem Monat",  "monthly_pushups", 1000,  "📆", "Regenwurm",      0xFFEC407A),
            new AchievementDef("monthly_3000",  "Monatschampion",     "Mache 3.000 Push-Ups in einem Monat",  "monthly_pushups", 3000,  "📆", "Unaufhaltsam",   0xFFFF1744),
            new AchievementDef("monthly_5000",  "Monatslegende",      "Mache 5.000 Push-Ups in einem Monat",  "monthly_pushups", 5000,  "📆", "Muskelprotz", 0xFFFF8A65),
            new AchievementDef("monthly_10000", "Monats-Titan",       "Mache 10.000 Push-Ups in einem Monat", "monthly_pushups", 10000, "📆", "Discord Kitten", 0xFFFF80AB),
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
     * Saves the user's manually selected title to prefs.
     * Pass null to clear the selection (revert to auto).
     */
    public void setSelectedTitle(String title) {
        if (title == null) {
            prefs.edit().remove(KEY_SELECTED_TITLE).apply();
        } else {
            prefs.edit().putString(KEY_SELECTED_TITLE, title).apply();
        }
    }

    /**
     * Returns the active title. If the user has manually selected a title and it is earned,
     * returns that title. Otherwise falls back to the highest-prestige earned title.
     */
    public String getActiveTitle() {
        Set<String> completed = getCompletedIds();

        // Check for manually selected title
        String selected = prefs.getString(KEY_SELECTED_TITLE, null);
        if (selected != null && !selected.isEmpty()) {
            // Check if the title is from achievements
            for (AchievementDef ach : ACHIEVEMENTS) {
                if (completed.contains(ach.id) && selected.equals(ach.titleReward)) {
                    return selected;
                }
            }
            // Check if it's "The Gambler" from the lucky wheel
            if ("The Gambler".equals(selected) && prefs.getBoolean("gambler_title_earned", false)) {
                return selected;
            }
        }

        // Fall back to highest-prestige earned title
        String bestTitle = null;
        for (AchievementDef ach : ACHIEVEMENTS) {
            if (completed.contains(ach.id) && ach.titleReward != null && !ach.titleReward.isEmpty()) {
                bestTitle = ach.titleReward;
            }
        }

        // Also check for "The Gambler" title from the lucky wheel
        if (bestTitle == null) {
            boolean gamblerEarned = prefs.getBoolean("gambler_title_earned", false);
            if (gamblerEarned) {
                bestTitle = "The Gambler";
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

    /**
     * Marks "The Gambler" title as earned (from lucky wheel, not from achievements).
     */
    public void earnGamblerTitle() {
        prefs.edit().putBoolean("gambler_title_earned", true).apply();
    }

    /**
     * Checks if "The Gambler" title has been earned from the lucky wheel.
     */
    public boolean isGamblerTitleEarned() {
        return prefs.getBoolean("gambler_title_earned", false);
    }
}