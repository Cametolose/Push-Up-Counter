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
        { // Sonntag
            {"Beispielaufgabe (So. 1)", "Starte entspannt — mache 20 Liegestütze"},
            {"Beispielaufgabe (So. 2)", "Halte deinen Rhythmus — 50 Liegestütze"},
            {"Beispielaufgabe (So. 3)", "Beende das Wochenende stark — 100 Liegestütze"}
        },
        { // Montag
            {"Beispielaufgabe (Mo. 1)", "Starte motiviert in die Woche — 20 Liegestütze"},
            {"Beispielaufgabe (Mo. 2)", "Zeig der Woche, was du drauf hast — 50 Liegestütze"},
            {"Beispielaufgabe (Mo. 3)", "Setze gleich am Montag ein Zeichen — 100 Liegestütze"}
        },
        { // Dienstag
            {"Beispielaufgabe (Di. 1)", "Halte den Schwung vom Montag — 20 Liegestütze"},
            {"Beispielaufgabe (Di. 2)", "Steigere dich Schritt für Schritt — 50 Liegestütze"},
            {"Beispielaufgabe (Di. 3)", "Übertreffe dich selbst heute — 100 Liegestütze"}
        },
        { // Mittwoch
            {"Beispielaufgabe (Mi. 1)", "Du bist auf halbem Weg — 20 Liegestütze"},
            {"Beispielaufgabe (Mi. 2)", "Kämpfe dich durch die Wochenmitte — 50 Liegestütze"},
            {"Beispielaufgabe (Mi. 3)", "Maximale Kraft zur Wochenmitte — 100 Liegestütze"}
        },
        { // Donnerstag
            {"Beispielaufgabe (Do. 1)", "Das Wochenende rückt näher — 20 Liegestütze"},
            {"Beispielaufgabe (Do. 2)", "Volle Kraft an diesem Donnerstag — 50 Liegestütze"},
            {"Beispielaufgabe (Do. 3)", "Alles geben vor dem Wochenende — 100 Liegestütze"}
        },
        { // Freitag
            {"Beispielaufgabe (Fr. 1)", "Starte locker ins Wochenende — 20 Liegestütze"},
            {"Beispielaufgabe (Fr. 2)", "Belohne dich mit einem starken Training — 50 Liegestütze"},
            {"Beispielaufgabe (Fr. 3)", "Beende die Arbeitswoche mit Kraft — 100 Liegestütze"}
        },
        { // Samstag
            {"Beispielaufgabe (Sa. 1)", "Nutze das Wochenende für Training — 20 Liegestütze"},
            {"Beispielaufgabe (Sa. 2)", "Zeig am Wochenende deine Stärke — 50 Liegestütze"},
            {"Beispielaufgabe (Sa. 3)", "Setz einen neuen Wochenend-Rekord — 100 Liegestütze"}
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

        int daily = mainActivity.getDailyPushups();
        boolean allDone = true;

        int dayIndex = todayDayIndex();
        String[][] todayQuests = QUEST_DATA[dayIndex];

        for (int i = 0; i < 3; i++) {
            boolean completed = mainActivity.isQuestCompleted(i);
            int target   = QUEST_TARGETS[i];
            int progress = Math.min(daily, target);

            questTitles[i].setText(todayQuests[i][0]);

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
                completeButtons[i].setVisibility(View.GONE);
            } else {
                questCards[i].setBackgroundResource(R.drawable.quest_card_bg);
                questChecks[i].setVisibility(View.GONE);
                questTitles[i].setPaintFlags(
                        questTitles[i].getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                questDescs[i].setText(todayQuests[i][1]);
                questDescs[i].setTextColor(0xFFBBBBBB);
                allDone = false;
                // Show "Complete" button only when enough pushups have been done
                if (daily >= target) {
                    completeButtons[i].setVisibility(View.VISIBLE);
                } else {
                    completeButtons[i].setVisibility(View.GONE);
                }
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
