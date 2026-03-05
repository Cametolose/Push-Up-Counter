package liege.counter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
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
import android.text.InputType;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * MainActivity — Push-up counter with XP/leveling system, daily quests, and an online leaderboard.
 *
 * Language note: UI strings shown to the user are in German; code identifiers are in English.
 */
public class MainActivity extends AppCompatActivity {

    // --- SharedPreferences Keys ---
    private static final String PREFS_NAME        = "AppPrefs";
    private static final String KEY_COUNTER       = "counter";
    private static final String KEY_XP            = "xp";
    private static final String KEY_LEVEL         = "level";
    private static final String KEY_QUESTS        = "questsCompleted";  // boolean[3] encoded as "000"
    private static final String KEY_QUEST_COUNTS  = "questCompletions"; // int[3] encoded as "0,0,0"
    private static final String KEY_BONUS         = "bonusCollected";
    private static final String KEY_BONUS_COUNT   = "bonusXpCollected";
    private static final String KEY_LAST_DAY      = "lastKnownDay";
    private static final String KEY_USERNAME      = "username";
    private static final String KEY_DAILY_LOG     = "dailyPushupLog";

    // JobScheduler ID — must be unique within the app
    private static final int JOB_ID = 1;

    // Leaderboard update broadcast action
    private static final String ACTION_UPDATE_LEADERBOARD = "UPDATE_LEADERBOARD";
    private static final String EXTRA_LEADERBOARD_DATA    = "leaderboardData";

    // --- App State ---
    private int counter;
    private int xp;
    private int level;
    private boolean[] questsCompleted = new boolean[3];
    private int[]     questCompletions = new int[3]; // lifetime per-quest completions
    private boolean   bonusCollected;
    private int       bonusXpCollected; // lifetime bonus count

    // Daily push-up log: day-name → count.
    // NOTE: Monthly/yearly stats are approximated from this map (see known limitations below).
    private final HashMap<String, Integer> dailyPushupLog = new HashMap<>();

    // --- UI ---
    private TextView    counterTextView;
    private TextView    levelTextView;
    private ProgressBar xpProgressBar;
    private Button[]    questButtons = new Button[3];
    private Animation   spinAnimation;

    // --- Networking ---
    private SharedPreferences sharedPreferences;
    private LeaderboardAPI    leaderboardAPI;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupStatusBar();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initRetrofit();
        initUI();

        loadState();
        resetQuestsIfNewDay();
        updateUI();

