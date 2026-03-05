package liege.counter;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private TextView counterTextView;
    private TextView levelTextView;
    private TextView xpTextView;
    private ProgressBar xpProgressBar;
    private TextView todayCountTextView;
    private TextView weeklyTextView;
    private TextView totalTextView;
    private TextView streakTextView;

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
        todayCountTextView = view.findViewById(R.id.todayCountTextView);
        weeklyTextView     = view.findViewById(R.id.weeklyTextView);
        totalTextView      = view.findViewById(R.id.totalTextView);
        streakTextView     = view.findViewById(R.id.streakTextView);

        setupButtons(view);
        updateDisplay();
    }

    private void setupButtons(View root) {
        setupIncrementButton(root, R.id.incrementButton1, 1);
        setupIncrementButton(root, R.id.incrementButton5, 5);
        setupIncrementButton(root, R.id.incrementButton10, 10);

        // Long-press on +1 for -1 (undo)
        Button btn1 = root.findViewById(R.id.incrementButton1);
        btn1.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            mainActivity.decrementCounter(1);
            Toast.makeText(getContext(), "−1 rückgängig gemacht", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void setupIncrementButton(View root, int buttonId, int amount) {
        Button button = root.findViewById(buttonId);
        button.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            int xpGained = mainActivity.incrementCounter(amount);
            if (xpGained > 0) {
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
        int total    = mainActivity.getLogTotal();
        int xpNeeded = mainActivity.getXpForNextLevel();

        counterTextView.setText("Liegestützen: " + counter);
        levelTextView.setText("Level: " + level);
        xpTextView.setText("XP: " + xp + "/" + xpNeeded);
        xpProgressBar.setProgress(xpNeeded > 0 ? (xp * 100) / xpNeeded : 0);
        todayCountTextView.setText("Heute: " + daily);
        weeklyTextView.setText(String.valueOf(weekly));
        totalTextView.setText(String.valueOf(total));

        int streak = mainActivity.getStreak();
        if (streak >= 2) {
            streakTextView.setVisibility(View.VISIBLE);
            streakTextView.setText("🔥 " + streak + " Tage in Folge!");
        } else {
            streakTextView.setVisibility(View.GONE);
        }
    }
}
