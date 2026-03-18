package liege.counter;

import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

@SuppressWarnings("deprecation")
public class SettingsFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private static final String NOTIF_PREFS        = "NotificationPrefs";
    private static final String KEY_STREAK_ENABLED = "streakNotifEnabled";
    private static final String KEY_LB_ENABLED     = "lbNotifEnabled";

    private TextView usernameDisplay;
    private TextView statsTotalPushups;
    private TextView statsDailyPushups;
    private TextView statsWeeklyPushups;
    private TextView statsMonthlyPushups;
    private TextView statsYearlyPushups;
    private TextView statsLevel;
    private TextView statsTotalXp;
    private TextView statsStreak;
    private TextView statsTotalQuests;
    private TextView statsQuest1Count;
    private TextView statsQuest2Count;
    private TextView statsQuest3Count;
    private TextView statsBonusCount;
    private TextView notificationTimeDisplay;
    private LinearLayout exactAlarmRow;
    private TextView exactAlarmStatus;

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

        usernameDisplay    = view.findViewById(R.id.usernameDisplay);
        statsTotalPushups  = view.findViewById(R.id.statsTotalPushups);
        statsDailyPushups  = view.findViewById(R.id.statsDailyPushups);
        statsWeeklyPushups = view.findViewById(R.id.statsWeeklyPushups);
        statsMonthlyPushups = view.findViewById(R.id.statsMonthlyPushups);
        statsYearlyPushups = view.findViewById(R.id.statsYearlyPushups);
        statsLevel         = view.findViewById(R.id.statsLevel);
        statsTotalXp       = view.findViewById(R.id.statsTotalXp);
        statsStreak        = view.findViewById(R.id.statsStreak);
        statsTotalQuests   = view.findViewById(R.id.statsTotalQuests);
        statsQuest1Count   = view.findViewById(R.id.statsQuest1Count);
        statsQuest2Count   = view.findViewById(R.id.statsQuest2Count);
        statsQuest3Count   = view.findViewById(R.id.statsQuest3Count);
        statsBonusCount    = view.findViewById(R.id.statsBonusCount);
        notificationTimeDisplay = view.findViewById(R.id.notificationTimeDisplay);
        exactAlarmRow           = view.findViewById(R.id.exactAlarmRow);
        exactAlarmStatus        = view.findViewById(R.id.exactAlarmStatus);

        // Name change button -- hide after name is set
        Button changeNameButton = view.findViewById(R.id.changeNameButton);
        String currentName = mainActivity.getUsername();
        if (!currentName.equals("Unbekannt") && !currentName.trim().isEmpty()) {
            changeNameButton.setVisibility(View.GONE);
        } else {
            changeNameButton.setOnClickListener(v -> showChangeNameDialog());
        }

        // Notification switches
        SharedPreferences notifPrefs = requireContext()
                .getSharedPreferences(NOTIF_PREFS, requireContext().MODE_PRIVATE);

        Switch switchStreak = view.findViewById(R.id.switchStreakNotification);
        switchStreak.setChecked(notifPrefs.getBoolean(KEY_STREAK_ENABLED, true));
        switchStreak.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notifPrefs.edit().putBoolean(KEY_STREAK_ENABLED, isChecked).apply();
            if (isChecked) {
                NotificationScheduler.scheduleStreakAlarm(requireContext());
            } else {
                NotificationScheduler.cancelStreakAlarm(requireContext());
            }
        });

        Switch switchLeaderboard = view.findViewById(R.id.switchLeaderboardNotification);
        switchLeaderboard.setChecked(notifPrefs.getBoolean(KEY_LB_ENABLED, true));
        switchLeaderboard.setOnCheckedChangeListener((buttonView, isChecked) ->
                notifPrefs.edit().putBoolean(KEY_LB_ENABLED, isChecked).apply());

        updateNotificationTimeDisplay();

        Button changeTimeButton = view.findViewById(R.id.changeNotificationTimeButton);
        changeTimeButton.setOnClickListener(v -> showTimePickerDialog());

        Button exactAlarmBtn = view.findViewById(R.id.exactAlarmSettingsButton);
        if (exactAlarmBtn != null) {
            exactAlarmBtn.setOnClickListener(v -> openExactAlarmSettings());
        }
        updateExactAlarmStatus();

        Button creditsButton = view.findViewById(R.id.creditsButton);
        creditsButton.setOnClickListener(v -> showCreditsDialog());

        updateDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.addStateChangedListener(this);
        updateDisplay();
        updateExactAlarmStatus();
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
        statsMonthlyPushups.setText(String.valueOf(mainActivity.getMonthlyPushups()));
        statsYearlyPushups.setText(String.valueOf(mainActivity.getYearlyPushups()));
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
                .setTitle("Namen eingeben")
                .setView(input)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        mainActivity.setUsername(name);
                        updateDisplay();
                        Button btn = getView() != null ? getView().findViewById(R.id.changeNameButton) : null;
                        if (btn != null) btn.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Name gesetzt: " + name, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Name darf nicht leer sein!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showTimePickerDialog() {
        int hour   = NotificationScheduler.getNotifHour(requireContext());
        int minute = NotificationScheduler.getNotifMinute(requireContext());

        new TimePickerDialog(requireContext(), (timePicker, h, m) -> {
            NotificationScheduler.setNotificationTime(requireContext(), h, m);
            updateNotificationTimeDisplay();
            Toast.makeText(getContext(),
                    String.format("Benachrichtigungszeit: %02d:%02d", h, m),
                    Toast.LENGTH_SHORT).show();
        }, hour, minute, true).show();
    }

    private void updateNotificationTimeDisplay() {
        int hour   = NotificationScheduler.getNotifHour(requireContext());
        int minute = NotificationScheduler.getNotifMinute(requireContext());
        if (notificationTimeDisplay != null) {
            notificationTimeDisplay.setText(String.format("Zeit: %02d:%02d", hour, minute));
        }
    }

    private void updateExactAlarmStatus() {
        if (exactAlarmRow == null || exactAlarmStatus == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            boolean canSchedule = am != null && am.canScheduleExactAlarms();
            exactAlarmRow.setVisibility(View.VISIBLE);
            if (canSchedule) {
                exactAlarmStatus.setText("✓ Exakte Alarme aktiviert");
                exactAlarmStatus.setTextColor(0xFF4CAF50);
                View btn = getView() != null ? getView().findViewById(R.id.exactAlarmSettingsButton) : null;
                if (btn != null) btn.setVisibility(View.GONE);
            } else {
                exactAlarmStatus.setText("⚠ Exakte Alarme deaktiviert");
                exactAlarmStatus.setTextColor(0xFFFF9800);
                View btn = getView() != null ? getView().findViewById(R.id.exactAlarmSettingsButton) : null;
                if (btn != null) btn.setVisibility(View.VISIBLE);
            }
        } else {
            exactAlarmRow.setVisibility(View.GONE);
        }
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        }
    }

    private void showCreditsDialog() {
        mainActivity.onCreditsViewed();
        new AlertDialog.Builder(requireContext())
                .setTitle("Credits")
                .setMessage("Entwickelt von: Alex\n\nIdeen von: Äkwav, Viktor, Philipp, Emil & Alex\n\nBesonderer Dank an die Kartoffel und dem Fake Burger")
                .setPositiveButton("OK", null)
                .show();
    }
}