        loadOnlineLeaderboard();
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
                .baseUrl(getString(R.string.base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        leaderboardAPI = retrofit.create(LeaderboardAPI.class);
    }

    private void initUI() {
        spinAnimation   = AnimationUtils.loadAnimation(this, R.anim.spin);
        counterTextView = findViewById(R.id.counterTextView);
        levelTextView   = findViewById(R.id.levelTextView);
        xpProgressBar   = findViewById(R.id.xpProgressBar);

        // Increment buttons
        setupIncrementButton(R.id.incrementButton1,  1);
        setupIncrementButton(R.id.incrementButton5,  5);
        setupIncrementButton(R.id.incrementButton10, 10);

        // Reload leaderboard button
        ImageButton reloadButton = findViewById(R.id.reloadLeaderboardButton);
        reloadButton.setOnClickListener(v -> {
            v.startAnimation(spinAnimation);
            loadOnlineLeaderboard();
        });

        // Change name button
        Button changeNameButton = findViewById(R.id.changeNameButton);
        changeNameButton.setOnClickListener(v -> showChangeNameDialog());

        // Quest buttons
        for (int i = 0; i < 3; i++) {
            int resId = getResources().getIdentifier("questButton" + (i + 1), "id", getPackageName());
            questButtons[i] = findViewById(resId);
            final int index = i;
            questButtons[i].setOnClickListener(v -> completeQuest(index));
        }

        // Bonus button
        Button bonusButton = findViewById(R.id.bonusButton);
        bonusButton.setOnClickListener(v -> collectBonus());
    }

    private void setupIncrementButton(int buttonId, int amount) {
        Button button = findViewById(buttonId);
        button.setBackgroundResource(R.drawable.buttonshape);
        button.setOnClickListener(v -> incrementCounter(amount));
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
                .putInt(KEY_LAST_DAY, Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) // DAY_OF_YEAR avoids same-weekday-next-week bug
                .putString(KEY_USERNAME, getUsername())
                .putString(KEY_DAILY_LOG, new Gson().toJson(dailyPushupLog))
                .apply();
    }

    // =========================================================================
    // Daily Quest Reset
    // =========================================================================

    /**
     * Resets quests and bonus when a new calendar day is detected.
     * Uses DAY_OF_YEAR so "Monday this week" ≠ "Monday last week".
     */
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

    private void incrementCounter(int amount) {
        counter += amount;
        xp      += amount;
        logDailyPushups(amount); // FIX: was never called before
        checkLevelUp();
        saveState();
        updateUI();
        // Leaderboard is updated on save, not on every tap — use the reload button or the scheduled job
        // to avoid hammering the API. Call updateOnlineLeaderboard() here only if you want real-time sync.
    }

    private void completeQuest(int index) {
        if (questsCompleted[index]) return;

        questsCompleted[index] = true;
        questCompletions[index]++;
        xp += 20;
        checkLevelUp();
        saveState();
        updateUI();
    }

    private void collectBonus() {
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
        updateUI();
    }

    private void checkLevelUp() {
        int xpNeeded = xpForNextLevel();
        while (xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;
            xpNeeded = xpForNextLevel();
            Toast.makeText(this, "Level Up! Du bist jetzt Level " + level, Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // XP Calculations
    // =========================================================================

    /** XP required to advance from the current level to the next. */
    private int xpForNextLevel() {
        return (int) (10 * Math.pow(level, 1.5));
    }

    /** Total XP accumulated across all previous levels (for leaderboard display). */
    private int totalXpAcrossLevels() {
        int total = 0;
        for (int i = 1; i < level; i++) {
            total += (int) (10 * Math.pow(i, 1.5));
        }
        return total + xp;
    }

    // =========================================================================
    // Daily Push-up Log
    // =========================================================================

    private void logDailyPushups(int amount) {
        String today = todayKey();
        dailyPushupLog.put(today, dailyPushupLog.getOrDefault(today, 0) + amount);
    }

    private int getDailyPushups() {
        return dailyPushupLog.getOrDefault(todayKey(), 0);
    }

    private int getWeeklyPushups() {
        int total = 0;
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            total += dailyPushupLog.getOrDefault(keyFor(cal), 0);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return total;
    }

    /**
     * Returns total push-ups stored in the log.
     * NOTE: The log only stores one entry per day-name (e.g. "Montag"), so entries older than
     * 7 days overwrite earlier ones. For true monthly/yearly tracking, switch to date-keyed
     * entries like "2025-03-05".
     */
    private int getLogTotal() {
        int total = 0;
        for (int v : dailyPushupLog.values()) total += v;
        return total;
    }

    private String todayKey() {
        return keyFor(Calendar.getInstance());
    }

    private String keyFor(Calendar cal) {
        return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    }

    // =========================================================================
    // UI Updates
    // =========================================================================

    private void updateUI() {
        counterTextView.setText("Liegestützen: " + counter);
        levelTextView.setText("Level: " + level);

        int xpNeeded = xpForNextLevel();
        xpProgressBar.setProgress(xpNeeded > 0 ? (xp * 100) / xpNeeded : 0);

        updateQuestButtons();
    }

    private void updateQuestButtons() {
        // Quest labels are in German for the user
        String[] labels = {"Quest 1", "Quest 2", "Quest 3"};
        for (int i = 0; i < questButtons.length; i++) {
            questButtons[i].setText(labels[i]);
            questButtons[i].setEnabled(!questsCompleted[i]);
        }
    }

    // =========================================================================
    // Username Dialog
    // =========================================================================

    private void showChangeNameDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Dein Name");

        new AlertDialog.Builder(this)
                .setTitle("Neuen Namen eingeben")
                .setView(input)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        sharedPreferences.edit().putString(KEY_USERNAME, name).apply();
                        Toast.makeText(this, "Name geändert zu: " + name, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Name darf nicht leer sein!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "Unbekannt");
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

    private void loadOnlineLeaderboard() {
        leaderboardAPI.getLeaderboard().enqueue(new Callback<List<LeaderboardEntry>>() {
            @Override
            public void onResponse(Call<List<LeaderboardEntry>> call, Response<List<LeaderboardEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> rows = new ArrayList<>();
                    for (LeaderboardEntry e : response.body()) {
                        rows.add(e.getName() + " — Liegestützen: " + e.getPushups() + ", Level: " + e.getLevel());
                    }
                    updateLeaderboardUI(rows);
                } else {
                    Log.w("Leaderboard", "Leaderboard response not successful");
                }
            }

            @Override
            public void onFailure(Call<List<LeaderboardEntry>> call, Throwable t) {
                Log.e("Leaderboard", "Fehler beim Laden des Leaderboards", t);
            }
        });
    }

    private void updateLeaderboardUI(List<String> rows) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        ListView listView = findViewById(R.id.leaderboardListView);
        listView.setAdapter(adapter);
    }

    /**
     * Pushes the current user's stats to the online leaderboard.
     * Call this explicitly (e.g. on the reload button) rather than on every tap.
     */
    private void updateOnlineLeaderboard() {
        String username = getUsername();
        // Use a sanitized username as the player ID
        String playerId = username.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");

        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setName(username);
        entry.setPushups(counter);
        entry.setLevel(level);
        entry.setCurrentXp(xp);
        entry.setTotalXp(totalXpAcrossLevels());
        entry.setXpForNextLevel(xpForNextLevel());

        entry.setTotalQuestsCompleted(questCompletions[0] + questCompletions[1] + questCompletions[2]);
        entry.setQuest1Completions(questCompletions[0]);
        entry.setQuest2Completions(questCompletions[1]);
        entry.setQuest3Completions(questCompletions[2]);
        entry.setBonusXpCollected(bonusXpCollected);

        int daily   = getDailyPushups();
        int weekly  = getWeeklyPushups();
        int allTime = getLogTotal();
        entry.setDailyPushups(daily);
        entry.setWeeklyPushups(weekly);
        entry.setMonthlyPushups(allTime);  // see NOTE in getLogTotal()
        entry.setYearlyPushups(allTime);   // see NOTE in getLogTotal()
        entry.setAvgPushupsPerDay(daily);
        entry.setAvgPushupsPerWeek((double) weekly / 7);
        entry.setAvgPushupsPerMonth((double) allTime / 30);

        leaderboardAPI.updateEntry(playerId, entry).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("Leaderboard", "Leaderboard erfolgreich aktualisiert");
                } else {
                    Log.e("Leaderboard", "Fehler beim Aktualisieren: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Leaderboard", "Fehler beim Senden der Leaderboard-Daten", t);
            }
        });
    }

    // =========================================================================
    // BroadcastReceiver — receives leaderboard updates from LeaderboardUpdateService
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
            ArrayList<String> data = intent.getStringArrayListExtra(EXTRA_LEADERBOARD_DATA);
            if (data != null) updateLeaderboardUI(data);
        }
    };

    // =========================================================================
    // JobScheduler — periodic leaderboard background refresh
    // =========================================================================

    /**
     * Schedules LeaderboardUpdateService to run every 15 minutes when a network is available.
     * LeaderboardUpdateService MUST be a top-level class (not an inner class) and declared
     * in AndroidManifest.xml with android:permission="android.permission.BIND_JOB_SERVICE".
     */
    private void scheduleLeaderboardJob() {
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;

        // Avoid rescheduling if the job is already pending
        if (scheduler.getPendingJob(JOB_ID) != null) return;

        ComponentName service = new ComponentName(this, LeaderboardUpdateService.class);
        JobInfo job = new JobInfo.Builder(JOB_ID, service)
                .setPeriodic(15 * 60 * 1000L) // 15-minute minimum enforced by Android
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();

        int result = scheduler.schedule(job);
        if (result != JobScheduler.RESULT_SUCCESS) {
            Log.w("Leaderboard", "Automatische Aktualisierung konnte nicht geplant werden: " + result);
        }
    }

    /**
     * LeaderboardUpdateService — background JobService that fetches the leaderboard
     * and broadcasts the result to MainActivity via a local broadcast.
     *
     * IMPORTANT: This must be a top-level class (not an inner class of MainActivity)
     * and must be declared in AndroidManifest.xml:
     *
     *   <service
     *       android:name=".LeaderboardUpdateService"
     *       android:permission="android.permission.BIND_JOB_SERVICE"
     *       android:exported="false" />
     */
    public static class LeaderboardUpdateService extends JobService {

        private static final String TAG = "LeaderboardService";

        private LeaderboardAPI leaderboardAPI;

        @Override
        public void onCreate() {
            super.onCreate();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(AppConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            leaderboardAPI = retrofit.create(LeaderboardAPI.class);
        }

        @Override
        public boolean onStartJob(JobParameters params) {
            Log.d(TAG, "Leaderboard-Aktualisierungsauftrag gestartet");
            fetchAndBroadcast(params);
            return true; // job is still running (async)
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            Log.w(TAG, "Leaderboard-Aktualisierungsauftrag gestoppt");
            return false; // do not retry
        }

        private void fetchAndBroadcast(JobParameters params) {
            leaderboardAPI.getLeaderboard().enqueue(new Callback<List<LeaderboardEntry>>() {
                @Override
                public void onResponse(Call<List<LeaderboardEntry>> call, Response<List<LeaderboardEntry>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ArrayList<String> rows = new ArrayList<>();
                        for (LeaderboardEntry e : response.body()) {
                            rows.add(e.getName() + " — Liegestützen: " + e.getPushups() + ", Level: " + e.getLevel());
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