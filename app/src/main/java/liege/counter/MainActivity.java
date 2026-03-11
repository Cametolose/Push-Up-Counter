package liege.counter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import okhttp3.OkHttpClient;

/**
 * MainActivity — Push-up counter with XP/leveling system, daily quests, and an online leaderboard.
 * Uses a BottomNavigationView with Fragments for each section.
 *
 * Language note: UI strings shown to the user are in German; code identifiers are in English.
 */
public class MainActivity extends AppCompatActivity {

    // --- SharedPreferences Keys ---
    private static final String PREFS_NAME       = "AppPrefs";
    private static final String KEY_COUNTER      = "counter";
    private static final String KEY_XP           = "xp";
    private static final String KEY_LEVEL        = "level";
    private static final String KEY_QUESTS       = "questsCompleted";
    private static final String KEY_QUEST_COUNTS = "questCompletions";
    private static final String KEY_BONUS        = "bonusCollected";
    private static final String KEY_BONUS_COUNT  = "bonusXpCollected";
    private static final String KEY_LAST_DAY     = "lastKnownDay";
    private static final String KEY_USERNAME     = "username";
    private static final String KEY_DAILY_LOG    = "dailyPushupLog";

    // Quest targets (shared with QuestsFragment)
    public static final int[] QUEST_TARGETS = {20, 50, 100};

    private static final int    JOB_ID                     = 1;
    private static final String ACTION_UPDATE_LEADERBOARD  = "UPDATE_LEADERBOARD";

    // --- App State ---
    private int       counter;
    private int       xp;
    private int       level;
    private boolean[] questsCompleted  = new boolean[3];
    private int[]     questCompletions = new int[3];
    private boolean   bonusCollected;
    private int       bonusXpCollected;

    private final HashMap<String, Integer> dailyPushupLog = new HashMap<>();

    // --- Networking ---
    private SharedPreferences sharedPreferences;
    private LeaderboardAPI leaderboardAPI;

    // --- Fragment state listeners ---
    private final CopyOnWriteArrayList<OnStateChangedListener> stateListeners =
            new CopyOnWriteArrayList<>();

    /** Implement in Fragments to receive state-change notifications. */
    public interface OnStateChangedListener {
        void onStateChanged();
    }

    public void addStateChangedListener(OnStateChangedListener l)    { stateListeners.add(l); }
    public void removeStateChangedListener(OnStateChangedListener l) { stateListeners.remove(l); }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupStatusBar();
        SupabaseConfig.validateConfig();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initRetrofit();
        loadState();
        resetQuestsIfNewDay();

        setupBottomNavigation();

