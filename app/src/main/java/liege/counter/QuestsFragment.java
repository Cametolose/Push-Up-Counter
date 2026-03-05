package liege.counter;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class QuestsFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private static final int[] QUEST_TARGETS = {20, 50, 100};

    private final LinearLayout[] questCards  = new LinearLayout[3];
    private final TextView[]     questTitles = new TextView[3];
    private final TextView[]     questDescs  = new TextView[3];
    private final ProgressBar[]  questBars   = new ProgressBar[3];
    private final TextView[]     questTexts  = new TextView[3];
    private final ImageView[]    questChecks = new ImageView[3];

    private LinearLayout bonusCard;
    private ImageView    bonusIcon;
    private Button       bonusButton;
    private TextView     bonusCollectedText;

    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) requireActivity();

        int[] cardIds   = {R.id.questCard1,          R.id.questCard2,          R.id.questCard3};
        int[] titleIds  = {R.id.questTitle1,         R.id.questTitle2,         R.id.questTitle3};
        int[] descIds   = {R.id.questDesc1,          R.id.questDesc2,          R.id.questDesc3};
        int[] barIds    = {R.id.questProgress1,      R.id.questProgress2,      R.id.questProgress3};
        int[] textIds   = {R.id.questProgressText1,  R.id.questProgressText2,  R.id.questProgressText3};
        int[] checkIds  = {R.id.questCheck1,         R.id.questCheck2,         R.id.questCheck3};

        for (int i = 0; i < 3; i++) {
            questCards[i]  = view.findViewById(cardIds[i]);
            questTitles[i] = view.findViewById(titleIds[i]);
            questDescs[i]  = view.findViewById(descIds[i]);
            questBars[i]   = view.findViewById(barIds[i]);
            questTexts[i]  = view.findViewById(textIds[i]);
            questChecks[i] = view.findViewById(checkIds[i]);
        }

        bonusCard          = view.findViewById(R.id.bonusCard);
        bonusIcon          = view.findViewById(R.id.bonusIcon);
        bonusButton        = view.findViewById(R.id.bonusButton);
        bonusCollectedText = view.findViewById(R.id.bonusCollectedText);

        bonusButton.setOnClickListener(v -> mainActivity.collectBonus());

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

        int daily = mainActivity.getDailyPushups();
        boolean allDone = true;

        String[] defaultDescs = {
            "Mache 20 Liegestütze heute",
            "Mache 50 Liegestütze heute",
            "Mache 100 Liegestütze heute"
        };

        for (int i = 0; i < 3; i++) {
            boolean completed = mainActivity.isQuestCompleted(i);
            int target   = QUEST_TARGETS[i];
            int progress = Math.min(daily, target);

            questBars[i].setMax(target);
            questBars[i].setProgress(progress);
            questTexts[i].setText(progress + "/" + target);

            if (completed) {
                questCards[i].setBackgroundResource(R.drawable.quest_card_completed_bg);
                questChecks[i].setVisibility(View.VISIBLE);
                questTitles[i].setPaintFlags(
                        questTitles[i].getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                questDescs[i].setText("Abgeschlossen! ✓");
                questDescs[i].setTextColor(0xFF00E676);
            } else {
                questCards[i].setBackgroundResource(R.drawable.quest_card_bg);
                questChecks[i].setVisibility(View.GONE);
                questTitles[i].setPaintFlags(
                        questTitles[i].getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                questDescs[i].setText(defaultDescs[i]);
                questDescs[i].setTextColor(0xFFBBBBBB);
                allDone = false;
            }
        }

        boolean bonusCollected = mainActivity.isBonusCollected();
        if (bonusCollected) {
            bonusCard.setBackgroundResource(R.drawable.quest_card_completed_bg);
            bonusButton.setVisibility(View.GONE);
            bonusCollectedText.setVisibility(View.VISIBLE);
            bonusIcon.setImageResource(R.drawable.ic_check);
        } else if (allDone) {
            bonusCard.setBackgroundResource(R.drawable.quest_card_locked_bg);
            bonusButton.setVisibility(View.VISIBLE);
            bonusCollectedText.setVisibility(View.GONE);
            bonusButton.setEnabled(true);
            bonusButton.setBackgroundResource(R.drawable.buttonshape);
            bonusButton.getBackground().setTint(0xFFFFD600);
            bonusButton.setTextColor(0xFF000000);
            bonusButton.setText("50 Bonus XP sammeln!");
            bonusIcon.setImageResource(R.drawable.ic_nav_quests);
        } else {
            bonusCard.setBackgroundResource(R.drawable.quest_card_locked_bg);
            bonusButton.setVisibility(View.VISIBLE);
            bonusCollectedText.setVisibility(View.GONE);
            bonusButton.setEnabled(false);
            bonusButton.setBackgroundResource(R.drawable.buttonshape);
            bonusButton.getBackground().setTint(0xFF424242);
            bonusButton.setTextColor(0xFF9E9E9E);
            bonusButton.setText("Alle Aufgaben abschließen");
            bonusIcon.setImageResource(R.drawable.ic_nav_quests);
        }
    }
}
