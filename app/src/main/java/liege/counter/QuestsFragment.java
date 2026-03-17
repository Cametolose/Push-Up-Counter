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

import java.util.Calendar;

public class QuestsFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private static final int[] QUEST_TARGETS = {20, 50, 100};

    /**
     * 21 example tasks: QUEST_DATA[dayIndex][questIndex] = {title, description}
     * dayIndex: 0 = Sunday … 6 = Saturday  (Calendar.DAY_OF_WEEK - 1)
     */
    private static final String[][][] QUEST_DATA = {
        { // Sonntag — Erholung & Stretching
            {"Free Sonntag", "1x Pushup"},
            {"Sonntags-Nacken", "1min Dehnen"},
            {"Sonntags-Sale", "1x Ausfallschritt"}
        },
        { // Montag — Wochenstart
            {"Wochenstart-Power", "2min Dehnen"},
            {"Montags-Schub", "10x Breitarm Push-Ups"},
            {"Mister Monday", "60s Plank"}
        },
        { // Dienstag — Oberkörper
            {"Meta-Aktivierung", "20x Squats"},
            {"Dehnen-Dienstag", "3min Dehnen"},
            {"Dienstags-Dilemma", "10x Diamond Push-Ups"}
        },
        { // Mittwoch — Core & Stabilität
            {"Die Wand Challenge", "25x Wand Push-Ups"},
            {"Wochenmitte-Wandsitzen", "30s Wandsitzen"},
            {"Mittwochs-Meister", "5min Dehnen"}
        },
        { // Donnerstag — Ausdauer
            {"Ausdauer-Basis", "2min Dehnen"},
            {"Donnerstags-Kraft", "25x Knie Push-Ups"},
            {"Donner-Donnerstag", "60s Superman"}
        },
        { // Freitag — Power & Explosivität
            {"Feierabend-Starter", "15x Hampelmänner"},
            {"Freitags-Feuer", "3min Dehnen"},
            {"Wochenend-Countdown", "15x Decline Push-Ups (Füße erhöht)"}
        },
        { // Samstag — Wochenend-Warrior
            {"Wochenend-Warmup", "10x Incline Push-Ups"},
            {"Samstags-Booster", "30x Bergsteiger"},
            {"Wochenend-Warrior", "5min Dehnen"}
        }
    };

    private final LinearLayout[] questCards   = new LinearLayout[3];
    private final TextView[]     questTitles  = new TextView[3];
    private final TextView[]     questDescs   = new TextView[3];
    private final ProgressBar[]  questBars    = new ProgressBar[3];
    private final TextView[]     questTexts   = new TextView[3];
    private final ImageView[]    questChecks  = new ImageView[3];
    private final Button[]       completeButtons = new Button[3];

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

        int[] cardIds    = {R.id.questCard1,             R.id.questCard2,             R.id.questCard3};
        int[] titleIds   = {R.id.questTitle1,            R.id.questTitle2,            R.id.questTitle3};
        int[] descIds    = {R.id.questDesc1,             R.id.questDesc2,             R.id.questDesc3};
        int[] barIds     = {R.id.questProgress1,         R.id.questProgress2,         R.id.questProgress3};
        int[] textIds    = {R.id.questProgressText1,     R.id.questProgressText2,     R.id.questProgressText3};
        int[] checkIds   = {R.id.questCheck1,            R.id.questCheck2,            R.id.questCheck3};
        int[] btnIds     = {R.id.questCompleteButton1,   R.id.questCompleteButton2,   R.id.questCompleteButton3};

        for (int i = 0; i < 3; i++) {
            questCards[i]      = view.findViewById(cardIds[i]);
            questTitles[i]     = view.findViewById(titleIds[i]);
            questDescs[i]      = view.findViewById(descIds[i]);
            questBars[i]       = view.findViewById(barIds[i]);
            questTexts[i]      = view.findViewById(textIds[i]);
            questChecks[i]     = view.findViewById(checkIds[i]);
            completeButtons[i] = view.findViewById(btnIds[i]);

            final int index = i;
            completeButtons[i].setOnClickListener(v -> mainActivity.completeQuest(index));
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

    /** Returns the 0-based day index (0 = Sunday … 6 = Saturday). */
    private int todayDayIndex() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
    }

    private void updateDisplay() {
        if (mainActivity == null) return;

        boolean allDone = true;

        int dayIndex = todayDayIndex();
        String[][] todayQuests = QUEST_DATA[dayIndex];

        for (int i = 0; i < 3; i++) {
            boolean completed = mainActivity.isQuestCompleted(i);

            questTitles[i].setText(todayQuests[i][0]);

            if (completed) {
                questCards[i].setBackgroundResource(R.drawable.quest_card_completed_bg);
                questChecks[i].setVisibility(View.VISIBLE);
                questBars[i].setMax(1);
                questBars[i].setProgress(1);
                questTexts[i].setText("Erledigt");
                questTexts[i].setTextColor(0xFF7C4DFF);
                questTitles[i].setPaintFlags(
                        questTitles[i].getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                questDescs[i].setText("Abgeschlossen! ✓");
                questDescs[i].setTextColor(0xFF7C4DFF);
                completeButtons[i].setVisibility(View.GONE);
            } else {
                questCards[i].setBackgroundResource(R.drawable.quest_card_bg);
                questChecks[i].setVisibility(View.GONE);
                questBars[i].setMax(1);
                questBars[i].setProgress(0);
                questTexts[i].setText("Offen");
                questTexts[i].setTextColor(0xFF9E9E9E);
                questTitles[i].setPaintFlags(
                        questTitles[i].getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                questDescs[i].setText(todayQuests[i][1]);
                questDescs[i].setTextColor(0xFFBBBBBB);
                allDone = false;
                completeButtons[i].setVisibility(View.VISIBLE);
            }
        }

        boolean bonusCollected = mainActivity.isBonusCollected();
        if (bonusCollected) {
            bonusCard.setBackgroundResource(R.drawable.quest_card_completed_bg);
            bonusButton.setVisibility(View.GONE);
            bonusCollectedText.setVisibility(View.VISIBLE);
            bonusCollectedText.setText("Glücksrad gedreht ✓");
            bonusIcon.setImageResource(R.drawable.ic_check);
        } else if (allDone) {
            bonusCard.setBackgroundResource(R.drawable.quest_card_locked_bg);
            bonusButton.setVisibility(View.VISIBLE);
            bonusCollectedText.setVisibility(View.GONE);
            bonusButton.setEnabled(true);
            bonusButton.setBackgroundResource(R.drawable.buttonshape);
            bonusButton.getBackground().setTint(0xFFFFD600);
            bonusButton.setTextColor(0xFF000000);
            bonusButton.setText("🎰 Glücksrad drehen!");
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
