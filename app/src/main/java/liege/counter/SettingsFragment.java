package liege.counter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private TextView usernameDisplay;
    private TextView statsTotalPushups;
    private TextView statsDailyPushups;
    private TextView statsWeeklyPushups;
    private TextView statsLevel;
    private TextView statsTotalXp;
    private TextView statsStreak;
    private TextView statsTotalQuests;
    private TextView statsQuest1Count;
    private TextView statsQuest2Count;
    private TextView statsQuest3Count;
    private TextView statsBonusCount;

    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) requireActivity();

        usernameDisplay   = view.findViewById(R.id.usernameDisplay);
        statsTotalPushups = view.findViewById(R.id.statsTotalPushups);
        statsDailyPushups = view.findViewById(R.id.statsDailyPushups);
        statsWeeklyPushups = view.findViewById(R.id.statsWeeklyPushups);
        statsLevel        = view.findViewById(R.id.statsLevel);
        statsTotalXp      = view.findViewById(R.id.statsTotalXp);
        statsStreak       = view.findViewById(R.id.statsStreak);
        statsTotalQuests  = view.findViewById(R.id.statsTotalQuests);
        statsQuest1Count  = view.findViewById(R.id.statsQuest1Count);
        statsQuest2Count  = view.findViewById(R.id.statsQuest2Count);
        statsQuest3Count  = view.findViewById(R.id.statsQuest3Count);
        statsBonusCount   = view.findViewById(R.id.statsBonusCount);

        Button changeNameButton = view.findViewById(R.id.changeNameButton);
        changeNameButton.setOnClickListener(v -> showChangeNameDialog());

        updateDisplay();
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
        usernameDisplay.setText(mainActivity.getUsername());
        statsTotalPushups.setText(String.valueOf(mainActivity.getCounter()));
        statsDailyPushups.setText(String.valueOf(mainActivity.getDailyPushups()));
        statsWeeklyPushups.setText(String.valueOf(mainActivity.getWeeklyPushups()));
        statsLevel.setText(String.valueOf(mainActivity.getLevel()));
        statsTotalXp.setText(String.valueOf(mainActivity.getTotalXpAcrossLevels()));
        statsStreak.setText(mainActivity.getStreak() + " Tage");
        statsTotalQuests.setText(String.valueOf(mainActivity.getTotalQuestsCompleted()));
        statsQuest1Count.setText(String.valueOf(mainActivity.getQuestCompletionCount(0)));
        statsQuest2Count.setText(String.valueOf(mainActivity.getQuestCompletionCount(1)));
        statsQuest3Count.setText(String.valueOf(mainActivity.getQuestCompletionCount(2)));
        statsBonusCount.setText(String.valueOf(mainActivity.getBonusXpCollected()));
    }

    private void showChangeNameDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Dein Name");

        new AlertDialog.Builder(requireContext())
                .setTitle("Neuen Namen eingeben")
                .setView(input)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        mainActivity.setUsername(name);
                        updateDisplay();
                        Toast.makeText(getContext(), "Name geändert zu: " + name,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Name darf nicht leer sein!",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }
}

