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
import android.os.Handler;
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

public class MainActivity extends AppCompatActivity {

    // Constants
    private final String SHARED_PREFS_NAME = "AppPrefs";
    private final String KEY_COUNTER = "counter";
    private final String KEY_XP = "xp";
    private final String KEY_LEVEL = "level";
    private final String KEY_QUESTS_COMPLETED = "questsCompleted";
    private final String KEY_BONUS_COLLECTED = "bonusCollected";
    private final String KEY_LAST_KNOWN_DAY = "lastKnownDay";
    private final String KEY_USERNAME = "username";
    private final int JOB_ID = 1;

    // Variables for the app's state
    private int counter = 0;
    private int xp = 0;
    private int level = 1;
    private int[] questCompletions = new int[3]; // Tracks how many times each quest is completed
    private int bonusXpCollected = 0; // Tracks total bonus XP collected
    private boolean[] questsCompleted = new boolean[3];
    private boolean bonusCollected = false;

    // UI Elements
    private TextView counterTextView, levelTextView, questsTextView;
    private Button[] questButtons;
    private Button bonusButton, changeNameButton;
    private ProgressBar xpProgressBar;

    // Utilities
    private SharedPreferences sharedPreferences;
    private LeaderboardAPI leaderboardAPI;
    private Animation spinAnimation; // Preloaded animation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize utilities and UI
        setupStatusBar();
        setupSharedPreferences();
        initializeRetrofit();
        initializeUI();

        // Load state and reset quests if needed
        loadState();
        resetQuestsIfNewDay();
        updateUI();

        // Fetch leaderboard and start updates
        loadOnlineLeaderboard();
        startAutomaticLeaderboardUpdates();

