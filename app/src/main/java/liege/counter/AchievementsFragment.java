package liege.counter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AchievementsFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private ListView achievementListView;
    private TextView titleDisplayView;
    private MainActivity mainActivity;
    private AchievementManager achievementManager;
    private AchievementAdapter achievementAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_achievements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) requireActivity();
        achievementManager = AchievementManager.getInstance(requireContext());

        achievementListView = view.findViewById(R.id.achievementListView);
        titleDisplayView = view.findViewById(R.id.achievementTitleDisplay);

        achievementAdapter = new AchievementAdapter();
        achievementListView.setAdapter(achievementAdapter);
        updateTitleDisplay();

        Button chooseTitleBtn = view.findViewById(R.id.chooseTitleButton);
        chooseTitleBtn.setOnClickListener(v -> showTitleChooserDialog());
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
        if (achievementAdapter != null) {
            achievementAdapter.refreshSortedList();
            achievementAdapter.notifyDataSetChanged();
        }
        updateTitleDisplay();
    }

    private void updateTitleDisplay() {
        String title = achievementManager.getActiveTitle();
        if (title != null && !title.isEmpty()) {
            int color = AchievementManager.getTitleColor(title);
            titleDisplayView.setText("Aktueller Titel: " + title);
            titleDisplayView.setTextColor(color);
        } else {
            titleDisplayView.setText("Aktueller Titel: \u2014");
            titleDisplayView.setTextColor(0xFF9E9E9E);
        }
    }

    private void showTitleChooserDialog() {
        Set<String> completedIds = achievementManager.getCompletedIds();
        List<String> unlockedTitles = new ArrayList<>();
        unlockedTitles.add("Automatisch (h\u00f6chster Rang)");

        for (AchievementManager.AchievementDef ach : AchievementManager.ACHIEVEMENTS) {
            if (completedIds.contains(ach.id) && ach.titleReward != null
                    && !ach.titleReward.isEmpty() && !unlockedTitles.contains(ach.titleReward)) {
                unlockedTitles.add(ach.titleReward);
            }
        }

        if (unlockedTitles.size() <= 1) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Titel w\u00e4hlen")
                    .setMessage("Du hast noch keine Titel freigeschaltet. Schlie\u00dfe Errungenschaften ab!")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] options = unlockedTitles.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Titel w\u00e4hlen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        achievementManager.setSelectedTitle(null);
                    } else {
                        achievementManager.setSelectedTitle(options[which]);
                    }
                    updateTitleDisplay();
                })
                .show();
    }

    // =====================================================================
    // Adapter
    // =====================================================================

    private class AchievementAdapter extends BaseAdapter {

        private final List<AchievementManager.AchievementDef> sortedList = new ArrayList<>();

        AchievementAdapter() {
            refreshSortedList();
        }

        void refreshSortedList() {
            sortedList.clear();
            // Add incomplete achievements first
            for (AchievementManager.AchievementDef ach : AchievementManager.ACHIEVEMENTS) {
                if (!achievementManager.isCompleted(ach.id)) {
                    sortedList.add(ach);
                }
            }
            // Then add completed achievements
            for (AchievementManager.AchievementDef ach : AchievementManager.ACHIEVEMENTS) {
                if (achievementManager.isCompleted(ach.id)) {
                    sortedList.add(ach);
                }
            }
        }

        @Override
        public int getCount() {
            return sortedList.size();
        }

        @Override
        public AchievementManager.AchievementDef getItem(int position) {
            return sortedList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_achievement, parent, false);
            }

            AchievementManager.AchievementDef ach = getItem(position);
            boolean completed = achievementManager.isCompleted(ach.id);

            View card = convertView.findViewById(R.id.achievementCard);
            card.setBackgroundResource(completed
                    ? R.drawable.achievement_card_completed_bg
                    : R.drawable.achievement_card_bg);

            TextView iconView = convertView.findViewById(R.id.achievementIcon);
            iconView.setText(completed ? "\u2705" : ach.icon);

            TextView nameView = convertView.findViewById(R.id.achievementName);
            TextView descView = convertView.findViewById(R.id.achievementDesc);
            nameView.setText(ach.name);
            descView.setText(ach.description);

            if (completed) {
                nameView.setTextColor(0xFFFFFFFF);
                descView.setTextColor(0xFF7C4DFF);
            } else {
                nameView.setTextColor(0xFFBBBBBB);
                descView.setTextColor(0xFF9E9E9E);
            }

            ProgressBar progressBar = convertView.findViewById(R.id.achievementProgress);
            TextView progressText = convertView.findViewById(R.id.achievementProgressText);

            int progress = achievementManager.getProgress(ach, mainActivity);
            int currentValue = achievementManager.getCurrentValue(ach.category, mainActivity);
            progressBar.setProgress(progress);

            if (completed) {
                progressText.setText("Abgeschlossen \u2713");
                progressText.setTextColor(0xFF7C4DFF);
            } else {
                progressText.setText(currentValue + "/" + ach.target);
                progressText.setTextColor(0xFF9E9E9E);
            }

            TextView titleView = convertView.findViewById(R.id.achievementTitle);
            TextView rewardView = convertView.findViewById(R.id.achievementReward);

            if (ach.titleReward != null && !ach.titleReward.isEmpty()) {
                titleView.setVisibility(View.VISIBLE);
                titleView.setText(ach.titleReward);
                titleView.setTextColor(ach.titleColor);
                rewardView.setText("Titel: " + ach.titleReward);
            } else {
                titleView.setVisibility(View.GONE);
                rewardView.setText("");
            }

            return convertView;
        }
    }
}
