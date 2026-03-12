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
            {"Morgendehnung", "Dehne dich 10 Minuten und mache 20 breite Liegestütze (breiter Griff = mehr Brust)"},
            {"Aktive Erholung", "30 normale + 20 Diamant-Liegestütze (Hände eng zusammen = Trizeps-Fokus)"},
            {"Sonntags-Champion", "100 Liegestütze nach Wahl — mische verschiedene Varianten für maximalen Effekt"}
        },
        { // Montag — Wochenstart
            {"Wochenstart-Power", "20 klassische Liegestütze als motivierter Tagesstart — saubere Technik!"},
            {"Montags-Schub", "20 normale + 20 breite + 10 Diamant-Liegestütze (kurze Pausen erlaubt)"},
            {"Montags-Held", "100 Liegestütze in Sets deiner Wahl — z.B. 10×10 oder 5×20"}
        },
        { // Dienstag — Oberkörper
            {"Schulter-Aktivierung", "20 Pike-Liegestütze: Gesäß hoch, Kopf zwischen die Arme — ideal für Schultern"},
            {"Brust & Trizeps", "25 normale + 25 Diamant-Liegestütze für Brust und Trizeps"},
            {"Dienstags-Krieger", "100 Liegestütze + je 3×30 Sek. Plank zwischen den Sets zur Core-Stärkung"}
        },
        { // Mittwoch — Core & Stabilität
            {"Slow-Push-up Challenge", "20 Liegestütze mit 3-Sek.-Halt unten — maximale Spannung, langsam & kontrolliert"},
            {"Wochenmitte-Push", "20 normale + 20 breite + 10 Archer-Liegestütze (eine Seite strecken, abwechselnd)"},
            {"Mittwochs-Meister", "100 Liegestütze gemischt + 10 Minuten Stretching danach (Schultern, Brust, Trizeps)"}
        },
        { // Donnerstag — Ausdauer
            {"Ausdauer-Basis", "20 Liegestütze in langsamem Tempo — 2 Sek. runter, kurz halten, 2 Sek. hoch"},
            {"Donnerstags-Kraft", "5 Sätze à 10 Liegestütze mit je 30 Sek. Pause — Fokus auf gleichmäßige Form"},
            {"Donnerstags-Titan", "100 Liegestütze beliebig verteilt + 5 Min. Dehnübungen für Schultern & Brust"}
        },
        { // Freitag — Power & Explosivität
            {"Feierabend-Starter", "20 Archer-Liegestütze — eine Seite strecken und abwechseln (Koordination & Kraft)"},
            {"Freitags-Feuer", "50 explosive Liegestütze — drücke dich so schnell wie möglich hoch (Sprengkraft)"},
            {"Wochenend-Countdown", "100 Liegestütze: breiter Griff → normaler Griff → Diamant (je ~33)"}
        },
        { // Samstag — Wochenend-Warrior
            {"Wochenend-Warmup", "5 Min. Ganzkörper-Dehnen, dann 20 Liegestütze nach Wahl — locker bleiben"},
            {"Samstags-Booster", "25 normale + 15 Decline-Liegestütze (Füße erhöht = obere Brust) + 10 Pike"},
            {"Wochenend-Warrior", "100 Liegestütze + Hampelmann 3×30 Sek. und 2×30 Sek. Plank zur Abkühlung"}
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
