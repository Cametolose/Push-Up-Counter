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
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LeaderboardFragment extends Fragment {

    private static final String ACTION_UPDATE_LEADERBOARD = "UPDATE_LEADERBOARD";
    private static final String EXTRA_LEADERBOARD_DATA    = "leaderboardData";

    private ListView    listView;
    private ProgressBar loadingBar;
    private TextView    emptyText;
    private Animation   spinAnimation;
    private LeaderboardAPI leaderboardAPI;

    private final BroadcastReceiver leaderboardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> data = intent.getStringArrayListExtra(EXTRA_LEADERBOARD_DATA);
            if (data != null) updateList(data);
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
        spinAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.spin);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(requireContext().getString(R.string.base_url))
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
                    List<String> rows = new ArrayList<>();
                    int rank = 1;
                    for (LeaderboardEntry e : response.body()) {
                        rows.add("#" + rank + "  " + e.getName()
                                + "   Liegestützen: " + e.getPushups()
                                + "   Level: " + e.getLevel());
                        rank++;
                    }
                    updateList(rows);
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

    private void updateList(List<String> rows) {
        if (!isAdded()) return;
        if (rows.isEmpty()) {
            showEmpty();
            return;
        }
        listView.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);
    }

    private void showEmpty() {
        listView.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
    }
}
