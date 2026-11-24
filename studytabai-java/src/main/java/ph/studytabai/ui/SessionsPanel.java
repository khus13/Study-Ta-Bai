package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.*;
import ph.studytabai.net.GameServer;
import ph.studytabai.service.MusicPlayer;
import ph.studytabai.session.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.BindException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;

/** Host a live game or assign a self-paced activity, then control it. */
public final class SessionsPanel extends JPanel {
    private final AppContext ctx;
    private final Teacher teacher;
    private final CardLayout cards = new CardLayout();

    // setup form
    private final JComboBox<Activity> activity = new JComboBox<>();
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"Live game (teacher-paced)", "Self-paced homework"});
    private final JComboBox<ScoringMode> scoring = new JComboBox<>(ScoringMode.values());
    private final JCheckBox reactions = new JCheckBox("Fun reactions", true);
    private final JCheckBox music = new JCheckBox("Background music (plays on this computer)");
    private final JTextField due = new JTextField(12);
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(5050, 1024, 65535, 1));

    // console
    private final JPanel console = new JPanel(new BorderLayout(8, 8));
    private final DefaultTableModel players = new DefaultTableModel(new Object[]{"Nickname", "Points", "Answered", "Done"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable playersTable = new JTable(players);
    private final JLabel codeLabel = Ui.heading("");
    private final JTextArea howTo = Ui.textArea(3, 40);
    private final JLabel status = new JLabel(" ");
    private final JTextArea reactionFeed = Ui.textArea(6, 20);
    private final JButton startBtn = Ui.button("▶ Start", this::start);
    private final JButton pauseBtn = Ui.button("⏸ Pause", this::togglePause);
    private final JButton nextBtn = Ui.button("⏭ Next / skip", this::next);
    private final JButton endBtn = Ui.button("■ End", this::end);

    private GameServer server;
    private Session session;
    private final MusicPlayer musicPlayer = new MusicPlayer();
    private Timer tick;
    private Timer autosave;

    public SessionsPanel(AppContext ctx, Teacher teacher) {
        super();
        this.ctx = ctx;
        this.teacher = teacher;
        setLayout(cards);
        add(buildSetup(), "setup");
        buildConsole();
        add(console, "console");
        cards.show(this, "setup");
        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override public void ancestorAdded(javax.swing.event.AncestorEvent e) { if (server == null) loadActivities(); }
            @Override public void ancestorRemoved(javax.swing.event.AncestorEvent e) { }
            @Override public void ancestorMoved(javax.swing.event.AncestorEvent e) { }
        });
    }

    private JComponent buildSetup() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        form.add(Ui.heading("Run a session"));
        form.add(Ui.labeled("Activity", activity));
        form.add(Ui.labeled("Mode", mode));
        form.add(Ui.labeled("Scoring (game mode)", scoring));
        form.add(Ui.flow(reactions, music));
        form.add(Ui.labeled("Due date for homework (yyyy-mm-dd, optional)", due));
        form.add(Ui.labeled("Network port (students need this number)", port));
        form.add(Ui.flow(Ui.button("Host session", this::host)));
        JTextArea note = Ui.textArea(3, 40);
        note.setEditable(false);
        note.setText("Students join over the same Wi-Fi/network: they open Study Ta Bai, choose \"I'm a student\", and enter this "
                + "computer's address, the port, the join code, and a nickname. Flashcard sets can only be assigned as self-paced homework.");
        form.add(note);
        p.add(form, BorderLayout.NORTH);
        activity.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                Activity a = v instanceof Activity ac ? ac : null;
                return super.getListCellRendererComponent(l, a == null ? "" : (a instanceof Quiz ? "📝 " : "🃏 ") + a.getTitle(), i, s, f);
            }
        });
        loadActivities();
        return p;
    }

    private void loadActivities() {
        Object sel = activity.getSelectedItem();
        activity.removeAllItems();
        ctx.activities.find(a -> a.getTeacherId().equals(teacher.getId())).stream()
                .sorted(Comparator.comparing(Activity::getCreatedAt).reversed()).forEach(activity::addItem);
        if (sel != null) activity.setSelectedItem(sel);
    }

    private void buildConsole() {
        howTo.setEditable(false);
        playersTable.getAccessibleContext().setAccessibleName("Students in the session");
        reactionFeed.setEditable(false);
        JPanel top = new JPanel(new BorderLayout());
        top.add(codeLabel, BorderLayout.NORTH);
        top.add(Ui.scroll(howTo), BorderLayout.CENTER);
        top.add(Ui.flow(Ui.button("Copy join info", this::copyInfo)), BorderLayout.SOUTH);
        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(status, BorderLayout.NORTH);
        center.add(Ui.scroll(playersTable), BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(new JLabel("Reactions"), BorderLayout.NORTH);
        right.add(Ui.scroll(reactionFeed), BorderLayout.CENTER);
        right.setPreferredSize(new Dimension(240, 200));
        JPanel controls = Ui.flow(startBtn, pauseBtn, nextBtn, endBtn, Ui.button("♿ Accommodations…", this::accommodations),
                Ui.button("Close & back", this::closeConsole));
        console.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        console.add(top, BorderLayout.NORTH);
        console.add(center, BorderLayout.CENTER);
        console.add(right, BorderLayout.EAST);
        console.add(controls, BorderLayout.SOUTH);
    }

    // ---------- hosting ----------
    private void host() {
        if (server != null) return;
        Activity a = (Activity) activity.getSelectedItem();
        if (a == null) { Ui.error(this, "Create a quiz or flashcard set first."); return; }
        boolean live = mode.getSelectedIndex() == 0;
        try {
            Session s;
            if (live) {
                if (!(a instanceof Quiz q)) { Ui.error(this, "Live games need a quiz. Flashcard sets can be assigned as self-paced homework."); return; }
                s = new LiveSession(teacher.getId(), q);
            } else {
                s = new SelfPacedSession(teacher.getId(), a, parseDue());
            }
            s.setScoringMode((ScoringMode) scoring.getSelectedItem());
            s.setReactionsEnabled(reactions.isSelected());
            s.setMusicEnabled(music.isSelected());
            try {
                server = new GameServer(s, (Integer) port.getValue());
            } catch (BindException e) {
                server = new GameServer(s, 0);
                Ui.info(this, "That port was busy, so port " + server.getPort() + " is being used instead.");
            }
            session = s;
            ctx.sessions.save(s);
            server.setListener(new GameServer.Listener() {
                @Override public void changed() { SwingUtilities.invokeLater(SessionsPanel.this::refresh); }
                @Override public void reaction(String n, String e) {
                    SwingUtilities.invokeLater(() -> reactionFeed.append(n + " " + e + "\n"));
                }
            });
            showConsole();
        } catch (IllegalArgumentException | IllegalStateException e) {
            Ui.error(this, e.getMessage());
        } catch (java.io.IOException e) {
            Ui.error(this, "Could not open the network port: " + e.getMessage());
        }
    }

    private java.time.Instant parseDue() {
        String t = due.getText().trim();
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t).plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use the date format yyyy-mm-dd, for example 2026-12-15.");
        }
    }

    private void showConsole() {
        boolean live = session instanceof LiveSession;
        codeLabel.setText("Join code:  " + session.getJoinCode());
        howTo.setText("Students: open Study Ta Bai → \"I'm a student\" → enter\n   Host: " + GameServer.guessHostAddress()
                + "     Port: " + server.getPort() + "     Code: " + session.getJoinCode() + "\n"
                + (live ? "Press Start when everyone has joined." : "The activity is open now" + (session instanceof SelfPacedSession sp && sp.getDueAt() != null ? " until " + sp.getDueAt() : "") + "."));
        startBtn.setVisible(live);
        pauseBtn.setVisible(live);
        nextBtn.setVisible(live);
        reactionFeed.setText("");
        tick = new Timer(500, e -> updateStatus());
        tick.start();
        autosave = new Timer(15_000, e -> { if (session != null) ctx.sessions.save(session); });
        autosave.start();
        cards.show(this, "console");
        refresh();
    }

    private void refresh() {
        if (session == null) return;
        String selected = selectedNickname();
        players.setRowCount(0);
        for (LeaderboardEntry e : session.leaderboard()) {
            Participant p = session.getParticipants().stream().filter(x -> x.getNickname().equals(e.getNickname())).findFirst().orElse(null);
            players.addRow(new Object[]{e.getNickname(), e.getPoints(), e.getAnswered() + "/" + session.getQuestions().size(),
                    p != null && session.isCompleted(p.getId()) ? "✔" : ""});
        }
        if (selected != null)
            for (int i = 0; i < players.getRowCount(); i++)
                if (selected.equals(players.getValueAt(i, 0))) playersTable.setRowSelectionInterval(i, i);
        boolean live = session instanceof LiveSession;
        boolean ended = session.getStatus() == SessionStatus.ENDED;
        if (live) {
            SessionStatus st = session.getStatus();
            startBtn.setEnabled(st == SessionStatus.LOBBY);
            pauseBtn.setEnabled(st == SessionStatus.RUNNING || st == SessionStatus.PAUSED);
            pauseBtn.setText(st == SessionStatus.PAUSED ? "▶ Resume" : "⏸ Pause");
            nextBtn.setEnabled(st == SessionStatus.RUNNING || st == SessionStatus.PAUSED);
        }
        endBtn.setEnabled(!ended);
        if (ended && music.isSelected()) musicPlayer.stop();
        updateStatus();
    }

    private String selectedNickname() {
        int r = playersTable.getSelectedRow();
        return r < 0 ? null : String.valueOf(players.getValueAt(r, 0));
    }

    private void updateStatus() {
        if (session == null) return;
        String s = session.getStatus() + " · " + session.participantCount() + " student(s)";
        if (session instanceof LiveSession ls && ls.currentQuestion() != null) {
            long left = Math.max(0, ls.currentQuestion().getTimeLimitSec() * 1000L - ls.elapsedMs());
            s += " · Question " + (ls.getCurrentIndex() + 1) + "/" + ls.getQuestions().size() + " · " + (left / 1000) + "s left: " + ls.currentQuestion().getPrompt();
        }
        status.setText(s);
    }

    // ---------- controls ----------
    private void guard(Runnable r) {
        try { r.run(); } catch (IllegalStateException e) { Ui.error(this, e.getMessage()); }
    }

    private void start() {
        guard(() -> {
            server.startGame();
            if (session.isMusicEnabled()) musicPlayer.start();
        });
    }

    private void togglePause() {
        guard(() -> { if (session.getStatus() == SessionStatus.PAUSED) server.resumeGame(); else server.pauseGame(); });
    }

    private void next() { guard(server::nextQuestion); }

    private void end() {
        if (!Ui.confirm(this, "End this session for everyone?")) return;
        server.endGame();
        musicPlayer.stop();
        ctx.sessions.save(session);
    }

    private void copyInfo() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(
                "Host: " + GameServer.guessHostAddress() + "  Port: " + server.getPort() + "  Code: " + session.getJoinCode()), null);
    }

    private void accommodations() {
        JCheckBox tts = new JCheckBox("Read questions aloud automatically");
        JCheckBox contrast = new JCheckBox("High contrast");
        JSpinner time = new JSpinner(new SpinnerNumberModel(1.0, 1.0, 3.0, 0.25));
        JSpinner font = new JSpinner(new SpinnerNumberModel(1.0, 0.8, 2.0, 0.1));
        JComboBox<String> target = new JComboBox<>();
        target.addItem("Whole session");
        for (Participant p : session.getParticipants()) target.addItem(p.getNickname());
        String sel = selectedNickname();
        if (sel != null) target.setSelectedItem(sel);
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(Ui.labeled("Apply to", target));
        p.add(tts); p.add(contrast);
        p.add(Ui.labeled("Time multiplier (1.0 = normal, 2.0 = double time)", time));
        p.add(Ui.labeled("Font scale", font));
        if (JOptionPane.showConfirmDialog(this, p, "Accommodations", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        Accommodation a = new Accommodation();
        a.setTextToSpeech(tts.isSelected());
        a.setHighContrast(contrast.isSelected());
        a.setTimeMultiplier(((Number) time.getValue()).doubleValue());
        a.setFontScale(((Number) font.getValue()).doubleValue());
        if (target.getSelectedIndex() == 0) session.setSessionAccommodation(a);
        else {
            String nick = (String) target.getSelectedItem();
            session.getParticipants().stream().filter(x -> x.getNickname().equals(nick))
                    .forEach(x -> session.setParticipantAccommodation(x.getId(), a));
        }
        server.refresh();
    }

    private void closeConsole() {
        if (session != null && session.getStatus() != SessionStatus.ENDED && !Ui.confirm(this, "This will end the session. Continue?")) return;
        shutdown();
        loadActivities();
        cards.show(this, "setup");
    }

    /** Stops the server, music and timers, and saves the session. Safe to call repeatedly. */
    public void shutdown() {
        if (tick != null) tick.stop();
        if (autosave != null) autosave.stop();
        musicPlayer.stop();
        if (session != null) {
            session.end();
            ctx.sessions.save(session);
        }
        if (server != null) server.close();
        server = null;
        session = null;
    }
}
