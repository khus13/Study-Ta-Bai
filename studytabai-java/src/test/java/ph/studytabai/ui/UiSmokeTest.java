package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.*;
import ph.studytabai.net.GameServer;
import ph.studytabai.service.MusicPlayer;
import ph.studytabai.session.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

/** Headless smoke test: builds every screen and drives the student screen against a real server. Run with -Djava.awt.headless=true */
public class UiSmokeTest {
    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS" : "FAIL") + " - " + name);
        if (!ok) failed++;
    }

    static <T> T edt(Callable<T> c) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return c.call();
        Object[] box = new Object[1];
        Exception[] err = new Exception[1];
        SwingUtilities.invokeAndWait(() -> { try { box[0] = c.call(); } catch (Exception e) { err[0] = e; } });
        if (err[0] != null) throw err[0];
        @SuppressWarnings("unchecked") T t = (T) box[0];
        return t;
    }

    static boolean waitFor(BooleanSupplier s) throws Exception {
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
            if (edt(() -> s.getAsBoolean())) return true;
            Thread.sleep(50);
        }
        return false;
    }

    static void walk(Component c, java.util.function.Consumer<Component> f) {
        f.accept(c);
        if (c instanceof Container k) for (Component x : k.getComponents()) walk(x, f);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("stb-ui");
        edt(() -> { Theme.install(); return null; });
        AppContext ctx = new AppContext(dir);
        Teacher t = ctx.auth.register("Ms. Cruz", "cruz@school.ph", "password123");
        ctx.documents.save(new Document(t.getId(), "Photosynthesis", "p.txt", "Plants make food using sunlight."));
        Quiz quiz = new Quiz(t.getId(), "Science Quiz");
        quiz.addQuestion(new MultipleChoiceQuestion("Gas plants absorb?", List.of(new Option("Oxygen", false), new Option("Carbon dioxide", true))));
        quiz.addQuestion(new ShortAnswerQuestion("Green pigment?", List.of("chlorophyll")));
        ctx.activities.save(quiz);
        FlashcardSet fs = new FlashcardSet(t.getId(), "Vocab");
        fs.addCard(new Flashcard("cell", "unit of life"));
        ctx.activities.save(fs);

        // ----- teacher screens build -----
        final boolean[] built = {false};
        TeacherHome home = edt(() -> new TeacherHome(null, ctx, t));
        edt(() -> { home.setSize(1000, 700); home.doLayout(); built[0] = true; return null; });
        check("teacher home with all tabs builds", built[0]);
        int[] tabs = {0};
        edt(() -> { walk(home, c -> { if (c instanceof JTabbedPane tp) tabs[0] = tp.getTabCount(); }); return null; });
        check("six teacher tabs present", tabs[0] == 6);

        // ----- theme switches -----
        edt(() -> { Theme.set(true, 1.5); Theme.set(false, 1.0); return null; });
        check("theme high contrast / font scale toggles", true);

        // ----- student screen against a real live server -----
        LiveSession live = new LiveSession(t.getId(), quiz);
        try (GameServer server = new GameServer(live, 0)) {
            StudentPanel sp = edt(() -> new StudentPanel(null, ctx));
            edt(() -> { sp.joinFor("127.0.0.1", server.getPort(), live.getJoinCode(), "Ana"); return null; });
            check("student panel joins and shows lobby", waitFor(sp::debugInPlay));
            server.startGame();
            check("question appears with options", waitFor(() -> sp.debugPrompt() != null && sp.debugOptionCount() == 2));
            edt(() -> { sp.debugClickOption(1); return null; });   // "Carbon dioxide"
            check("correct answer shown + score updated", waitFor(() -> sp.debugScore() > 0 && sp.debugFeedback().contains("Correct")));
            server.nextQuestion();
            check("next question is short-answer (no option buttons)", waitFor(() -> "Green pigment?".equals(sp.debugPrompt()) && sp.debugOptionCount() == 0));
            server.endGame();
            check("game over screen reached", waitFor(() -> sp.debugPrompt() == null));
            edt(() -> { sp.shutdown(); return null; });

            // wrong code
            StudentPanel sp2 = edt(() -> new StudentPanel(null, ctx));
            edt(() -> { sp2.joinFor("127.0.0.1", server.getPort(), "ZZZZZZ", "Bob"); return null; });
            check("wrong code shows a friendly error", waitFor(() -> sp2.debugJoinStatus().contains("Wrong join code") || sp2.debugJoinStatus().contains("ended")));
            edt(() -> { sp2.shutdown(); return null; });
        }

        // ----- self-paced quiz via the student panel -----
        SelfPacedSession hw = new SelfPacedSession(t.getId(), quiz, null);
        try (GameServer server = new GameServer(hw, 0)) {
            StudentPanel sp = edt(() -> new StudentPanel(null, ctx));
            edt(() -> { sp.joinFor("127.0.0.1", server.getPort(), hw.getJoinCode(), "Kim"); return null; });
            check("self-paced first question shown", waitFor(() -> sp.debugPrompt() != null && sp.debugInPlay()));
            edt(() -> { sp.debugClickOption(1); return null; });
            check("self-paced answer graded", waitFor(() -> sp.debugScore() > 0));
            edt(() -> { sp.shutdown(); return null; });
        }

        // ----- flashcards via the student panel -----
        SelfPacedSession cardsSession = new SelfPacedSession(t.getId(), fs, null);
        try (GameServer server = new GameServer(cardsSession, 0)) {
            StudentPanel sp = edt(() -> new StudentPanel(null, ctx));
            edt(() -> { sp.joinFor("127.0.0.1", server.getPort(), cardsSession.getJoinCode(), "Lee"); return null; });
            check("flashcard mode opens", waitFor(() -> sp.debugInPlay() && sp.debugStatus().equals("Flashcards")));
            edt(() -> { sp.shutdown(); return null; });
        }

        // ----- reports panel renders a saved session -----
        ctx.sessions.save(live);
        ReportsPanel rp = edt(() -> new ReportsPanel(ctx, t));
        Field lf = ReportsPanel.class.getDeclaredField("list");
        lf.setAccessible(true);
        @SuppressWarnings("unchecked") JList<Session> rl = (JList<Session>) lf.get(rp);
        int[] tables = {0};
        edt(() -> { rl.setSelectedIndex(0); walk(rp, c -> { if (c instanceof JTable) tables[0]++; }); return null; });
        check("reports panel shows tables for the selected session", tables[0] >= 3);

        // ----- sessions panel hosts + shuts down cleanly -----
        SessionsPanel sess = edt(() -> new SessionsPanel(ctx, t));
        edt(() -> { sess.shutdown(); return null; });
        check("sessions panel shutdown is safe", true);

        new MusicPlayer().stop();
        check("music player stop without start is safe", true);

        System.out.println(failed == 0 ? "\nALL UI SMOKE TESTS PASSED" : "\n" + failed + " TEST(S) FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