        scheduleLeaderboardJob();
        registerLeaderboardReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(leaderboardReceiver);
    }

    // =========================================================================
    // Initialization
    // =========================================================================

    private void setupStatusBar() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.SUPABASE_URL)
                .client(buildSupabaseClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        leaderboardAPI = retrofit.create(LeaderboardAPI.class);
    }

    /**
     * Builds an OkHttpClient that injects the required Supabase auth headers
     * on every request. Shared by MainActivity, LeaderboardFragment and
     * LeaderboardUpdateService.
     */
    public static OkHttpClient buildSupabaseClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request request = chain.request().newBuilder()
                            .header("apikey",        SupabaseConfig.SUPABASE_ANON_KEY)
                            .header("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                            .header("Content-Type",  "application/json")
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {
            loadFragment(new HomeFragment(), false);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment(), false);
            } else if (id == R.id.nav_quests) {
                loadFragment(new QuestsFragment(), false);
            } else if (id == R.id.nav_leaderboard) {
                loadFragment(new LeaderboardFragment(), false);
            } else if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment(), false);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction tx =
                getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }

    // =========================================================================
    // State — Load & Save
    // =========================================================================

    private void loadState() {
        counter          = sharedPreferences.getInt(KEY_COUNTER, 0);
        xp               = sharedPreferences.getInt(KEY_XP, 0);
        level            = sharedPreferences.getInt(KEY_LEVEL, 1);
        bonusCollected   = sharedPreferences.getBoolean(KEY_BONUS, false);
        bonusXpCollected = sharedPreferences.getInt(KEY_BONUS_COUNT, 0);

        questsCompleted  = decodeBooleanArray(sharedPreferences.getString(KEY_QUESTS, "000"));
        questCompletions = decodeIntArray(sharedPreferences.getString(KEY_QUEST_COUNTS, "0,0,0"));

        String logJson = sharedPreferences.getString(KEY_DAILY_LOG, "{}");
        HashMap<String, Integer> savedLog = new Gson().fromJson(
                logJson, new TypeToken<HashMap<String, Integer>>() {}.getType());
        if (savedLog != null) {
            dailyPushupLog.putAll(savedLog);
        }
    }

    private void saveState() {
        sharedPreferences.edit()
                .putInt(KEY_COUNTER, counter)
                .putInt(KEY_XP, xp)
                .putInt(KEY_LEVEL, level)
                .putBoolean(KEY_BONUS, bonusCollected)
                .putInt(KEY_BONUS_COUNT, bonusXpCollected)
                .putString(KEY_QUESTS, encodeBooleanArray(questsCompleted))
                .putString(KEY_QUEST_COUNTS, encodeIntArray(questCompletions))
                .putInt(KEY_LAST_DAY, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
                .putString(KEY_USERNAME, getUsername())
                .putString(KEY_DAILY_LOG, new Gson().toJson(dailyPushupLog))
                .apply();
    }

    // =========================================================================
    // Daily Quest Reset
    // =========================================================================

    private void resetQuestsIfNewDay() {
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int lastDay    = sharedPreferences.getInt(KEY_LAST_DAY, -1);
        if (currentDay != lastDay) {
            questsCompleted = new boolean[3];
            bonusCollected  = false;
            saveState();
        }
    }

    // =========================================================================
    // Counter & XP Logic
    // =========================================================================

    /** Increments counter and returns XP gained (equals amount added). */
    public int incrementCounter(int amount) {
        counter += amount;
        xp      += amount;
        logDailyPushups(amount);
        checkQuestProgress();
        checkLevelUp();
        saveState();
        notifyListeners();
        updateOnlineLeaderboard();
        return amount;
    }

    /** Decrements counter (undo). Minimum 0. */
    public void decrementCounter(int amount) {
        counter = Math.max(0, counter - amount);
        xp      = Math.max(0, xp - amount);
        int today = dailyPushupLog.getOrDefault(todayKey(), 0);
        dailyPushupLog.put(todayKey(), Math.max(0, today - amount));
        saveState();
        notifyListeners();
    }

    public void completeQuest(int index) {
        if (questsCompleted[index]) return;
        questsCompleted[index] = true;
        questCompletions[index]++;
        xp += 20;
        checkLevelUp();
        saveState();
        notifyListeners();
        Toast.makeText(this, "Quest abgeschlossen! +20 XP", Toast.LENGTH_SHORT).show();
    }

    public void collectBonus() {
        for (boolean done : questsCompleted) {
            if (!done) {
                Toast.makeText(this, "Schließe zuerst alle Quests ab!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (bonusCollected) return;

        xp += 50;
        bonusXpCollected++;
        bonusCollected = true;
        checkLevelUp();
        saveState();
        notifyListeners();
        Toast.makeText(this, "+50 Bonus XP gesammelt!", Toast.LENGTH_SHORT).show();
    }

    private void checkQuestProgress() {
        int daily = getDailyPushups();
        for (int i = 0; i < 3; i++) {
            if (!questsCompleted[i] && daily >= QUEST_TARGETS[i]) {
                completeQuest(i);
            }
        }
    }

    private void checkLevelUp() {
        int xpNeeded = xpForNextLevel();
        while (xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;
            xpNeeded = xpForNextLevel();
            Toast.makeText(this, "Level Up! Du bist jetzt Level " + level,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // XP Calculations
    // =========================================================================

    public int xpForNextLevel() {
        return 50 * level;
    }

    public int totalXpAcrossLevels() {
        // Total XP earned across all previous levels (level 1 through level-1).
        // Each level i required 50*i XP, so sum = 50*(1+2+...+(level-1)) = 50*level*(level-1)/2
        int total = 50 * level * (level - 1) / 2;
        return total + xp;
    }

    // =========================================================================
    // Daily Push-up Log
    // =========================================================================

    private void logDailyPushups(int amount) {
        String today = todayKey();
        dailyPushupLog.put(today, dailyPushupLog.getOrDefault(today, 0) + amount);
    }

    public int getDailyPushups() {
        return dailyPushupLog.getOrDefault(todayKey(), 0);
    }

    public int getWeeklyPushups() {
        int total = 0;
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            total += dailyPushupLog.getOrDefault(keyFor(cal), 0);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return total;
    }

    public int getLogTotal() {
        int total = 0;
        for (int v : dailyPushupLog.values()) total += v;
        return total;
    }

    /**
     * Returns the current consecutive-day streak.
     * Counts how many consecutive days (going back from today or yesterday)
     * had at least 10 push-ups.
     */
    public int getStreak() {
        Calendar cal = Calendar.getInstance();
        // If today has fewer than 10 pushups, start counting from yesterday
        // (keeps the streak visible until midnight even if today's session hasn't started)
        if (dailyPushupLog.getOrDefault(keyFor(cal), 0) < 10) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        int streak = 0;
        for (int i = 0; i < 365; i++) {
            if (dailyPushupLog.getOrDefault(keyFor(cal), 0) >= 10) {
                streak++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    private String todayKey() {
        return keyFor(Calendar.getInstance());
    }

    private String keyFor(Calendar cal) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    // =========================================================================
    // Public Getters
    // =========================================================================

    public int     getCounter()             { return counter; }
    public int     getXp()                  { return xp; }
    public int     getLevel()               { return level; }
    public int     getXpForNextLevel()      { return xpForNextLevel(); }
    public int     getTotalXpAcrossLevels() { return totalXpAcrossLevels(); }
    public boolean isQuestCompleted(int i)  { return questsCompleted[i]; }
    public boolean isBonusCollected()       { return bonusCollected; }
    public int     getBonusXpCollected()    { return bonusXpCollected; }
    public int     getQuestCompletionCount(int index) { return questCompletions[index]; }
    public String  getUsername()            {
        return sharedPreferences.getString(KEY_USERNAME, "Unbekannt");
    }
    public int getTotalQuestsCompleted() {
        return questCompletions[0] + questCompletions[1] + questCompletions[2];
    }

    public void setUsername(String name) {
        sharedPreferences.edit().putString(KEY_USERNAME, name).apply();
    }

    // =========================================================================
    // Listener notification
    // =========================================================================

    private void notifyListeners() {
        for (OnStateChangedListener l : stateListeners) {
            l.onStateChanged();
        }
    }

    // =========================================================================
    // Encoding Helpers
    // =========================================================================

    private String encodeBooleanArray(boolean[] arr) {
        StringBuilder sb = new StringBuilder();
        for (boolean b : arr) sb.append(b ? '1' : '0');
        return sb.toString();
    }

    private boolean[] decodeBooleanArray(String s) {
        boolean[] arr = new boolean[s.length()];
        for (int i = 0; i < s.length(); i++) arr[i] = s.charAt(i) == '1';
        return arr;
    }

    private String encodeIntArray(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private int[] decodeIntArray(String s) {
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { arr[i] = Integer.parseInt(parts[i].trim()); }
            catch (NumberFormatException ignored) { arr[i] = 0; }
        }
        return arr;
    }

    // =========================================================================
    // Networking — Leaderboard
    // =========================================================================

    /** Pushes the current user's stats to the online leaderboard. */
    public void updateOnlineLeaderboard() {
        String username = getUsername();
        if (username.equals("Unbekannt") || username.trim().isEmpty()) {
            Log.w("Leaderboard", "Kein Benutzername gesetzt — Leaderboard-Upload übersprungen");
            return;
        }
        String playerId = username.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");

        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setName(username);
        entry.setPushups(counter);
        entry.setLevel(level);
        entry.setCurrentXp(xp);
        entry.setTotalXp(totalXpAcrossLevels());
        entry.setXpForNextLevel(xpForNextLevel());
        entry.setTotalQuestsCompleted(getTotalQuestsCompleted());
        entry.setQuest1Completions(questCompletions[0]);
        entry.setQuest2Completions(questCompletions[1]);
        entry.setQuest3Completions(questCompletions[2]);
        entry.setBonusXpCollected(bonusXpCollected);

        int daily  = getDailyPushups();
        int weekly = getWeeklyPushups();
        int allTime = getLogTotal();
        entry.setDailyPushups(daily);
        entry.setWeeklyPushups(weekly);
        entry.setMonthlyPushups(allTime);
        entry.setYearlyPushups(allTime);
        entry.setAvgPushupsPerDay(daily);
        entry.setAvgPushupsPerWeek((double) weekly / 7);
        entry.setAvgPushupsPerMonth((double) allTime / 30);
        entry.setStreak(getStreak());

        entry.setId(playerId);
        leaderboardAPI.upsertEntry(entry).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("Leaderboard", "Erfolgreich aktualisiert für: " + username);
                } else {
                    try {
                        String errorBody = response.errorBody() != null
                                ? response.errorBody().string() : "kein Body";
                        Log.e("Leaderboard", "HTTP " + response.code() + " — " + errorBody);
                    } catch (Exception e) {
                        Log.e("Leaderboard", "HTTP " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Leaderboard", "Fehler beim Senden der Leaderboard-Daten", t);
            }
        });
    }

    // =========================================================================
    // BroadcastReceiver
    // =========================================================================

    private void registerLeaderboardReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_UPDATE_LEADERBOARD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(leaderboardReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(leaderboardReceiver, filter);
        }
    }

    private final BroadcastReceiver leaderboardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Leaderboard updates are handled by LeaderboardFragment's own receiver
            Log.d("MainActivity", "Leaderboard broadcast received");
        }
    };

    // =========================================================================
    // JobScheduler
    // =========================================================================

    private void scheduleLeaderboardJob() {
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        if (scheduler.getPendingJob(JOB_ID) != null) return;

        ComponentName service = new ComponentName(this, LeaderboardUpdateService.class);
        JobInfo job = new JobInfo.Builder(JOB_ID, service)
                .setPeriodic(15 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();

        int result = scheduler.schedule(job);
        if (result != JobScheduler.RESULT_SUCCESS) {
            Log.w("Leaderboard", "Automatische Aktualisierung konnte nicht geplant werden: "
                    + result);
        }
    }

    /**
     * LeaderboardUpdateService — background JobService that fetches the leaderboard
     * and broadcasts the result.
     */
    public static class LeaderboardUpdateService extends JobService {

        private static final String TAG = "LeaderboardService";
        private LeaderboardAPI leaderboardAPI;

        @Override
        public void onCreate() {
            super.onCreate();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.SUPABASE_URL)
                    .client(MainActivity.buildSupabaseClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            leaderboardAPI = retrofit.create(LeaderboardAPI.class);
        }

        @Override
        public boolean onStartJob(JobParameters params) {
            Log.d(TAG, "Leaderboard-Aktualisierungsauftrag gestartet");
            fetchAndBroadcast(params);
            return true;
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            Log.w(TAG, "Leaderboard-Aktualisierungsauftrag gestoppt");
            return false;
        }

        private void fetchAndBroadcast(JobParameters params) {
            leaderboardAPI.getLeaderboard().enqueue(new Callback<List<LeaderboardEntry>>() {
                @Override
                public void onResponse(Call<List<LeaderboardEntry>> call,
                                       Response<List<LeaderboardEntry>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ArrayList<String> rows = new ArrayList<>();
                        int rank = 1;
                        for (LeaderboardEntry e : response.body()) {
                            rows.add("#" + rank + "  " + e.getName()
                                    + "   Liegestützen: " + e.getPushups()
                                    + "   Level: " + e.getLevel()
                                    + "   🔥" + e.getStreak() + " Tage");
                            rank++;
                        }
                        Intent intent = new Intent("UPDATE_LEADERBOARD");
                        intent.putStringArrayListExtra("leaderboardData", rows);
                        sendBroadcast(intent);
                        Log.d(TAG, "Leaderboard-Broadcast gesendet");
                    } else {
                        Log.e(TAG, "Leaderboard-Antwort fehlerhaft: " + response.code());
                    }
                    jobFinished(params, false);
                }

                @Override
                public void onFailure(Call<List<LeaderboardEntry>> call, Throwable t) {
                    Log.e(TAG, "Fehler beim Abrufen des Leaderboards", t);
                    jobFinished(params, false);
                }
            });
        }
    }
}
