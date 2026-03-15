package liege.counter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * ItemManager — manages the item/loot system including inventory, active effects,
 * lucky wheel items, and trap/buff tracking.
 *
 * Item Types:
 *   XP        — instant XP gain/loss
 *   BUFF      — timed positive effect on self
 *   TRAP      — sent to random other player
 *   AUTOMATIC — stored in inventory, used automatically when triggered
 */
public class ItemManager {

    private static final String TAG = "ItemManager";
    private static final String PREFS_NAME = "ItemPrefs";

    // Inventory keys (automatic items)
    private static final String KEY_NEGATE_TRAP_COUNT = "negate_trap_count";
    private static final String KEY_STREAK_SAVE_COUNT = "streak_save_count";

    // Active buff/trap keys
    private static final String KEY_DOUBLE_XP_UNTIL = "double_xp_until";
    private static final String KEY_HALF_XP_UNTIL = "half_xp_until";
    private static final String KEY_HALF_XP_FROM = "half_xp_from";       // who sent the half XP trap

    // Gambler title tracking
    private static final String KEY_GAMBLER_EARNED = "gambler_title_earned";

    // Streak save tracking
    private static final String KEY_STREAK_SAVED_DAY = "streak_saved_day";

    private static ItemManager instance;
    private final SharedPreferences prefs;
    private final Random random = new Random();

    // =====================================================================
    // Item definitions for the Lucky Wheel
    // =====================================================================

    public enum ItemType { XP, BUFF, TRAP, AUTOMATIC, TITLE }

    public static class WheelItem {
        public final String id;
        public final String name;
        public final String description;
        public final String emoji;
        public final ItemType type;
        public final int color;      // Wheel segment color (ARGB)
        public final double weight;  // Probability weight

