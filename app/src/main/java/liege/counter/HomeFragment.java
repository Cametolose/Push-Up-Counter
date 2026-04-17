package liege.counter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class HomeFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private static final String JOKE_PREFS   = "JokePrefs";
    private static final String KEY_JOKE     = "cachedJoke";
    private static final String KEY_JOKE_DAY = "jokeDay";
    private static final TimeZone BERLIN_TIME_ZONE = TimeZone.getTimeZone("Europe/Berlin");
    private static final String[] DAILY_JOKES = {
            "Warum tragen Programmierer immer eine Brille? Weil sie C# nicht sehen. 👓",
            "Ich mache keine Pausen, ich cache nur kurz. 💾",
            "Warum sind Bugs so sportlich? Weil sie ständig in Loops rennen. 🐞",
            "Mein Code funktioniert nicht? Dann ist es wohl ein Feature. ✨",
            "Was sagt der Fitness-Coach zum Dev? Mehr Push-Ups, weniger Push-Force! 💪",
            "Warum ging der Code ins Gym? Für bessere Performance. 🚀",
            "Ich wollte heute refactoren, aber der Legacy-Code hat Nein gesagt. 🧱",
            "Warum mag Java Kaffee? Wegen den Beans. ☕",
            "Ein guter Commit am Tag hält den Hotfix fern. ✅",
            "Warum war der Server traurig? Zu viele Requests ohne Liebe. 🌐",
            "NullPointerException: Gefühle wurden nicht initialisiert. 💔",
            "Ich trainiere wie ich code: erst testen, dann pushen. 📦",
            "Warum sind Arrays fit? Sie haben immer eine feste Größe. 📏",
            "Der Unterschied zwischen mir und meinem Code? Ich kann schlafen. 😴",
            "Was ist der Lieblingssport von Entwicklern? Sprint Planning. 🏃",
            "Warum war der SQL-Query müde? Zu viele Joins. 🔗",
            "Heute nur ein kurzer Workout-Loop: do pushup while(alive). 🔁",
            "Mein Körper sagt Pause, mein Streak sagt nein. 🔥",
            "Warum hat der Dev Muskelkater? Zu viel Overengineering im Gym. 🏋️",
            "404 Motivation not found? Einfach einen Push-Up machen. 🙌",
            "Ich mache Push-Ups, damit mein Code leichter trägt. 🧠",
            "Warum hat Git gewonnen? Es konnte besser committen. 🏆",
            "Der beste Bugfix: erst atmen, dann loggen. 🧘",
            "Warum ist Debuggen wie Sport? Man schwitzt für kleine Fortschritte. 😅",
            "Mein Lieblingskommando: git push und dann echte Push-Ups. 💥",
            "Wenn's brennt: Wasser trinken und Stacktrace lesen. 🚒",
            "Warum feiern Devs Streaks? Weil Konsistenz mehr zählt als Motivation. 📈",
            "Ich bin nicht langsam, ich kompiliere nur innerlich. 🐢",
            "Wer täglich pusht, braucht keinen Montag-Motivationstalk. 📅",
            "Warum war der Workout-Plan stabil? Er hatte gute Abhängigkeiten. 🧩",
            "Ein Push-Up mehr ist besser als ein Excuse mehr. 🫡"
    };

    private TextView counterTextView;
    private TextView levelTextView;
    private TextView xpTextView;
    private ProgressBar xpProgressBar;
    private TextView dailyTextView;
    private TextView weeklyTextView;
    private TextView totalTextView;
    private TextView streakTextView;
    private TextView jokeTextView;

    // Active effects
    private LinearLayout activeEffectsSection;
    private LinearLayout doubleXpEffect;
    private TextView doubleXpTimer;
    private LinearLayout halfXpEffect;
    private TextView halfXpInfo;

    // Inventory
    private LinearLayout inventorySection;
    private LinearLayout negateTrapItem;
    private TextView negateTrapCount;
    private LinearLayout streakSaveItem;
    private TextView streakSaveCount;

    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) requireActivity();

        counterTextView    = view.findViewById(R.id.counterTextView);
        levelTextView      = view.findViewById(R.id.levelTextView);
        xpTextView         = view.findViewById(R.id.xpTextView);
        xpProgressBar      = view.findViewById(R.id.xpProgressBar);
        dailyTextView      = view.findViewById(R.id.dailyTextView);
        weeklyTextView     = view.findViewById(R.id.weeklyTextView);
        totalTextView      = view.findViewById(R.id.totalTextView);
        streakTextView     = view.findViewById(R.id.streakTextView);
        jokeTextView       = view.findViewById(R.id.jokeTextView);

        // Active effects
        activeEffectsSection = view.findViewById(R.id.activeEffectsSection);
        doubleXpEffect       = view.findViewById(R.id.doubleXpEffect);
        doubleXpTimer        = view.findViewById(R.id.doubleXpTimer);
        halfXpEffect         = view.findViewById(R.id.halfXpEffect);
        halfXpInfo           = view.findViewById(R.id.halfXpInfo);

        // Inventory
        inventorySection = view.findViewById(R.id.inventorySection);
        negateTrapItem   = view.findViewById(R.id.negateTrapItem);
        negateTrapCount  = view.findViewById(R.id.negateTrapCount);
        streakSaveItem   = view.findViewById(R.id.streakSaveItem);
        streakSaveCount  = view.findViewById(R.id.streakSaveCount);

        setupButtons(view);
        updateDisplay();
        loadDailyJoke();
    }

    private void setupButtons(View root) {
        setupIncrementButton(root, R.id.incrementButton1, 1);
        setupIncrementButton(root, R.id.incrementButton5, 5);
        setupIncrementButton(root, R.id.incrementButton10, 10);
    }

    private void setupIncrementButton(View root, int buttonId, int amount) {
        Button button = root.findViewById(buttonId);
        button.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            int xpGained = mainActivity.incrementCounter(amount);
            if (xpGained > 10) {
                Toast.makeText(getContext(), "+" + xpGained + " XP!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.addStateChangedListener(this);
        updateDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.removeStateChangedListener(this);
    }

    @Override
    public void onStateChanged() {
        if (getView() != null) {
            updateDisplay();
        }
    }

    private void updateDisplay() {
        if (mainActivity == null) return;

        int counter  = mainActivity.getCounter();
        int xp       = mainActivity.getXp();
        int level    = mainActivity.getLevel();
        int daily    = mainActivity.getDailyPushups();
        int weekly   = mainActivity.getWeeklyPushups();
        int total    = mainActivity.getMonthlyPushups();
        int xpNeeded = mainActivity.getXpForNextLevel();

        counterTextView.setText("Push-Ups: " + counter);
        levelTextView.setText("Level: " + level);
        xpTextView.setText("XP: " + xp + "/" + xpNeeded);
        xpProgressBar.setProgress(xpNeeded > 0 ? (xp * 100) / xpNeeded : 0);
        dailyTextView.setText(String.valueOf(daily));
        weeklyTextView.setText(String.valueOf(weekly));
        totalTextView.setText(String.valueOf(total));

        int streak = mainActivity.getStreak();
        if (streak >= 2) {
            streakTextView.setVisibility(View.VISIBLE);
            streakTextView.setText("🔥 " + streak);
        } else {
            streakTextView.setVisibility(View.GONE);
        }

        // Update active effects and inventory
        updateActiveEffects();
        updateInventory();
    }

    private void updateActiveEffects() {
        ItemManager itemManager = mainActivity.getItemManager();
        boolean hasEffects = false;

        // Double XP
        if (itemManager.isDoubleXpActive()) {
            doubleXpEffect.setVisibility(View.VISIBLE);
            doubleXpTimer.setText("Noch " + ItemManager.formatRemaining(itemManager.getDoubleXpRemaining()));
            hasEffects = true;
        } else {
            doubleXpEffect.setVisibility(View.GONE);
        }

        // Half XP
        if (itemManager.isHalfXpActive()) {
            halfXpEffect.setVisibility(View.VISIBLE);
            halfXpInfo.setText("Von " + itemManager.getHalfXpFrom() + " — Noch "
                    + ItemManager.formatRemaining(itemManager.getHalfXpRemaining()));
            hasEffects = true;
        } else {
            halfXpEffect.setVisibility(View.GONE);
        }

        activeEffectsSection.setVisibility(hasEffects ? View.VISIBLE : View.GONE);
    }

    private void updateInventory() {
        ItemManager itemManager = mainActivity.getItemManager();

        int negateTrapAmt = itemManager.getNegateTrapCount();
        negateTrapItem.setVisibility(View.VISIBLE);
        negateTrapCount.setText("×" + negateTrapAmt);

        int streakSaveAmt = itemManager.getStreakSaveCount();
        streakSaveItem.setVisibility(View.VISIBLE);
        streakSaveCount.setText("×" + streakSaveAmt);

        // Always show inventory section
        inventorySection.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // Daily German joke (cached per calendar day)
    // =========================================================================

    private void loadDailyJoke() {
        String today = getBerlinDayKey();
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(JOKE_PREFS, Context.MODE_PRIVATE);

        String cachedDay  = prefs.getString(KEY_JOKE_DAY, "");
        String cachedJoke = prefs.getString(KEY_JOKE, "");

        if (today.equals(cachedDay) && !cachedJoke.isEmpty()) {
            jokeTextView.setText(cachedJoke);
            return;
        }

        showAndCacheJoke(getDeterministicJokeForDay(today), today, prefs);
    }

    private void showAndCacheJoke(String joke, String day, SharedPreferences prefs) {
        if (jokeTextView != null) jokeTextView.setText(joke);
        prefs.edit().putString(KEY_JOKE, joke).putString(KEY_JOKE_DAY, day).apply();
    }

    private String getBerlinDayKey() {
        Calendar calendar = Calendar.getInstance(BERLIN_TIME_ZONE);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(BERLIN_TIME_ZONE);
        return format.format(calendar.getTime());
    }

    private String getDeterministicJokeForDay(String dayKey) {
        if (DAILY_JOKES.length == 0) {
            return "Warum machen Programmierer keine Push-Ups? Weil sie schon genug Push-Requests haben! 💪";
        }

        int dayOfMonth = 1;
        if (dayKey != null && dayKey.length() >= 10) {
            try {
                dayOfMonth = Integer.parseInt(dayKey.substring(8, 10));
            } catch (NumberFormatException ignored) {
                dayOfMonth = 1;
            }
        }
        int index = Math.floorMod(dayOfMonth - 1, DAILY_JOKES.length);
        return DAILY_JOKES[index];
    }
}
