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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private static final String JOKE_PREFS   = "JokePrefs";
    private static final String KEY_JOKE     = "cachedJoke";
    private static final String KEY_JOKE_DAY = "jokeDay";

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
            streakTextView.setText("🔥 " + streak + " Tage in Folge!");
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
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(JOKE_PREFS, Context.MODE_PRIVATE);

        String cachedDay  = prefs.getString(KEY_JOKE_DAY, "");
        String cachedJoke = prefs.getString(KEY_JOKE, "");

        if (today.equals(cachedDay) && !cachedJoke.isEmpty()) {
            jokeTextView.setText(cachedJoke);
            return;
        }

        // Fetch a fresh joke for today
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://v2.jokeapi.dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        JokeApiService service = retrofit.create(JokeApiService.class);

        service.getGermanJoke("de", "single", "nsfw,racist,sexist")
                .enqueue(new Callback<JokeResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<JokeResponse> call,
                                           @NonNull Response<JokeResponse> response) {
                        if (!isAdded()) return;
                        JokeResponse body = response.body();
                        if (response.isSuccessful() && body != null && !body.isError()) {
                            String joke;
                            if ("twopart".equals(body.getType())
                                    && body.getSetup() != null && body.getDelivery() != null) {
                                joke = body.getSetup() + "\n— " + body.getDelivery();
                            } else {
                                joke = body.getJoke();
                            }
                            if (joke == null || joke.isEmpty()) joke = buildFallbackJoke();
                            showAndCacheJoke(joke, today, prefs);
                        } else {
                            showAndCacheJoke(buildFallbackJoke(), today, prefs);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JokeResponse> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        Log.w("HomeFragment", "Witz konnte nicht geladen werden", t);
                        showAndCacheJoke(buildFallbackJoke(), today, prefs);
                    }
                });
    }

    private void showAndCacheJoke(String joke, String day, SharedPreferences prefs) {
        if (jokeTextView != null) jokeTextView.setText(joke);
        prefs.edit().putString(KEY_JOKE, joke).putString(KEY_JOKE_DAY, day).apply();
    }

    private String buildFallbackJoke() {
        return "Warum machen Programmierer keine Push-Ups? " +
               "Weil sie schon genug Push-Requests haben! 💪";
    }
}
