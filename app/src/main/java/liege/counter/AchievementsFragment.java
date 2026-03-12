package liege.counter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AchievementsFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private ListView achievementListView;
    private TextView titleDisplayView;
    private MainActivity mainActivity;
    private AchievementManager achievementManager;

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

        achievementListView.setAdapter(new AchievementAdapter());
        updateTitleDisplay();
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
        if (achievementListView != null && achievementListView.getAdapter() != null) {
            ((BaseAdapter) achievementListView.getAdapter()).notifyDataSetChanged();
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
            titleDisplayView.setText("Aktueller Titel: —");
            titleDisplayView.setTextColor(0xFF9E9E9E);
        }
    }

    // =====================================================================
    // Adapter
    // =====================================================================

    private class AchievementAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return AchievementManager.ACHIEVEMENTS.length;
        }

        @Override
        public AchievementManager.AchievementDef getItem(int position) {
            return AchievementManager.ACHIEVEMENTS[position];
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

            // Card background
            View card = convertView.findViewById(R.id.achievementCard);
            card.setBackgroundResource(completed
                    ? R.drawable.achievement_card_completed_bg
                    : R.drawable.achievement_card_bg);

            // Icon
            TextView iconView = convertView.findViewById(R.id.achievementIcon);
            iconView.setText(completed ? "✅" : ach.icon);

            // Name & Description
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

            // Progress
            ProgressBar progressBar = convertView.findViewById(R.id.achievementProgress);
            TextView progressText = convertView.findViewById(R.id.achievementProgressText);

            int progress = achievementManager.getProgress(ach, mainActivity);
            int currentValue = achievementManager.getCurrentValue(ach.category, mainActivity);
            progressBar.setProgress(progress);

            if (completed) {
                progressText.setText("Abgeschlossen ✓");
                progressText.setTextColor(0xFF7C4DFF);
            } else {
                progressText.setText(currentValue + "/" + ach.target);
                progressText.setTextColor(0xFF9E9E9E);
            }

            // Title reward
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
