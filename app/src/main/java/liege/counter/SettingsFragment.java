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
    private TextView statsTotalXp;
    private TextView statsTotalQuests;

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
        statsTotalXp      = view.findViewById(R.id.statsTotalXp);
        statsTotalQuests  = view.findViewById(R.id.statsTotalQuests);

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
        statsTotalXp.setText(String.valueOf(mainActivity.getTotalXpAcrossLevels()));
        statsTotalQuests.setText(String.valueOf(mainActivity.getTotalQuestsCompleted()));
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
