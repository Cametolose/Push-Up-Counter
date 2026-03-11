package liege.counter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class LeaderboardAdapter extends ArrayAdapter<LeaderboardEntry> {

    public LeaderboardAdapter(@NonNull Context context, @NonNull List<LeaderboardEntry> entries) {
        super(context, 0, entries);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_leaderboard, parent, false);
        }

        LeaderboardEntry entry = getItem(position);
        if (entry == null) return convertView;

        TextView rankView    = convertView.findViewById(R.id.lbRank);
        TextView nameView    = convertView.findViewById(R.id.lbName);
        TextView pushupsView = convertView.findViewById(R.id.lbPushups);
        TextView levelView   = convertView.findViewById(R.id.lbLevel);
        TextView streakView  = convertView.findViewById(R.id.lbStreak);

        rankView.setText("#" + (position + 1));
        nameView.setText(entry.getName() != null ? entry.getName() : "–");
        pushupsView.setText("💪 " + entry.getPushups() + " Liegestütze");
        levelView.setText("⭐ Level " + entry.getLevel());
        streakView.setText("  🔥 " + entry.getStreak() + " Tage");

        return convertView;
    }
}