        // Register the BroadcastReceiver
        registerLeaderboardReceiver();
    }


    // --- Initialization Methods ---
    private void setupStatusBar() {
        // Set the status bar color to black
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void initializeRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        leaderboardAPI = retrofit.create(LeaderboardAPI.class);
    }

    private void initializeUI() {
        // Preload animation
        spinAnimation = AnimationUtils.loadAnimation(this, R.anim.spin);

        // Initialize TextViews and ProgressBar
        counterTextView = findViewById(R.id.counterTextView);
        levelTextView = findViewById(R.id.levelTextView);
        questsTextView = findViewById(R.id.questsTextView);
        xpProgressBar = findViewById(R.id.xpProgressBar);

        // Initialize Buttons
        initializeButtons();
    }

    private void initializeButtons() {
        // Increment buttons
        incrementButtonConfig(R.id.incrementButton1, 1);
        incrementButtonConfig(R.id.incrementButton5, 5);
        incrementButtonConfig(R.id.incrementButton10, 10);

        // Set up the onClickListener to play the animation and reload the leaderboard
        ImageButton reloadLeaderboardButton = findViewById(R.id.reloadLeaderboardButton);
        reloadLeaderboardButton.setOnClickListener(v -> {
            v.startAnimation(spinAnimation);
            loadOnlineLeaderboard();
        });

        // Change Name Button    Temporarily
        changeNameButton = findViewById(R.id.changeNameButton);
        changeNameButton.setOnClickListener(v -> promptForUserName());

        // Quest buttons
        questButtons = new Button[3];
        for (int i = 0; i < 3; i++) {
            int id = getResources().getIdentifier("questButton" + (i + 1), "id", getPackageName());
            questButtons[i] = findViewById(id);
            int finalI = i;
            questButtons[i].setOnClickListener(v -> completeQuest(finalI));
        }

        // Bonus button
        bonusButton = findViewById(R.id.bonusButton);
        bonusButton.setOnClickListener(v -> collectBonus());
    }

    private void incrementButtonConfig(int buttonId, int incrementValue) {
        Button button = findViewById(buttonId);
        button.setBackgroundResource(R.drawable.buttonshape);
        button.setOnClickListener(v -> incrementCounter(incrementValue));
    }


    // --- State Management Methods ---
    private void loadState() {
        counter = sharedPreferences.getInt(KEY_COUNTER, 0);
        xp = sharedPreferences.getInt(KEY_XP, 0);
        level = sharedPreferences.getInt(KEY_LEVEL, 1);
        questsCompleted = stringToBooleanArray(sharedPreferences.getString(KEY_QUESTS_COMPLETED, "000"));
        bonusCollected = sharedPreferences.getBoolean(KEY_BONUS_COLLECTED, false);

        // Restore daily push-up log
        String dailyLogJson = sharedPreferences.getString("dailyPushupLog", "{}");
        dailyPushupLog.putAll(new Gson().fromJson(dailyLogJson, new TypeToken<HashMap<String, Integer>>() {}.getType()));
    }


    private void saveState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_COUNTER, counter);
        editor.putInt(KEY_XP, xp);
        editor.putInt(KEY_LEVEL, level);
        editor.putString(KEY_QUESTS_COMPLETED, booleanArrayToString(questsCompleted));
        editor.putBoolean(KEY_BONUS_COLLECTED, bonusCollected);
        editor.putInt(KEY_LAST_KNOWN_DAY, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        editor.putString(KEY_USERNAME, getUserName());
        editor.putString("dailyPushupLog", new Gson().toJson(dailyPushupLog)); // Save daily log
        editor.apply();
    }

    private void resetQuestsIfNewDay() {
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int lastDay = sharedPreferences.getInt(KEY_LAST_KNOWN_DAY, -1);
        if (currentDay != lastDay) {
            questsCompleted = new boolean[3];
            bonusCollected = false;
            saveState();
        }
        updateQuestsUI();
    }


    // --- UI Update Methods ---
    private void updateUI() {
        counterTextView.setText("Push-Ups: " + counter);
        levelTextView.setText("Level: " + level);
        updateXpProgressBar();
        updateQuestsUI();
    }

    private void updateXpProgressBar() {
        int xpForNextLevel = calculateXpForNextLevel();
        xpProgressBar.setProgress((xp * 100) / xpForNextLevel);
    }

    private void updateQuestsUI() {
        String[] quests = {"Quest 1", "Quest 2", "Quest 3"};
        questsTextView.setText("Tägliche Quests:\n" + String.join("\n", quests));
        for (int i = 0; i < questsCompleted.length; i++) {
            questButtons[i].setText(quests[i]);
            questButtons[i].setEnabled(!questsCompleted[i]);
        }
    }


    // --- Other Methods ---
    private void promptForUserName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Neuen Namen eingeben");
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                changeUserName(newName);
            } else {
                Toast.makeText(this, "Name kann nicht leer sein!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void changeUserName(String newName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, newName);
        editor.apply();
        Toast.makeText(this, "Name geändert zu: " + newName, Toast.LENGTH_SHORT).show();
    }

    private String getUserName() {
        return sharedPreferences.getString(KEY_USERNAME, "Unbekannt");
    }

    private String booleanArrayToString(boolean[] array) {
        StringBuilder builder = new StringBuilder();
        for (boolean b : array) {
            builder.append(b ? "1" : "0");
        }
        return builder.toString();
    }

    private boolean[] stringToBooleanArray(String s) {
        boolean[] array = new boolean[s.length()];
        for (int i = 0; i < s.length(); i++) {
            array[i] = s.charAt(i) == '1';
        }
        return array;
    }


    // --- Quests and Bonus Handling ---
    private void completeQuest(int index) {
        if (!questsCompleted[index]) {
            questsCompleted[index] = true;
            questCompletions[index]++; // Increment quest completion count
            xp += 20;
            checkLevelUp();
            saveState();
            updateUI();
        }
    }

    private void collectBonus() {
        for (boolean completed : questsCompleted) {
            if (!completed) {
                Toast.makeText(this, "Complete all quests first!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (!bonusCollected) {
            xp += 50;
            bonusXpCollected++; // Increment bonus XP collection count
            bonusCollected = true;
            checkLevelUp();
            saveState();
            updateUI();
        }
    }

    private void checkLevelUp() {
        int xpForNextLevel = calculateXpForNextLevel();
        while (xp >= xpForNextLevel) {
            xp -= xpForNextLevel;
            level++;
            Toast.makeText(this, "Level Up! Du bist jetzt level " + level, Toast.LENGTH_SHORT).show();
        }
    }

    private void incrementCounter(int amount) {
        counter += amount;
        xp += amount;
        checkLevelUp();
        saveState();
        updateUI();
        updateOnlineLeaderboard(getUserName(), counter, level); // Update leaderboard with new score
    }


    // --- Data Methods ---
    private int calculateTotalXp(int level) {
        int totalXp = 0;
        for (int i = 1; i < level; i++) {
            totalXp += (int) (10 * Math.pow(i, 1.5));
        }
        return totalXp;
    }

    private int calculateXpForNextLevel() {
        return (int) (10 * Math.pow(level, 1.5));
    }

    private int calculateTotalQuestCompletions() {
        return questCompletions[0] + questCompletions[1] + questCompletions[2];
    }

    private final HashMap<String, Integer> dailyPushupLog = new HashMap<>(); // Tracks daily push-ups

    private void logDailyPushups(int amount) {
        String today = getDayName();
        dailyPushupLog.put(today, dailyPushupLog.getOrDefault(today, 0) + amount);
    }

    private int getDailyPushups() {
        String today = getDayName();
        return dailyPushupLog.getOrDefault(today, 0);
    }

    private int getWeeklyPushups() {
        int total = 0;
        for (String day : getLastSevenDays()) {
            total += dailyPushupLog.getOrDefault(day, 0);
        }
        return total;
    }

    private int getMonthlyPushups() {
        return dailyPushupLog.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int getYearlyPushups() {
        return dailyPushupLog.values().stream().mapToInt(Integer::intValue).sum(); // Approximate
    }


    private String getDayName() {
        return Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    }

    private List<String> getLastSevenDays() {
        List<String> last7Days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            last7Days.add(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()));
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        }
        return last7Days;
    }

    private double calculateAvgPushupsPerDay() {
        int totalDays = dailyPushupLog.size();
        return totalDays > 0 ? (double) getMonthlyPushups() / totalDays : 0;
    }

    private double calculateAvgPushupsPerWeek() {
        return (double) getWeeklyPushups() / 7;
    }

    private double calculateAvgPushupsPerMonth() {
        return (double) getMonthlyPushups() / 30; // Assuming a 30-day month
    }


    // --- Job Scheduler ---
    private void startAutomaticLeaderboardUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ComponentName serviceComponent = new ComponentName(this, LeaderboardUpdateService.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent)
                    .setPeriodic(15 * 60 * 1000) // Minimum interval: 15 minutes
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            int resultCode = jobScheduler.schedule(builder.build());
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.d("Leaderboard", "Automatic updates scheduled");
            } else {
                Log.w("Leaderboard", "Failed to schedule automatic updates: " + resultCode);
            }
        } else {
            Toast.makeText(this, "Automatische Aktualisierung des Leaderboards wird auf diesem Gerät nicht unterstützt.", Toast.LENGTH_SHORT).show();
        }
    }


    // --- Networking Methods ---
    private void loadOnlineLeaderboard() {
        leaderboardAPI.getLeaderboard().enqueue(new Callback<List<LeaderboardEntry>>() {
            @Override
            public void onResponse(Call<List<LeaderboardEntry>> call, Response<List<LeaderboardEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> displayEntries = new ArrayList<>();
                    for (LeaderboardEntry entry : response.body()) {
                        displayEntries.add(entry.getName() + " - Push-ups: " + entry.getPushups() + ", Level: " + entry.getLevel());
                    }
                    updateLeaderboardUI(displayEntries);
                }
            }

            @Override
            public void onFailure(Call<List<LeaderboardEntry>> call, Throwable t) {
                Log.e("Leaderboard", "Fehler beim Laden des Leaderboards", t);
            }
        });
    }

    private void updateLeaderboardUI(List<String> displayEntries) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayEntries);
        ListView leaderboardView = findViewById(R.id.leaderboardListView);
        leaderboardView.setAdapter(adapter);
    }

    private void registerLeaderboardReceiver() {
        IntentFilter filter = new IntentFilter("UPDATE_LEADERBOARD");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(leaderboardReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(leaderboardReceiver, filter);
        }
    }

    private final Handler dataSyncHandler = new Handler();
    private final int DATA_SYNC_INTERVAL = 5000; // Sync every 5 seconds

    private void startPeriodicDataSync() {
        dataSyncHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Update leaderboard data
                updateOnlineLeaderboard(getUserName(), counter, level);

                // Schedule the next update
                dataSyncHandler.postDelayed(this, DATA_SYNC_INTERVAL);
            }
        }, DATA_SYNC_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(leaderboardReceiver);
    }

    private final BroadcastReceiver leaderboardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> leaderboardData = intent.getStringArrayListExtra("leaderboardData");
            if (leaderboardData != null) {
                updateLeaderboardUI(leaderboardData);
            }
        }
    };

    private void updateOnlineLeaderboard(String name, int pushups, int level) {
        String playerId = name.toLowerCase().replaceAll("\\s+", "-");
        Log.d("Leaderboard", "Generated player ID: " + playerId);

        // Create the leaderboard entry
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setName(name);
        entry.setPushups(pushups);
        entry.setLevel(level);
        entry.setTotalXp(xp + calculateTotalXp(level));
        entry.setXpForNextLevel(calculateXpForNextLevel());
        entry.setCurrentXp(xp);

        // Update quests and bonus stats
        entry.setTotalQuestsCompleted(calculateTotalQuestCompletions());
        entry.setQuest1Completions(questCompletions[0]);
        entry.setQuest2Completions(questCompletions[1]);
        entry.setQuest3Completions(questCompletions[2]);
        entry.setBonusXpCollected(bonusXpCollected);

        // Push-up stats
        entry.setDailyPushups(getDailyPushups());
        entry.setWeeklyPushups(getWeeklyPushups());
        entry.setMonthlyPushups(getMonthlyPushups());
        entry.setYearlyPushups(getYearlyPushups());

        // Dynamically calculate averages
        entry.setAvgPushupsPerDay(getDailyPushups());
        entry.setAvgPushupsPerWeek((double) getWeeklyPushups() / 7);
        entry.setAvgPushupsPerMonth((double) getMonthlyPushups() / 30); // Approximate

        // Send data to MockAPI
        leaderboardAPI.updateEntry(playerId, entry).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("Leaderboard", "Leaderboard updated successfully");
                } else {
                    Log.e("Leaderboard", "Failed to update leaderboard: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Leaderboard", "Error updating leaderboard", t);
            }
        });
    }


    public class LeaderboardUpdateService extends JobService {

        private static LeaderboardAPI leaderboardAPI;

        @Override
        public boolean onStartJob(JobParameters params) {
            Log.d("Leaderboard", "Leaderboard update job started");

            // Fetch leaderboard data asynchronously
            fetchLeaderboardData(this, params);

            // Indicate that the job is still running
            return true;
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            Log.w("Leaderboard", "Leaderboard update job stopped");
            return false; // No need to retry
        }

        private void fetchLeaderboardData(Context context, JobParameters params) {
            // Initialize Retrofit if not already done
            if (leaderboardAPI == null) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(AppConfig.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                leaderboardAPI = retrofit.create(LeaderboardAPI.class);
            }

            leaderboardAPI.getLeaderboard().enqueue(new Callback<List<LeaderboardEntry>>() {
                @Override
                public void onResponse(Call<List<LeaderboardEntry>> call, Response<List<LeaderboardEntry>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<LeaderboardEntry> entries = response.body();

                        // Prepare broadcast
                        Intent intent = new Intent("UPDATE_LEADERBOARD");
                        ArrayList<String> displayEntries = new ArrayList<>();
                        for (LeaderboardEntry entry : entries) {
                            displayEntries.add(entry.getName() + " - Push-ups: " + entry.getPushups() + ", Level: " + entry.getLevel());
                        }
                        intent.putStringArrayListExtra("leaderboardData", displayEntries);
                        context.sendBroadcast(intent);

                        Log.d("LeaderboardService", "Broadcast sent with updated leaderboard data");
                    } else {
                        Log.e("LeaderboardService", "Failed to fetch leaderboard data: " + response.errorBody());
                    }
                    // Mark job as finished
                    jobFinished(params, false);
                }

                @Override
                public void onFailure(Call<List<LeaderboardEntry>> call, Throwable t) {
                    Log.e("LeaderboardService", "Error fetching leaderboard data", t);
                    // Mark job as finished even on failure
                    jobFinished(params, false);
                }
            });
        }
    }
}