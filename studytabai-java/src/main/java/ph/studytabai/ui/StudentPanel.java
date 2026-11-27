package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.*;
import ph.studytabai.net.GameClient;
import ph.studytabai.net.Messages.*;
import ph.studytabai.service.MemeBank;
import ph.studytabai.service.TextToSpeech;
import ph.studytabai.session.LeaderboardEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Student side: join with host + code + nickname (no account), then play live, self-paced, or flashcards.
 * Accessibility: keyboard shortcuts (1-6, Enter, Space, arrows), read-aloud, high contrast and font size
 * (View menu or teacher-applied), extended timers (applied by the server).
 */
public final class StudentPanel extends JPanel {
    private static final String[] EMOJI = {"👏", "😂", "🔥", "🎉", "❤️", "😮"};

    private final MainFrame frame;
    private final AppContext ctx;
    private final CardLayout top = new CardLayout();
    private final TextToSpeech tts = new TextToSpeech();

    // join form
    private final JTextField host = new JTextField("127.0.0.1", 16);
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(5050, 1, 65535, 1));
    { port.setEditor(new JSpinner.NumberEditor(port, "#")); }
    private final JTextField code = new JTextField(8);
    private final JTextField nickname = new JTextField(16);
    private final JCheckBox saveProgress = new JCheckBox("Save my progress on this computer");
    private final JLabel joinStatus = new JLabel(" ");

    // play view
    private final JLabel titleLabel = Ui.heading(" ");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel timerLabel = new JLabel(" ");
    private final JLabel stateLabel = new JLabel(" ");
    private final CardLayout stageLayout = new CardLayout();
    private final JPanel stage = new JPanel(stageLayout);
    private final JTextArea waitText = Ui.textArea(3, 30);
    private final JTextArea promptArea = Ui.textArea(4, 40);
    private final JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 6, 6));
    private final JTextField shortAnswer = new JTextField(24);
    private final JButton shortSubmit = Ui.button("Submit Answer", this::submitShort);
    private final JButton readAloud = Ui.button("🔊 Read Aloud", this::readCurrent);
    private final JLabel feedback = new JLabel(" ");
    private final JTextArea feedbackDetail = Ui.textArea(3, 40);
    private final JButton nextBtn = Ui.button("Next ▶", this::nextSelfPaced);
    private final JTextArea endText = Ui.textArea(5, 40);
    private final JPanel boardHolder = new JPanel(new BorderLayout());
    private final JTextArea reactionFeed = Ui.textArea(5, 18);
    private final JLabel cardText = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel cardProgress = new JLabel(" ");
    private final JLabel imageLabel = new JLabel();
    private final JPanel shortRow = Ui.flow(shortAnswer, shortSubmit);

    // state
    private GameClient client;
    private volatile boolean closing;
    private String mode = "";
    private String sessionTitle = "";
    private String myNick = "";
    private List<QuestionView> spQuestions = List.of();
    private int spIndex;
    private List<Flashcard> flashcards = List.of();
    private int cardIndex;
    private boolean cardFront = true;
    private QuestionView current;
    private boolean answered;
    private boolean timedOut;
    private long deadlineMs;
    private long pausedRemaining = -1;
    private long startedNs;
    private int score, correctCount;
    private Accommodation acc = new Accommodation();
    private String lastAppliedAcc = "";
    private final Timer countdown = new Timer(250, e -> tickCountdown());
    private final List<JButton> optionButtons = new ArrayList<>();

    public StudentPanel(MainFrame frame, AppContext ctx) {
        super();
        this.frame = frame;
        this.ctx = ctx;
        setLayout(top);
        add(buildJoin(), "join");
        add(buildPlay(), "play");
        top.show(this, "join");
    }

    // ---------------------------------------------------------------- join
    private JComponent buildJoin() {
        ProvinceScene p = new ProvinceScene(false);
        p.setLayout(new GridBagLayout());
        WoodCard form = new WoodCard(new GridLayout(0, 1, 6, 6));
        form.setPreferredSize(new Dimension(540, 520));
        form.add(Ui.heading("Join a Session"));
        form.add(Ui.labeled("Teacher's computer address (host)", host));
        form.add(Ui.labeled("Invitation Code", port));
        form.add(Ui.labeled("Join code", code));
        form.add(Ui.labeled("Nickname (no real name needed)", nickname));
        saveProgress.setOpaque(false);
        form.add(saveProgress);
        form.add(joinStatus);
        JButton join = Ui.button("Join", this::joinFromForm);
        form.add(Ui.flow(join, Ui.button("My History", this::showHistory), Ui.button("Back", () -> frame.showRole())));
        nickname.addActionListener(e -> joinFromForm());
        code.addActionListener(e -> nickname.requestFocusInWindow());
        p.add(form);
        return p;
    }

    private void joinFromForm() {
        joinFor(host.getText().trim(), (Integer) port.getValue(), code.getText(), nickname.getText());
    }

    /** Also used by tests. */
    void joinFor(String h, int p, String joinCode, String nick) {
        if (h.isEmpty() || joinCode.isBlank() || nick.isBlank()) { setJoinStatus("Enter the host, join code and a nickname."); return; }
        closeClient();
        closing = false;
        myNick = nick.trim();
        setJoinStatus("Connecting…");
        GameClient c = new GameClient(h, p, new GameClient.Listener() {
            @Override public void joined(JoinResponse r) { SwingUtilities.invokeLater(() -> onJoined(r)); }
            @Override public void state(StateUpdate s) { SwingUtilities.invokeLater(() -> onState(s)); }
            @Override public void answerResult(AnswerResult r) { SwingUtilities.invokeLater(() -> onResult(r)); }
            @Override public void reaction(ReactionBroadcast r) { SwingUtilities.invokeLater(() -> onReaction(r)); }
            @Override public void error(String m) { SwingUtilities.invokeLater(() -> setJoinStatus(m)); }
            @Override public void disconnected() { SwingUtilities.invokeLater(() -> onDisconnected()); }
        });
        client = c;
        Ui.background(this, () -> { c.connect(joinCode.trim(), nick.trim(), null, null); return Boolean.TRUE; }, ok -> { },
                ex -> { client = null; setJoinStatus("Could not connect. Check the host, the invitation code and that your teacher has started the session."); });
    }

    private void setJoinStatus(String s) {
        joinStatus.setText(s);
        joinStatus.setForeground(Theme.feedbackColor(false));
    }

    // ---------------------------------------------------------------- play layout
    private JComponent buildPlay() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel header = new JPanel(new BorderLayout());
        header.add(titleLabel, BorderLayout.WEST);
        header.add(Ui.flow(stateLabel, timerLabel, scoreLabel, Ui.button("LEAVE", this::leave)), BorderLayout.EAST);
        timerLabel.getAccessibleContext().setAccessibleName("Time remaining");
        scoreLabel.getAccessibleContext().setAccessibleName("Your score");

        waitText.setEditable(false);
        JPanel wait = new JPanel(new GridBagLayout());
        wait.add(waitText);

        promptArea.setEditable(false);
        promptArea.setFont(promptArea.getFont().deriveFont(Font.BOLD, promptArea.getFont().getSize2D() * 1.3f));
        Ui.name(promptArea, "Question");
        feedbackDetail.setEditable(false);
        JPanel question = new JPanel(new BorderLayout(8, 8));
        JPanel qTop = new JPanel(new BorderLayout(4, 4));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.getAccessibleContext().setAccessibleName("Picture for the question");
        imageLabel.setVisible(false);
        qTop.add(Ui.scroll(promptArea), BorderLayout.CENTER);
        qTop.add(imageLabel, BorderLayout.NORTH);
        qTop.add(Ui.flow(readAloud), BorderLayout.SOUTH);
        JPanel answerArea = new JPanel(new BorderLayout(4, 4));
        answerArea.add(optionsPanel, BorderLayout.CENTER);
        answerArea.add(shortRow, BorderLayout.SOUTH);
        Ui.name(shortAnswer, "Type your answer");
        shortAnswer.addActionListener(e -> submitShort());
        JPanel qBottom = new JPanel(new BorderLayout(4, 4));
        qBottom.add(feedback, BorderLayout.NORTH);
        qBottom.add(feedbackDetail, BorderLayout.CENTER);
        qBottom.add(Ui.flow(nextBtn), BorderLayout.SOUTH);
        question.add(qTop, BorderLayout.NORTH);
        question.add(answerArea, BorderLayout.CENTER);
        question.add(qBottom, BorderLayout.SOUTH);
        installOptionShortcuts(question);

        endText.setEditable(false);
        JPanel ended = new JPanel(new BorderLayout(8, 8));
        ended.add(Ui.heading("🎉 All Done!"), BorderLayout.NORTH);
        ended.add(Ui.scroll(endText), BorderLayout.CENTER);

        cardText.setFont(cardText.getFont().deriveFont(Font.BOLD, cardText.getFont().getSize2D() * 2f));
        Ui.name(cardText, "Flashcard");
        JPanel cards = new JPanel(new BorderLayout(8, 8));
        cards.add(cardProgress, BorderLayout.NORTH);
        cards.add(cardText, BorderLayout.CENTER);
        cards.add(Ui.flow(Ui.button("◀ Previous", () -> moveCard(-1)), Ui.button("Flip (Space)", this::flipCard), Ui.button("Next ▶", () -> moveCard(1))), BorderLayout.SOUTH);
        installCardShortcuts(cards);

        stage.add(wait, "wait");
        stage.add(question, "question");
        stage.add(ended, "ended");
        stage.add(cards, "cards");

        JPanel side = new JPanel(new BorderLayout(4, 4));
        side.add(Ui.heading("Leaderboard"), BorderLayout.NORTH);
        side.add(boardHolder, BorderLayout.CENTER);
        JPanel react = new JPanel(new BorderLayout(4, 4));
        JPanel emojiRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        for (String e : EMOJI) {
            JButton b = Ui.button(e, () -> sendReaction(e));
            b.getAccessibleContext().setAccessibleName("Send reaction " + e);
            emojiRow.add(b);
        }
        reactionFeed.setEditable(false);
        react.add(emojiRow, BorderLayout.NORTH);
        react.add(Ui.scroll(reactionFeed), BorderLayout.CENTER);
        side.add(react, BorderLayout.SOUTH);
        side.setPreferredSize(new Dimension(300, 400));

        ProvinceScene strip = ProvinceScene.banner();
        strip.setPreferredSize(new Dimension(400, 84));
        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.add(strip, BorderLayout.NORTH);
        north.add(header, BorderLayout.CENTER);
        p.add(north, BorderLayout.NORTH);
        p.add(stage, BorderLayout.CENTER);
        p.add(side, BorderLayout.EAST);
        return p;
    }

    private void installOptionShortcuts(JComponent c) {
        for (int i = 1; i <= 6; i++) {
            final int idx = i - 1;
            c.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + idx, 0), "opt" + idx);
            c.getActionMap().put("opt" + idx, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (idx < optionButtons.size() && optionButtons.get(idx).isEnabled()) optionButtons.get(idx).doClick();
                }
            });
        }
    }

    private void installCardShortcuts(JComponent c) {
        bind(c, KeyEvent.VK_SPACE, "flip", this::flipCard);
        bind(c, KeyEvent.VK_RIGHT, "nextCard", () -> moveCard(1));
        bind(c, KeyEvent.VK_LEFT, "prevCard", () -> moveCard(-1));
    }

    private void bind(JComponent c, int key, String name, Runnable r) {
        c.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { r.run(); }
        });
    }

    // ---------------------------------------------------------------- server events
    private void onJoined(JoinResponse r) {
        if (!r.ok) { setJoinStatus(r.error); closeClient(); return; }
        joinStatus.setText(" ");
        mode = r.mode;
        sessionTitle = r.title;
        titleLabel.setText(r.title);
        score = 0; correctCount = 0;
        scoreLabel.setText("Score: 0");
        reactionFeed.setText("");
        applyAccommodation(r.accommodation);
        top.show(this, "play");
        if (r.cards != null) startFlashcards(r.cards);
        else if ("self_paced".equals(r.mode)) startSelfPaced(r.questions);
        else showWait("You're in, " + myNick + "! 🎉\nWaiting for the teacher to start the game…");
    }

    private void onState(StateUpdate s) {
        applyAccommodation(s.accommodation);
        updateBoard(s.leaderboard);
        if (!"live".equals(mode)) return;
        stateLabel.setText(s.status.equals("PAUSED") ? "⏸ Paused" : "");
        switch (s.status) {
            case "LOBBY" -> showWait("You're in, " + myNick + "! 🎉\n" + s.participantCount + " player(s) here. Waiting for the teacher…");
            case "ENDED" -> showEnded();
            default -> {
                if (s.current == null) return;
                if (current == null || !current.getId().equals(s.current.getId())) {
                    showQuestion(s.current, s.remainingMs);
                    if (s.answered) { answered = true; lockInputs(); feedback.setText("Answer received ✔"); feedbackDetail.setText("Waiting for the next question…"); }
                }
                if (s.status.equals("PAUSED")) { pausedRemaining = s.remainingMs; countdown.stop(); timerLabel.setText("⏸"); }
                else { pausedRemaining = -1; deadlineMs = System.currentTimeMillis() + s.remainingMs; if (!answered) countdown.start(); }
            }
        }
    }

    private void onResult(AnswerResult r) {
        if (!r.accepted) {
            feedback.setText("⚠ " + (r.error == null ? "Answer not accepted." : r.error));
            feedback.setForeground(Theme.feedbackColor(false));
            if (current != null && !answered) { answered = true; lockInputs(); }
            return;
        }
        showResult(r.correct, r.points, r.total, r.correctText, r.explanation);
    }

    private void showResult(boolean correct, int points, int total, String correctText, String explanation) {
        score = total;
        if (correct) correctCount++;
        scoreLabel.setText("Score: " + score);
        feedback.setText((correct ? "✅ Correct! +" + points : "❌ Not quite") + "   " + MemeBank.caption(correct));
        feedback.setForeground(Theme.feedbackColor(correct));
        feedbackDetail.setText("Answer: " + correctText + (explanation == null || explanation.isBlank() ? "" : "\n" + explanation));
        lockInputs();
        if ("self_paced".equals(mode)) {
            nextBtn.setText(spIndex + 1 >= spQuestions.size() ? "Finish ▶" : "Next ▶");
            nextBtn.setVisible(true);
        }
    }

    private void onReaction(ReactionBroadcast r) {
        reactionFeed.append(r.nickname + " " + r.emoji + "\n");
        reactionFeed.setCaretPosition(reactionFeed.getDocument().getLength());
    }

    private void onDisconnected() {
        if (closing) return;
        countdown.stop();
        stateLabel.setText("⚠ Connection lost");
        stateLabel.setForeground(Theme.feedbackColor(false));
        if (top != null && "play".equals(visibleCard())) showWait("The connection to the teacher was lost.\nUse Leave, then join again with the same code.");
        else setJoinStatus("Disconnected.");
    }

    private String visibleCard() {
        for (Component c : getComponents()) if (c.isVisible()) return c == getComponent(0) ? "join" : "play";
        return "join";
    }

    // ---------------------------------------------------------------- question flow
    private void showQuestion(QuestionView q, long remainingMs) {
        current = q;
        answered = false;
        timedOut = false;
        promptArea.setText(q.getPrompt());
        promptArea.setCaretPosition(0);
        showImage(q.getImage());
        feedback.setText(" ");
        feedbackDetail.setText("");
        nextBtn.setVisible(false);
        optionsPanel.removeAll();
        optionButtons.clear();
        boolean isShort = q.getOptions().isEmpty();
        shortRow.setVisible(isShort);
        shortAnswer.setText("");
        shortAnswer.setEnabled(true);
        shortSubmit.setEnabled(true);
        List<String> opts = q.getOptions();
        for (int i = 0; i < opts.size(); i++) {
            final int idx = i;
            JButton b = Ui.button((i + 1) + ".  " + opts.get(i), () -> submitOption(idx));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.getAccessibleContext().setAccessibleName("Option " + (i + 1) + ": " + opts.get(i));
            optionButtons.add(b);
            optionsPanel.add(b);
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
        deadlineMs = System.currentTimeMillis() + remainingMs;
        startedNs = System.nanoTime();
        stage.setVisible(true);
        stageLayout.show(stage, "question");
        countdown.restart();
        if (acc.isTextToSpeech()) readCurrent();
        (isShort ? shortAnswer : (optionButtons.isEmpty() ? promptArea : optionButtons.get(0))).requestFocusInWindow();
        tickCountdown();
    }

    private void showImage(byte[] bytes) {
        if (bytes == null) { imageLabel.setIcon(null); imageLabel.setVisible(false); return; }
        ImageIcon icon = new ImageIcon(bytes);
        if (icon.getIconHeight() > 240 && icon.getIconWidth() > 0) {
            int w = Math.max(1, icon.getIconWidth() * 240 / icon.getIconHeight());
            icon = new ImageIcon(icon.getImage().getScaledInstance(w, 240, Image.SCALE_SMOOTH));
        }
        imageLabel.setIcon(icon.getIconWidth() > 0 ? icon : null);
        imageLabel.setVisible(icon.getIconWidth() > 0);
    }

    private void submitOption(int idx) {
        if (answered || current == null) return;
        send(Answer.ofOption(idx, elapsedMs()));
    }

    private void submitShort() {
        if (answered || current == null || shortAnswer.getText().isBlank()) return;
        send(Answer.ofText(shortAnswer.getText(), elapsedMs()));
    }

    private long elapsedMs() { return (System.nanoTime() - startedNs) / 1_000_000; }

    private void send(Answer a) {
        try {
            client.submit(current.getId(), a);
            answered = true;
            lockInputs();
            feedback.setText("Sending…");
            feedback.setForeground(UIManager.getColor("Label.foreground"));
        } catch (IOException | NullPointerException e) {
            feedback.setText("⚠ Could not send your answer (connection lost).");
        }
    }

    private void lockInputs() {
        for (JButton b : optionButtons) b.setEnabled(false);
        shortAnswer.setEnabled(false);
        shortSubmit.setEnabled(false);
    }

    private void tickCountdown() {
        if (current == null || answered || pausedRemaining >= 0) { if (answered) countdown.stop(); return; }
        long left = deadlineMs - System.currentTimeMillis();
        if (left <= 0) {
            timerLabel.setText("⏱ Time's up!");
            countdown.stop();
            if (!answered) {
                timedOut = true;
                if ("self_paced".equals(mode)) {
                    answered = true;
                    lockInputs();
                    feedback.setText("⏱ Time's up for this question.");
                    nextBtn.setText(spIndex + 1 >= spQuestions.size() ? "Finish ▶" : "Next ▶");
                    nextBtn.setVisible(true);
                } else lockInputs();
            }
            return;
        }
        timerLabel.setText("⏱ " + (left + 999) / 1000 + "s");
    }

    private void readCurrent() {
        if (current == null) return;
        StringBuilder sb = new StringBuilder(current.getPrompt());
        for (int i = 0; i < current.getOptions().size(); i++) sb.append(". Option ").append(i + 1).append(". ").append(current.getOptions().get(i));
        if (!tts.speak(sb.toString())) feedbackDetail.setText("No speech engine was found on this computer (needs espeak on Linux; built in on Windows and macOS).");
    }

    // ---------------------------------------------------------------- self-paced
    private void startSelfPaced(List<QuestionView> qs) {
        spQuestions = qs == null ? List.of() : qs;
        spIndex = 0;
        updateBoard(List.of());
        showSelfPacedQuestion();
    }

    private void showSelfPacedQuestion() {
        if (spIndex >= spQuestions.size()) { showEnded(); return; }
        QuestionView q = spQuestions.get(spIndex);
        stateLabel.setText("Question " + (spIndex + 1) + " of " + spQuestions.size());
        long ms = (long) Math.ceil(q.getTimeLimitSec() * acc.getTimeMultiplier()) * 1000L;
        showQuestion(q, ms);
    }

    private void nextSelfPaced() {
        spIndex++;
        showSelfPacedQuestion();
    }

    // ---------------------------------------------------------------- flashcards
    private void startFlashcards(List<Flashcard> cards) {
        flashcards = cards;
        cardIndex = 0;
        cardFront = true;
        mode = "cards";
        stateLabel.setText("Flashcards");
        stageLayout.show(stage, "cards");
        showCard();
        updateBoard(List.of());
    }

    private void showCard() {
        if (flashcards.isEmpty()) return;
        Flashcard c = flashcards.get(cardIndex);
        cardText.setText("<html><div style='text-align:center;width:420px'>" + escape(cardFront ? c.getFront() : c.getBack()) + "</div></html>");
        cardProgress.setText("Card " + (cardIndex + 1) + " of " + flashcards.size() + (cardFront ? "  (front)" : "  (back)"));
        if (acc.isTextToSpeech()) tts.speak(cardFront ? c.getFront() : c.getBack());
    }

    private void flipCard() { if ("cards".equals(mode)) { cardFront = !cardFront; showCard(); } }

    private void moveCard(int d) {
        if (!"cards".equals(mode) || flashcards.isEmpty()) return;
        cardIndex = Math.floorMod(cardIndex + d, flashcards.size());
        cardFront = true;
        showCard();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    // ---------------------------------------------------------------- misc
    private void showWait(String text) {
        countdown.stop();
        current = null;
        timerLabel.setText(" ");
        waitText.setText(text);
        stageLayout.show(stage, "wait");
    }

    private void showEnded() {
        countdown.stop();
        current = null;
        timerLabel.setText(" ");
        int total = "live".equals(mode) ? -1 : spQuestions.size();
        StringBuilder sb = new StringBuilder("Final score: " + score + " points\nCorrect answers: " + correctCount + (total >= 0 ? " of " + total : "") + "\n");
        if (saveProgress.isSelected() && !sessionTitle.isEmpty()) {
            ctx.history.save(new HistoryEntry(myNick, sessionTitle, correctCount, Math.max(total, correctCount), score));
            sb.append("\nYour progress was saved on this computer (see \"My history\").");
        }
        endText.setText(sb.toString());
        stageLayout.show(stage, "ended");
    }

    private void updateBoard(List<LeaderboardEntry> lb) {
        List<List<String>> rows = new ArrayList<>();
        int i = 0;
        for (LeaderboardEntry e : lb) rows.add(List.of(String.valueOf(++i), e.getNickname(), String.valueOf(e.getPoints())));
        JTable t = Ui.table(List.of("#", "Player", "Pts"), rows);
        t.setAutoCreateRowSorter(false);
        t.getAccessibleContext().setAccessibleName("Leaderboard");
        boardHolder.removeAll();
        boardHolder.add(Ui.scroll(t));
        boardHolder.revalidate();
        boardHolder.repaint();
    }

    private void applyAccommodation(Accommodation a) {
        if (a == null) return;
        acc = a;
        String key = a.isHighContrast() + "|" + a.getFontScale();
        boolean nonDefault = a.isHighContrast() || a.getFontScale() != 1.0;
        if (nonDefault && !key.equals(lastAppliedAcc)) {
            lastAppliedAcc = key;
            Theme.set(a.isHighContrast(), a.getFontScale());
        }
    }

    private void sendReaction(String emoji) {
        try { if (client != null) client.react(emoji); } catch (IOException ignored) { }
    }

    private void showHistory() {
        List<List<String>> rows = new ArrayList<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        ctx.history.findAll().stream().sorted(java.util.Comparator.comparing(HistoryEntry::getWhen).reversed()).forEach(h ->
                rows.add(List.of(f.format(h.getWhen()), h.getNickname(), h.getTitle(), h.getCorrect() + "/" + h.getTotal(), String.valueOf(h.getPoints()))));
        if (rows.isEmpty()) { Ui.info(this, "No saved progress yet. Tick \"Save my progress on this computer\" before joining."); return; }
        JScrollPane sp = Ui.scroll(Ui.table(List.of("When", "Nickname", "Activity", "Correct", "Points"), rows));
        sp.setPreferredSize(new Dimension(640, 300));
        JOptionPane.showMessageDialog(this, sp, "My History", JOptionPane.PLAIN_MESSAGE);
    }

    private void leave() {
        closeClient();
        countdown.stop();
        tts.stop();
        current = null;
        mode = "";
        top.show(this, "join");
    }

    private void closeClient() {
        closing = true;
        if (client != null) { client.close(); client = null; }
    }

    public void shutdown() {
        closeClient();
        countdown.stop();
        tts.stop();
    }

    // ---- test hooks ----
    String debugStatus() { return stateLabel.getText(); }
    String debugPrompt() { return current == null ? null : current.getPrompt(); }
    int debugScore() { return score; }
    boolean debugImageShown() { return imageLabel.isVisible() && imageLabel.getIcon() != null; }
    int debugOptionCount() { return optionButtons.size(); }
    void debugClickOption(int i) { optionButtons.get(i).doClick(); }
    String debugFeedback() { return feedback.getText(); }
    boolean debugInPlay() { return "play".equals(visibleCard()); }
    String debugJoinStatus() { return joinStatus.getText(); }
}