        public WheelItem(String id, String name, String description, String emoji,
                         ItemType type, int color, double weight) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.emoji = emoji;
            this.type = type;
            this.color = color;
            this.weight = weight;
        }
    }

    /**
     * All items on the lucky wheel. Weights are relative.
     * Each regular item has weight 99 (equal chance: ~12.375% each).
     * "The Gambler" has weight 8, giving exactly 1% chance (8/800 = 1%).
     */
    public static final WheelItem[] WHEEL_ITEMS = {
        new WheelItem("xp_50",       "+50 XP",           "Sofort +50 XP!",
                "✨", ItemType.XP,        0xFF4CAF50, 99),  // green — 12.375%
        new WheelItem("xp_100",      "+100 XP",          "Sofort +100 XP!",
                "💎", ItemType.XP,        0xFF2196F3, 99),  // blue — 12.375%
        new WheelItem("double_xp",   "Doppel-XP",        "Doppelte XP für 24 Stunden!",
                "🚀", ItemType.BUFF,      0xFFFF9800, 99),  // orange — 12.375%
        new WheelItem("half_xp",     "Halbe XP",         "Halbe XP für 24h — wird an einen zufälligen Spieler gesendet!",
                "🐌", ItemType.TRAP,      0xFFF44336, 99),  // red — 12.375%
        new WheelItem("minus_50",    "-50 XP",           "-50 XP für einen zufälligen Spieler!",
                "💣", ItemType.TRAP,      0xFF9C27B0, 99),  // purple — 12.375%
        new WheelItem("minus_100",   "-100 XP",          "-100 XP für einen zufälligen Spieler!",
                "💥", ItemType.TRAP,      0xFF880E4F, 99),  // dark pink — 12.375%
        new WheelItem("negate_trap", "Fallen-Schutz",    "Negiert automatisch die nächste Falle, die du erhältst!",
                "🛡️", ItemType.AUTOMATIC, 0xFF00BCD4, 99),  // teal — 12.375%
        new WheelItem("streak_save", "Streak-Rettung",   "Rettet deinen Streak automatisch, wenn du ihn verlieren würdest!",
                "❤️", ItemType.AUTOMATIC, 0xFFFFD600, 99),  // gold — 12.375%
        // "The Gambler" is special — exactly 1% chance (8/800), removed after earning
        new WheelItem("gambler",     "The Gambler",       "Seltener Titel: \"The Gambler\" freigeschaltet!",
                "🃏", ItemType.TITLE,     0xFFE040FB, 8),   // pink — 1%
    };

    // =====================================================================
    // Singleton
    // =====================================================================

    private ItemManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ItemManager getInstance(Context context) {
        if (instance == null) {
            instance = new ItemManager(context);
        }
        return instance;
    }

    // =====================================================================
    // Lucky Wheel Spin
    // =====================================================================

    /**
     * Returns the list of eligible wheel items (excludes gambler if already earned).
     */
    public List<WheelItem> getEligibleItems() {
        boolean gamblerEarned = prefs.getBoolean(KEY_GAMBLER_EARNED, false);
        List<WheelItem> eligible = new ArrayList<>();
        for (WheelItem item : WHEEL_ITEMS) {
            if ("gambler".equals(item.id) && gamblerEarned) continue;
            eligible.add(item);
        }
        return eligible;
    }

    /**
     * Spins the wheel and returns the index within the eligible items list.
     */
    public int spinWheel() {
        List<WheelItem> eligible = getEligibleItems();
        double totalWeight = 0;
        for (WheelItem item : eligible) {
            totalWeight += item.weight;
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < eligible.size(); i++) {
            cumulative += eligible.get(i).weight;
            if (roll < cumulative) {
                return i;
            }
        }
        return eligible.size() - 1; // fallback
    }

    // =====================================================================
    // Inventory — Automatic Items
    // =====================================================================

    public int getNegateTrapCount() {
        return prefs.getInt(KEY_NEGATE_TRAP_COUNT, 0);
    }

    public void addNegateTrap() {
        prefs.edit().putInt(KEY_NEGATE_TRAP_COUNT, getNegateTrapCount() + 1).apply();
    }

    public boolean useNegateTrap() {
        int count = getNegateTrapCount();
        if (count <= 0) return false;
        prefs.edit().putInt(KEY_NEGATE_TRAP_COUNT, count - 1).apply();
        return true;
    }

    public int getStreakSaveCount() {
        return prefs.getInt(KEY_STREAK_SAVE_COUNT, 0);
    }

    public void addStreakSave() {
        prefs.edit().putInt(KEY_STREAK_SAVE_COUNT, getStreakSaveCount() + 1).apply();
    }

    /**
     * Try to use a streak save for today. Returns true if one was available.
     */
    public boolean useStreakSave() {
        int count = getStreakSaveCount();
        if (count <= 0) return false;

        // Only one streak save per day
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastSavedDay = prefs.getString(KEY_STREAK_SAVED_DAY, "");
        if (today.equals(lastSavedDay)) return false;

        prefs.edit()
                .putInt(KEY_STREAK_SAVE_COUNT, count - 1)
                .putString(KEY_STREAK_SAVED_DAY, today)
                .apply();
        return true;
    }

    /**
     * Check if streak was saved today.
     */
    public boolean wasStreakSavedToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        return today.equals(prefs.getString(KEY_STREAK_SAVED_DAY, ""));
    }

    // =====================================================================
    // Active Buffs & Traps (Time-based)
    // =====================================================================

    /** Activate Double XP for 24 hours. */
    public void activateDoubleXp() {
        long until = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
        prefs.edit().putLong(KEY_DOUBLE_XP_UNTIL, until).apply();
    }

    /** Check if Double XP is currently active. */
    public boolean isDoubleXpActive() {
        long until = prefs.getLong(KEY_DOUBLE_XP_UNTIL, 0);
        return System.currentTimeMillis() < until;
    }

    /** Get remaining time for Double XP in millis. */
    public long getDoubleXpRemaining() {
        long until = prefs.getLong(KEY_DOUBLE_XP_UNTIL, 0);
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /** Activate Half XP trap for 24 hours. */
    public void activateHalfXp(String fromPlayer) {
        long until = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
        prefs.edit()
                .putLong(KEY_HALF_XP_UNTIL, until)
                .putString(KEY_HALF_XP_FROM, fromPlayer)
                .apply();
    }

    /** Check if Half XP trap is currently active. */
    public boolean isHalfXpActive() {
        long until = prefs.getLong(KEY_HALF_XP_UNTIL, 0);
        return System.currentTimeMillis() < until;
    }

    /** Get remaining time for Half XP in millis. */
    public long getHalfXpRemaining() {
        long until = prefs.getLong(KEY_HALF_XP_UNTIL, 0);
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /** Get who sent the Half XP trap. */
    public String getHalfXpFrom() {
        return prefs.getString(KEY_HALF_XP_FROM, "Unbekannt");
    }

    // =====================================================================
    // XP Multiplier
    // =====================================================================

    /**
     * Returns the XP multiplier based on active buffs/traps.
     * Double XP = 2.0, Half XP = 0.5.
     * When both are active simultaneously, they cancel out: 2.0 * 0.5 = 1.0.
     * This is intentional — the effects stack multiplicatively.
     */
    public double getXpMultiplier() {
        double multiplier = 1.0;
        if (isDoubleXpActive()) multiplier *= 2.0;
        if (isHalfXpActive()) multiplier *= 0.5;
        return multiplier;
    }

    // =====================================================================
    // "The Gambler" Title
    // =====================================================================

    public boolean isGamblerEarned() {
        return prefs.getBoolean(KEY_GAMBLER_EARNED, false);
    }

    public void earnGamblerTitle() {
        prefs.edit().putBoolean(KEY_GAMBLER_EARNED, true).apply();
    }

    // =====================================================================
    // Helper: Random target player selection
    // =====================================================================

    /**
     * Picks a random player from the leaderboard entries excluding the current player.
     * Returns null if no other players are available.
     */
    public LeaderboardEntry pickRandomTarget(List<LeaderboardEntry> entries, String myPlayerId) {
        List<LeaderboardEntry> others = new ArrayList<>();
        for (LeaderboardEntry e : entries) {
            if (e.getId() != null && !e.getId().equals(myPlayerId)) {
                others.add(e);
            }
        }
        if (others.isEmpty()) return null;
        return others.get(random.nextInt(others.size()));
    }

    // =====================================================================
    // Format helpers
    // =====================================================================

    /**
     * Formats remaining millis as "XXh XXm" string.
     */
    public static String formatRemaining(long millis) {
        if (millis <= 0) return "Abgelaufen";
        long hours = millis / (60 * 60 * 1000);
        long minutes = (millis % (60 * 60 * 1000)) / (60 * 1000);
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
