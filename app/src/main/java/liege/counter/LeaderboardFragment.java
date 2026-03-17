package liege.counter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LeaderboardFragment extends Fragment {

    private static final String ACTION_UPDATE_LEADERBOARD = "UPDATE_LEADERBOARD";

    private ListView    listView;
    private ProgressBar loadingBar;
    private TextView    emptyText;
    private TextView    monthlyInfo;
    private Button      btnNormal;
    private Button      btnMonthly;
    private Animation   spinAnimation;
    private LeaderboardAPI leaderboardAPI;
    private List<LeaderboardEntry> allEntries = new ArrayList<>();
    private boolean showMonthly = false;

    /** Triggered by background job — just request a fresh load. */
    private final BroadcastReceiver leaderboardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadLeaderboard();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView      = view.findViewById(R.id.leaderboardListView);
        loadingBar    = view.findViewById(R.id.leaderboardLoading);
        emptyText     = view.findViewById(R.id.leaderboardEmpty);
        monthlyInfo   = view.findViewById(R.id.monthlyLeaderboardInfo);
        btnNormal     = view.findViewById(R.id.btnLeaderboardNormal);
        btnMonthly    = view.findViewById(R.id.btnLeaderboardMonthly);
        spinAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.spin);

        btnNormal.setOnClickListener(v -> {
            showMonthly = false;
            updateLeaderboardView();
        });
        btnMonthly.setOnClickListener(v -> {
            showMonthly = true;
            updateLeaderboardView();
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.SUPABASE_URL)
                .client(MainActivity.buildSupabaseClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        leaderboardAPI = retrofit.create(LeaderboardAPI.class);

        ImageButton reloadButton = view.findViewById(R.id.reloadLeaderboardButton);
        reloadButton.setOnClickListener(v -> {
            v.startAnimation(spinAnimation);
            loadLeaderboard();
        });

        loadLeaderboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ACTION_UPDATE_LEADERBOARD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(leaderboardReceiver, filter,
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(leaderboardReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(leaderboardReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private void loadLeaderboard() {
        loadingBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        leaderboardAPI.getLeaderboard().enqueue(new Callback<List<LeaderboardEntry>>() {
            @Override
            public void onResponse(Call<List<LeaderboardEntry>> call,
                                   Response<List<LeaderboardEntry>> response) {
                if (!isAdded()) return;
                loadingBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    showEntries(response.body());
                } else {
                    showEmpty();
                }
            }

            @Override
            public void onFailure(Call<List<LeaderboardEntry>> call, Throwable t) {
                if (!isAdded()) return;
                loadingBar.setVisibility(View.GONE);
                Log.e("LeaderboardFragment", "Fehler beim Laden", t);
                showEmpty();
            }
        });
    }

    private void showEntries(List<LeaderboardEntry> entries) {
        if (!isAdded()) return;
        if (entries.isEmpty()) {
            showEmpty();
            return;
        }

        // Filter out banned players (banned flag set in database by admin)
        List<LeaderboardEntry> filtered = new ArrayList<>();
        for (LeaderboardEntry entry : entries) {
            if (!entry.isBanned()) {
                filtered.add(entry);
            }
        }

        if (filtered.isEmpty()) {
            showEmpty();
            return;
        }

        // Check for rank drop and send notification if needed
        String myName = ((MainActivity) requireActivity()).getUsername();
        int currentRank = -1;
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).getName() != null
                    && filtered.get(i).getName().equalsIgnoreCase(myName)) {
                currentRank = i + 1;
                break;
            }
        }
        if (currentRank > 0) {
            android.content.SharedPreferences rankPrefs = requireContext()
                    .getSharedPreferences("NotificationPrefs", android.content.Context.MODE_PRIVATE);
            int prevRank = rankPrefs.getInt("lastLeaderboardRank_" + myName, currentRank);
            boolean lbEnabled = rankPrefs.getBoolean("lbNotifEnabled", true);
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(new java.util.Date());
            String lastLbNotif = rankPrefs.getString("lastLbNotifDay", "");
            if (lbEnabled && currentRank > prevRank && !today.equals(lastLbNotif)) {
                NotificationHelper.sendLeaderboardNotification(requireContext());
                rankPrefs.edit().putString("lastLbNotifDay", today).apply();
            }
            rankPrefs.edit().putInt("lastLeaderboardRank_" + myName, currentRank).apply();
        }

        // Check if current user is #1 monthly and award Goat title
        List<LeaderboardEntry> monthlyFiltered = getMonthlyFiltered(filtered);
        if (!monthlyFiltered.isEmpty()) {
            LeaderboardEntry monthlyLeader = monthlyFiltered.get(0);
            if (monthlyLeader.getName() != null && monthlyLeader.getName().equalsIgnoreCase(myName)) {
                AchievementManager.getInstance(requireContext()).earnGoatTitle();
            }
        }

        allEntries = filtered;
        updateLeaderboardView();
    }

    private List<LeaderboardEntry> getMonthlyFiltered(List<LeaderboardEntry> base) {
        List<LeaderboardEntry> monthly = new ArrayList<>();
        for (LeaderboardEntry e : base) {
            if (e.getMonthlyPushups() > 0) monthly.add(e);
        }
        monthly.sort(Comparator.comparingInt(LeaderboardEntry::getMonthlyPushups).reversed());
        return monthly;
    }

    private void updateLeaderboardView() {
        if (!isAdded() || allEntries == null || allEntries.isEmpty()) return;

        List<LeaderboardEntry> toShow;
        if (showMonthly) {
            toShow = getMonthlyFiltered(allEntries);
            monthlyInfo.setVisibility(View.VISIBLE);
            btnMonthly.setAlpha(1.0f);
            btnNormal.setAlpha(0.5f);
        } else {
            toShow = allEntries;
            monthlyInfo.setVisibility(View.GONE);
            btnNormal.setAlpha(1.0f);
            btnMonthly.setAlpha(0.5f);
        }

        if (toShow.isEmpty()) {
            showEmpty();
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            listView.setAdapter(new LeaderboardAdapter(requireContext(), toShow, showMonthly));
        }
    }

    private void showEmpty() {
        listView.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
    }
}

