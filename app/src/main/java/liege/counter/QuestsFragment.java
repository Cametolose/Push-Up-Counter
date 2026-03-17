package liege.counter;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestsFragment extends Fragment implements MainActivity.OnStateChangedListener {

    private static final int[] QUEST_TARGETS = {20, 50, 100};

    /**
     * Quest info: howTo and YouTube/guide link for each quest.
     * Index matches QUEST_DATA[dayIndex][questIndex].
     * Format: {howToText, guideUrl}
     */
    private static final String[][][] QUEST_INFO = {
        { // Sonntag
            {"Mache genau 1 Push-Up in voller Bewegungsbreite – nur zum Aufwärmen!", "https://youtu.be/IODxDxX7oi4"},
            {"Steh gerade, Füße schulterbreit. Dehne Nacken, Schultern und Rücken je 20 Sekunden.", "https://youtu.be/L_xrDAtykMI"},
            {"Mache einen großen Schritt nach vorne, Knie fast auf dem Boden, wieder hoch. Wechsel Seiten.", "https://youtu.be/wrwwXE_x-pQ"}
        },
        { // Montag
            {"Dehne alle großen Muskelgruppen: Brust, Schultern, Rücken, Hüfte – je mindestens 20 Sek.", "https://youtu.be/L_xrDAtykMI"},
            {"Hände weiter als schulterbreit, Körper gerade halten. Brust zur Boden, zurück drücken.", "https://youtu.be/IODxDxX7oi4"},
            {"Unterarme oder Hände auf dem Boden, Körper wie ein Brett, Bauch anspannen.", "https://youtu.be/pSHjTRCQxIw"}
        },
        { // Dienstag
            {"Steh aufrecht, Füße schulterbreit. Knie beugen, Oberschenkel parallel zum Boden, wieder hoch.", "https://youtu.be/aclHkVaku9U"},
            {"Dehne Oberkörper und Hüfte gründlich, halte jede Position mindestens 20–30 Sekunden.", "https://youtu.be/L_xrDAtykMI"},
            {"Hände bilden Diamant (Zeigefinger + Daumen zusammen). Push-Up ausführen – Trizeps arbeitet!", "https://youtu.be/J0DXGHoB31g"}
        },
        { // Mittwoch
            {"Hände flach an die Wand, leicht schräg stehen. Brust zur Wand, dann wegdrücken.", "https://youtu.be/IODxDxX7oi4"},
            {"Rücken flach an die Wand, Knie 90° beugen. Halten so lange wie möglich.", "https://youtu.be/y-wV4Venusw"},
            {"Dehne alle Muskelgruppen: Oberschenkel, Rücken, Schultern – je 30–60 Sek. halten.", "https://youtu.be/L_xrDAtykMI"}
        },
        { // Donnerstag
            {"Dehne Hüftbeuger, Oberschenkel und Schultern. Je Position 20–30 Sekunden halten.", "https://youtu.be/L_xrDAtykMI"},
            {"Knie auf dem Boden, Hände schulterbreit. Push-Up ausführen – ideal für Einsteiger.", "https://youtu.be/IODxDxX7oi4"},
            {"Auf dem Bauch liegen, Arme und Beine gleichzeitig anheben und halten.", "https://youtu.be/cc3ZpvwnwKo"}
        },
        { // Freitag
            {"Abwechselnd Sprünge mit Armkreisen – Ganzkörperaufwärmen. Locker bleiben!", "https://youtu.be/UpH7rm0cYbM"},
            {"Dehne intensiv Brust, Schultern, Hüfte und Oberschenkel, je 30 Sek.", "https://youtu.be/L_xrDAtykMI"},
            {"Füße auf einer Erhöhung (Stuhl/Treppe), Hände auf dem Boden. Push-Up ausführen.", "https://youtu.be/IODxDxX7oi4"}
        },
        { // Samstag
            {"Hände auf Kniehöhe auf eine Erhöhung (Stuhl/Treppe). Push-Up – erleichterte Variante.", "https://youtu.be/IODxDxX7oi4"},
            {"In Plank-Position, abwechselnd Knie schnell zur Brust ziehen – Cardio & Core!", "https://youtu.be/nmwgirgXLYM"},
            {"Intensives Dehnen aller Muskelgruppen, 30–60 Sek. je Position. Ruhig atmen.", "https://youtu.be/L_xrDAtykMI"}
        }
    };

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
    private final Button[]       timerButtons = new Button[3];
    private final Button[]       infoButtons  = new Button[3];

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
        int[] timerBtnIds= {R.id.questTimerButton1,      R.id.questTimerButton2,      R.id.questTimerButton3};
        int[] infoBtnIds = {R.id.questInfoButton1,       R.id.questInfoButton2,       R.id.questInfoButton3};

        for (int i = 0; i < 3; i++) {
            questCards[i]      = view.findViewById(cardIds[i]);
            questTitles[i]     = view.findViewById(titleIds[i]);
            questDescs[i]      = view.findViewById(descIds[i]);
            questBars[i]       = view.findViewById(barIds[i]);
            questTexts[i]      = view.findViewById(textIds[i]);
            questChecks[i]     = view.findViewById(checkIds[i]);
            completeButtons[i] = view.findViewById(btnIds[i]);
            timerButtons[i]    = view.findViewById(timerBtnIds[i]);
            infoButtons[i]     = view.findViewById(infoBtnIds[i]);

            final int index = i;
            completeButtons[i].setOnClickListener(v -> mainActivity.completeQuest(index));
            timerButtons[i].setOnClickListener(v -> showTimerDialog(index));
            infoButtons[i].setOnClickListener(v -> showInfoDialog(index));
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

    /** Parses timer seconds from a quest description like "3min Dehnen" or "60s Plank". */
    private static final Pattern MIN_PAT = Pattern.compile("(\\d+)min");
    private static final Pattern SEC_PAT = Pattern.compile("(\\d+)s\\b");

    private static int parseTimerSeconds(String description) {
        Matcher m = MIN_PAT.matcher(description);
        if (m.find()) {
            return Integer.parseInt(m.group(1)) * 60;
        }
        m = SEC_PAT.matcher(description);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    /** Formats seconds as "m:ss" or "ss s". */
    private static String formatTime(int totalSecs) {
        if (totalSecs >= 60) {
            return (totalSecs / 60) + ":" + String.format(java.util.Locale.US, "%02d", totalSecs % 60);
        }
        return totalSecs + "s";
    }

    private void showTimerDialog(int questIndex) {
        int dayIndex = todayDayIndex();
        String description = QUEST_DATA[dayIndex][questIndex][1];
        int totalSecs = parseTimerSeconds(description);
        if (totalSecs <= 0) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(56, 40, 56, 40);
        layout.setBackgroundResource(R.drawable.quest_card_bg);
        layout.setGravity(android.view.Gravity.CENTER);

        TextView titleTv = new TextView(requireContext());
        titleTv.setText("⏱ Timer");
        titleTv.setTextColor(0xFFFFD600);
        titleTv.setTextSize(22);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setGravity(android.view.Gravity.CENTER);
        layout.addView(titleTv);

        TextView subtitleTv = new TextView(requireContext());
        subtitleTv.setText(description);
        subtitleTv.setTextColor(0xFF9E9E9E);
        subtitleTv.setTextSize(14);
        subtitleTv.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams subtitleParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.setMargins(0, 10, 0, 28);
        layout.addView(subtitleTv, subtitleParams);

        TextView timerTv = new TextView(requireContext());
        timerTv.setText(formatTime(totalSecs));
        timerTv.setTextColor(0xFFFFFFFF);
        timerTv.setTextSize(56);
        timerTv.setTypeface(null, android.graphics.Typeface.BOLD);
        timerTv.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams timerParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        timerParams.setMargins(0, 0, 0, 28);
        layout.addView(timerTv, timerParams);

        Button startBtn = new Button(requireContext());
        startBtn.setText("▶ Start Timer");
        startBtn.setTextColor(0xFF000000);
        startBtn.setTextSize(15);
        android.widget.LinearLayout.LayoutParams btnParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 130);
        btnParams.setMargins(0, 0, 0, 0);
        startBtn.setBackgroundResource(R.drawable.button_increment_bg);
        startBtn.setLayoutParams(btnParams);
        layout.addView(startBtn);

        builder.setView(layout);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        // Ensure the dialog is wide enough for the text to display fully
        if (dialog.getWindow() != null) {
            int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
            dialog.getWindow().setLayout(
                    (int)(screenWidth * 0.9f),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        final int[] remaining = {totalSecs};
        final CountDownTimer[] activeTimer = {null};
        final boolean[] running = {false};

        startBtn.setOnClickListener(v -> {
            if (running[0]) return;
            running[0] = true;
            startBtn.setEnabled(false);
            startBtn.setText("Bereit machen…");

            // 3-second countdown before the real timer
            new CountDownTimer(3000, 1000) {
                int count = 3;
                @Override
                public void onTick(long ms) {
                    timerTv.setText(String.valueOf(count--));
                    timerTv.setTextColor(0xFFFF8800);
                }
                @Override
                public void onFinish() {
                    timerTv.setTextColor(0xFF4CAF50);
                    startBtn.setText("Läuft…");
                    activeTimer[0] = new CountDownTimer((long) remaining[0] * 1000, 1000) {
                        @Override
                        public void onTick(long ms) {
                            remaining[0] = (int) (ms / 1000) + 1;
                            timerTv.setText(formatTime(remaining[0]));
                        }
                        @Override
                        public void onFinish() {
                            timerTv.setText("✓ Fertig!");
                            timerTv.setTextColor(0xFF4CAF50);
                            startBtn.setText("Schließen");
                            startBtn.setEnabled(true);
                            startBtn.setOnClickListener(sv -> dialog.dismiss());
                        }
                    }.start();
                }
            }.start();
        });

        dialog.setOnDismissListener(d -> {
            if (activeTimer[0] != null) activeTimer[0].cancel();
        });
    }

    private void showInfoDialog(int questIndex) {
        int dayIndex = todayDayIndex();
        String questTitle = QUEST_DATA[dayIndex][questIndex][0];
        String description = QUEST_DATA[dayIndex][questIndex][1];
        String[] info = QUEST_INFO[dayIndex][questIndex];
        String howTo = info[0];
        String url   = info[1];

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 40, 48, 32);
        layout.setBackgroundResource(R.drawable.quest_card_bg);

        TextView titleTv = new TextView(requireContext());
        titleTv.setText("ℹ " + questTitle);
        titleTv.setTextColor(0xFF00E5FF);
        titleTv.setTextSize(18);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams titleParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, 0, 0, 8);
        layout.addView(titleTv, titleParams);

        TextView descTv = new TextView(requireContext());
        descTv.setText("📌 " + description);
        descTv.setTextColor(0xFF9E9E9E);
        descTv.setTextSize(13);
        android.widget.LinearLayout.LayoutParams descParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, 0, 0, 16);
        layout.addView(descTv, descParams);

        TextView howToTv = new TextView(requireContext());
        howToTv.setText("💡 So geht's:\n" + howTo);
        howToTv.setTextColor(0xFFBBBBBB);
        howToTv.setTextSize(14);
        howToTv.setLineSpacing(4, 1.2f);
        android.widget.LinearLayout.LayoutParams howToParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        howToParams.setMargins(0, 0, 0, 20);
        layout.addView(howToTv, howToParams);

        TextView linkTv = new TextView(requireContext());
        linkTv.setText("🎥 Video-Anleitung ansehen");
        linkTv.setTextColor(0xFF7C4DFF);
        linkTv.setTextSize(14);
        linkTv.setTypeface(null, android.graphics.Typeface.BOLD);
        linkTv.setPaintFlags(linkTv.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        linkTv.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                android.util.Log.w("QuestsFragment", "Konnte Video-Link nicht öffnen", e);
                android.widget.Toast.makeText(requireContext(),
                        "Link konnte nicht geöffnet werden", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        android.widget.LinearLayout.LayoutParams linkParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        linkParams.setMargins(0, 0, 0, 20);
        layout.addView(linkTv, linkParams);

        Button closeBtn = new Button(requireContext());
        closeBtn.setText("Schließen");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setTextSize(14);
        closeBtn.setBackgroundResource(R.drawable.button_increment_bg);
        android.widget.LinearLayout.LayoutParams closeBtnParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 120);
        layout.addView(closeBtn, closeBtnParams);

        builder.setView(layout);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateDisplay() {
        if (mainActivity == null) return;

        boolean allDone = true;

        int dayIndex = todayDayIndex();
        String[][] todayQuests = QUEST_DATA[dayIndex];

        for (int i = 0; i < 3; i++) {
            boolean completed = mainActivity.isQuestCompleted(i);

            questTitles[i].setText(todayQuests[i][0]);

            // Show/hide timer button based on whether quest has a time component
            int timerSecs = parseTimerSeconds(todayQuests[i][1]);
            timerButtons[i].setVisibility(timerSecs > 0 && !completed ? View.VISIBLE : View.GONE);
            infoButtons[i].setVisibility(completed ? View.GONE : View.VISIBLE);

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
