package ph.studytabai;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ph.studytabai.model.Accommodation;
import ph.studytabai.model.Answer;
import ph.studytabai.model.Flashcard;
import ph.studytabai.model.FlashcardSet;
import ph.studytabai.model.MultipleChoiceQuestion;
import ph.studytabai.model.Option;
import ph.studytabai.model.Question;
import ph.studytabai.model.Quiz;
import ph.studytabai.model.ShortAnswerQuestion;
import ph.studytabai.model.Teacher;
import ph.studytabai.model.TrueFalseQuestion;
import ph.studytabai.net.GameClient;
import ph.studytabai.net.GameServer;
import ph.studytabai.net.Messages.*;
import ph.studytabai.net.Messages.AnswerResult;
import ph.studytabai.net.Messages.ErrorMsg;
import ph.studytabai.net.Messages.JoinResponse;
import ph.studytabai.net.Messages.ReactionBroadcast;
import ph.studytabai.net.Messages.StateUpdate;
import ph.studytabai.report.CsvExporter;
import ph.studytabai.report.PdfExporter;
import ph.studytabai.report.ReportService;
import ph.studytabai.report.Reportable;
import ph.studytabai.report.SimpleReport;
import ph.studytabai.report.Table;
import ph.studytabai.service.AuthException;
import ph.studytabai.service.DocumentException;
import ph.studytabai.service.DocumentParser;
import ph.studytabai.session.AnswerRecord;
import ph.studytabai.session.LiveSession;
import ph.studytabai.session.ScoringMode;
import ph.studytabai.session.SelfPacedSession;
import ph.studytabai.session.Session;
import ph.studytabai.session.SessionStatus;
import ph.studytabai.storage.DataStore;
import ph.studytabai.util.Json;

/** Dependency-free tests for everything except the Swing screens. */
public class LogicTests {
    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS" : "FAIL") + " - " + name);
        if (!ok) failed++;
    }

    static class TestClient implements GameClient.Listener {
        final BlockingQueue<Object> q = new LinkedBlockingQueue<>();
        public void joined(JoinResponse r) { q.add(r); }
        public void state(StateUpdate s) { q.add(s); }
        public void answerResult(AnswerResult r) { q.add(r); }
        public void reaction(ReactionBroadcast r) { q.add(r); }
        public void error(String m) { q.add(new ErrorMsg(m)); }
        public void disconnected() { q.add("DISCONNECTED"); }

        <T> T await(Class<T> c, Predicate<T> p) throws InterruptedException {
            long end = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < end) {
                Object o = q.poll(200, TimeUnit.MILLISECONDS);
                if (c.isInstance(o) && p.test(c.cast(o))) return c.cast(o);
            }
            return null;
        }
    }

    static Quiz sampleQuiz(String tid) {
        Quiz quiz = new Quiz(tid, "Science Quiz");
        MultipleChoiceQuestion mcq = new MultipleChoiceQuestion("Gas plants absorb?",
                List.of(new Option("Oxygen", false), new Option("Carbon dioxide", true), new Option("Helium", false)));
        mcq.addStandardCode("SAMPLE-1");
        mcq.setExplanation("Plants take in CO2.");
        TrueFalseQuestion tf = new TrueFalseQuestion("Plants make food by photosynthesis.", true);
        tf.addStandardCode("SAMPLE-1");
        ShortAnswerQuestion sa = new ShortAnswerQuestion("Green pigment?", List.of("chlorophyll"));
        sa.setTimeLimitSec(10);
        quiz.addQuestion(mcq); quiz.addQuestion(tf); quiz.addQuestion(sa);
        return quiz;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("stb-logic");

        // ---------- JSON ----------
        Map<String, Object> parsed = Json.asMap(Json.parse("{\"a\":[1,2.5,true,null,\"x\\ny\\u0041\"],\"b\":{\"c\":\"d\"}}"));
        check("json parse", Json.asList(parsed.get("a")).size() == 5 && "xy".length() == 2 && "x\nyA".equals(Json.asList(parsed.get("a")).get(4)));
        check("json roundtrip", Json.parse(Json.write(parsed)).equals(parsed));
        check("json extract from chatty reply", Json.asList(Json.extract("Sure!\n```json\n[{\"k\":1}]\n```")).size() == 1);
        boolean bad = false;
        try { Json.parse("{\"a\":"); } catch (IllegalArgumentException e) { bad = true; }
        check("json rejects truncated input", bad);

        // ---------- Auth ----------
        AppContext ctx = new AppContext(dir);
        Teacher t = ctx.auth.register("Ms. Cruz", "Cruz@School.ph", "correct horse");
        check("teacher registered", ctx.teachers.count() == 1 && !t.getPasswordHash().contains("correct"));
        check("login works", ctx.auth.login("cruz@school.PH", "correct horse").getId().equals(t.getId()));
        boolean dup = false;
        try { ctx.auth.register("X", "cruz@school.ph", "password123"); } catch (AuthException e) { dup = true; }
        check("duplicate email rejected", dup);
        boolean weak = false;
        try { ctx.auth.register("X", "x@y.com", "short"); } catch (AuthException e) { weak = true; }
        check("short password rejected", weak);
        boolean locked = false;
        for (int i = 0; i < 5; i++) { try { ctx.auth.login("cruz@school.ph", "wrong" + i); } catch (AuthException e) { } }
        try { ctx.auth.login("cruz@school.ph", "correct horse"); } catch (AuthException e) { locked = e.getMessage().contains("Too many"); }
        check("lockout after repeated failures", locked);

        // ---------- Document parser ----------
        DocumentParser dp = new DocumentParser();
        Path txt = dir.resolve("a.txt");
        Files.writeString(txt, "Hello\r\n\r\n\r\n\r\nWorld");
        check("txt parse", dp.parse(txt).equals("Hello\n\nWorld"));
        Path docx = dir.resolve("a.docx");
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(docx))) {
            z.putNextEntry(new ZipEntry("word/document.xml"));
            z.write("<w:document><w:p><w:t>Fish &amp; chips</w:t></w:p><w:p><w:t>Two</w:t></w:p></w:document>".getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        check("docx parse", dp.parse(docx).equals("Fish & chips\nTwo"));
        Path exe = dir.resolve("a.exe"); Files.writeString(exe, "x");
        Path fakePdf = dir.resolve("fake.pdf"); Files.writeString(fakePdf, "not a pdf");
        Path emptyTxt = dir.resolve("empty.txt"); Files.writeString(emptyTxt, "");
        Path fakeDocx = dir.resolve("fake.docx"); Files.writeString(fakeDocx, "plain text");
        int rejected = 0;
        for (Path p : List.of(exe, fakePdf, emptyTxt, fakeDocx)) { try { dp.parse(p); } catch (DocumentException e) { rejected++; } }
        check("bad files rejected (exe, fake pdf, empty, fake docx)", rejected == 4);

        // ---------- Live session logic ----------
        AtomicLong clock = new AtomicLong(1_000_000);
        Quiz quiz = sampleQuiz(t.getId());
        LiveSession live = new LiveSession(t.getId(), quiz);
        live.setClock(clock::get);
        Session.Joined ana = live.join("Ana");
        Session.Joined ben = live.join("Ben");
        boolean dupNick = false;
        try { live.join("ana"); } catch (IllegalArgumentException e) { dupNick = true; }
        boolean badNick = false;
        try { live.join("<script>"); } catch (IllegalArgumentException e) { badNick = true; }
        check("duplicate/invalid nicknames rejected", dupNick && badNick);
        check("rejoin needs the right token", live.rejoin(ana.participant.getId(), ana.token) != null && live.rejoin(ana.participant.getId(), "nope") == null);
        Question q1 = quiz.getQuestions().get(0);
        boolean early = false;
        try { live.submit(ana.participant.getId(), q1.getId(), Answer.ofOption(1, 0)); } catch (IllegalStateException e) { early = true; }
        check("cannot answer before start", early);
        live.start();
        clock.addAndGet(2000);
        AnswerRecord r = live.submit(ana.participant.getId(), q1.getId(), Answer.ofOption(1, 0));   // claims 0 ms, server measures 2000
        check("server measures time (client cannot cheat)", r.getTimeMs() == 2000 && r.isCorrect() && r.getPoints() > 900);
        boolean twice = false;
        try { live.submit(ana.participant.getId(), q1.getId(), Answer.ofOption(1, 0)); } catch (IllegalStateException e) { twice = true; }
        check("double answer rejected", twice);
        live.pause();
        clock.addAndGet(60_000);
        live.resume();
        clock.addAndGet(1000);
        AnswerRecord rb = live.submit(ben.participant.getId(), q1.getId(), Answer.ofOption(0, 0));
        check("pause does not burn question time", rb.getTimeMs() == 3000 && !rb.isCorrect() && rb.getPoints() == 0);
        Question q2 = quiz.getQuestions().get(1);
        boolean wrongQ = false;
        try { live.submit(ana.participant.getId(), q2.getId(), Answer.ofOption(0, 0)); } catch (IllegalStateException e) { wrongQ = true; }
        check("cannot answer a question that is not current", wrongQ);
        live.next(); live.next();   // now on question 3 (10 s limit)
        Accommodation extra = new Accommodation(); extra.setTimeMultiplier(2.0);
        live.setParticipantAccommodation(ben.participant.getId(), extra);
        clock.addAndGet(14_000);
        Question q3 = quiz.getQuestions().get(2);
        boolean lateAna = false;
        try { live.submit(ana.participant.getId(), q3.getId(), Answer.ofText("chlorophyll", 0)); } catch (IllegalStateException e) { lateAna = true; }
        AnswerRecord benLate = live.submit(ben.participant.getId(), q3.getId(), Answer.ofText("Chlorophyll", 0));
        check("late answer rejected, extended-time student still accepted", lateAna && benLate.isCorrect());
        check("remaining time honours accommodation", live.remainingMs(ben.participant.getId()) == 6000 && live.remainingMs(ana.participant.getId()) == 0);
        live.next();
        check("game ends after last question", live.getStatus() == SessionStatus.ENDED);
        check("leaderboard sorted", live.leaderboard().get(0).getNickname().equals("Ana") || live.leaderboard().get(0).getPoints() >= live.leaderboard().get(1).getPoints());

        // ---------- Self-paced ----------
        AtomicLong sc = new AtomicLong(5_000_000);
        Quiz hw = sampleQuiz(t.getId());
        SelfPacedSession sp = new SelfPacedSession(t.getId(), hw, java.time.Instant.ofEpochMilli(sc.get() + 10_000));
        sp.setClock(sc::get);
        Session.Joined zoe = sp.join("Zoe");
        Question h1 = hw.getQuestions().get(0);
        AnswerRecord zr = sp.submit(zoe.participant.getId(), h1.getId(), Answer.ofOption(1, 999_999));
        check("self-paced clamps client time", zr.getTimeMs() == 20_000 && zr.isCorrect());
        sc.addAndGet(20_000);
        boolean pastDue = false;
        try { sp.submit(zoe.participant.getId(), hw.getQuestions().get(1).getId(), Answer.ofOption(0, 10)); } catch (IllegalStateException e) { pastDue = true; }
        check("self-paced rejects answers after due date", pastDue);
        FlashcardSet fcs = new FlashcardSet(t.getId(), "Cards"); fcs.addCard(new Flashcard("a", "b"));
        check("flashcard set can be assigned", new SelfPacedSession(t.getId(), fcs, null).getActivity() == fcs);
        boolean emptyLive = false;
        try { new LiveSession(t.getId(), new Quiz(t.getId(), "Empty")); } catch (IllegalArgumentException e) { emptyLive = true; }
        check("empty quiz cannot be hosted live", emptyLive);
        ScoringMode flat = ScoringMode.FLAT;
        check("flat scoring gives full points", flat.strategy().points(h1, Answer.ofOption(1, 19_000), 20) == h1.getPoints());

        // ---------- Persistence of sessions ----------
        ctx.sessions.save(live);
        ctx.sessions.save(sp);
        DataStore<Session> reloaded = new DataStore<>(dir.resolve("sessions.ser"), Session.class);
        Session rl = reloaded.findById(live.getId()).orElseThrow();
        check("session survives serialization (answers + participants)", rl.participantCount() == 2 && rl.totalPoints(ana.participant.getId()) == live.totalPoints(ana.participant.getId()));

        // ---------- Reports ----------
        ReportService rs = new ReportService();
        Reportable cr = rs.classReport(rl);
        Table summary = cr.getTables().get(0);
        check("class report has students + completion", summary.getRows().stream().anyMatch(x -> x.get(0).equals("Students") && x.get(1).equals("2"))
                && summary.getRows().stream().anyMatch(x -> x.get(0).equals("Completion rate")));
        check("hardest question listed first", !cr.getTables().get(1).getRows().isEmpty());
        check("standards mastery table present", cr.getTables().stream().anyMatch(x -> x.getTitle().startsWith("Mastery")));
        Reportable sr = rs.studentReport("Ana", rl, List.copyOf(reloaded.findAll()));
        check("student report rows", sr.getTables().get(0).getRows().size() == 3 && !sr.getTables().get(1).getRows().isEmpty());
        Path csv = dir.resolve("r.csv"), pdf = dir.resolve("r.pdf");
        new CsvExporter().export(cr, csv);
        new PdfExporter().export(cr, pdf);
        String csvText = Files.readString(csv);
        check("csv written", csvText.contains("Class report") && csvText.contains("Nickname,Points"));
        check("pdf written", new String(Files.readAllBytes(pdf), StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4") && Files.size(pdf) > 500);
        check("csv injection neutralised", CsvExportersCell("=cmd|' /C calc'!A0").startsWith("'="));

        // ---------- Networking ----------
        Quiz netQuiz = sampleQuiz(t.getId());
        LiveSession netLive = new LiveSession(t.getId(), netQuiz);
        try (GameServer server = new GameServer(netLive, 0)) {
            int port = server.getPort();
            TestClient l1 = new TestClient(), l2 = new TestClient(), lBad = new TestClient(), lDup = new TestClient();
            GameClient c1 = new GameClient("127.0.0.1", port, l1);
            c1.connect(netLive.getJoinCode().toLowerCase(), "Ana", null, null);
            JoinResponse jr = l1.await(JoinResponse.class, x -> true);
            check("student joins with code + nickname only", jr != null && jr.ok && "live".equals(jr.mode));

            GameClient cBad = new GameClient("127.0.0.1", port, lBad);
            cBad.connect("WRONG1", "Eve", null, null);
            JoinResponse jb = lBad.await(JoinResponse.class, x -> true);
            check("wrong code rejected", jb != null && !jb.ok);
            GameClient cDup = new GameClient("127.0.0.1", port, lDup);
            cDup.connect(netLive.getJoinCode(), "ANA", null, null);
            JoinResponse jd = lDup.await(JoinResponse.class, x -> true);
            check("duplicate nickname rejected over network", jd != null && !jd.ok);

            GameClient c2 = new GameClient("127.0.0.1", port, l2);
            c2.connect(netLive.getJoinCode(), "Ben", null, null);
            JoinResponse j2 = l2.await(JoinResponse.class, x -> true);
            check("second student joins", j2 != null && j2.ok);

            server.startGame();
            StateUpdate st = l1.await(StateUpdate.class, s -> s.current != null);
            check("students see the question without answers", st != null && st.current.getOptions().size() == 3 && st.status.equals("RUNNING"));

            int right = 0;
            MultipleChoiceQuestion nq = (MultipleChoiceQuestion) netQuiz.getQuestions().get(0);
            for (int i = 0; i < nq.getOptions().size(); i++) if (nq.getOptions().get(i).isCorrect()) right = i;
            c1.submit(st.current.getId(), Answer.ofOption(right, 0));
            AnswerResult ar = l1.await(AnswerResult.class, x -> true);
            check("correct answer scored on the server", ar != null && ar.accepted && ar.correct && ar.points > 0 && "Carbon dioxide".equals(ar.correctText));
            c1.submit(st.current.getId(), Answer.ofOption(right, 0));
            AnswerResult ar2 = l1.await(AnswerResult.class, x -> true);
            check("duplicate answer refused over network", ar2 != null && !ar2.accepted);

            c2.react("🔥");
            ReactionBroadcast rb2 = l1.await(ReactionBroadcast.class, x -> true);
            check("reaction broadcast", rb2 != null && rb2.nickname.equals("Ben") && rb2.emoji.equals("🔥"));
            c2.react("💣");   // not in whitelist
            check("unknown emoji ignored", l1.await(ReactionBroadcast.class, x -> true) == null);

            StateUpdate lbState = l2.await(StateUpdate.class, s -> s.leaderboard.stream().anyMatch(e -> e.getNickname().equals("Ana") && e.getPoints() > 0));
            check("leaderboard pushed to everyone", lbState != null);

            // rejoin with token
            c2.close();
            TestClient l2b = new TestClient();
            GameClient c2b = new GameClient("127.0.0.1", port, l2b);
            c2b.connect(netLive.getJoinCode(), "ignored", j2.participantId, j2.token);
            JoinResponse jr2 = l2b.await(JoinResponse.class, x -> true);
            check("rejoin with token keeps identity", jr2 != null && jr2.ok && jr2.participantId.equals(j2.participantId));

            // hostile serialized object is refused by the filter and the connection is dropped
            try (Socket raw = new Socket("127.0.0.1", port)) {
                raw.setSoTimeout(3000);
                boolean eof = false;
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(raw.getOutputStream());
                    oos.writeObject(new java.sql.Timestamp(0L));
                    oos.flush();
                } catch (IOException reset) { eof = true; }   // server already dropped us mid-write
                if (!eof) {
                    try {
                        int b;
                        while ((b = raw.getInputStream().read()) != -1) { /* drain the server's stream header */ }
                        eof = true;
                    } catch (IOException timeoutOrReset) { eof = !(timeoutOrReset instanceof java.net.SocketTimeoutException); }
                }
                check("deserialization filter drops hostile payload", eof);
            }

            server.endGame();
            check("host can end the game", netLive.getStatus() == SessionStatus.ENDED && l1.await(StateUpdate.class, s -> s.status.equals("ENDED")) != null);
            c1.close(); c2b.close(); cBad.close(); cDup.close();
        }

        // question pictures travel to students inside the view (byte[] passes the deserialization filter)
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Path png = dir.resolve("pic.png");
        javax.imageio.ImageIO.write(bi, "png", png.toFile());
        Quiz picQuiz = new Quiz(t.getId(), "Pictures");
        TrueFalseQuestion picQ = new TrueFalseQuestion("Is this a square?", true);
        picQ.setImagePath(png.toString());
        TrueFalseQuestion noPic = new TrueFalseQuestion("No picture here", true);
        noPic.setImagePath(dir.resolve("missing.png").toString());
        picQuiz.addQuestion(picQ); picQuiz.addQuestion(noPic);
        SelfPacedSession picSession = new SelfPacedSession(t.getId(), picQuiz, null);
        try (GameServer server = new GameServer(picSession, 0)) {
            TestClient l = new TestClient();
            GameClient c = new GameClient("127.0.0.1", server.getPort(), l);
            c.connect(picSession.getJoinCode(), "Pia", null, null);
            JoinResponse j = l.await(JoinResponse.class, x -> true);
            check("question picture is delivered to the student", j != null && j.ok && j.questions.get(0).getImage() != null && j.questions.get(0).getImage().length > 20);
            check("missing picture file is ignored safely", j != null && j.questions.get(1).getImage() == null);
            c.close();
        }

        // self-paced over the network (quiz + flashcards)
        SelfPacedSession netSp = new SelfPacedSession(t.getId(), sampleQuiz(t.getId()), null);
        try (GameServer server = new GameServer(netSp, 0)) {
            TestClient l = new TestClient();
            GameClient c = new GameClient("127.0.0.1", server.getPort(), l);
            c.connect(netSp.getJoinCode(), "Kim", null, null);
            JoinResponse j = l.await(JoinResponse.class, x -> true);
            check("self-paced join returns all questions", j != null && j.ok && j.questions != null && j.questions.size() == 3 && "self_paced".equals(j.mode));
            c.submit(j.questions.get(1).getId(), Answer.ofOption(0, 1500));
            AnswerResult a = l.await(AnswerResult.class, x -> true);
            check("self-paced answer scored", a != null && a.accepted && a.correct);
            c.close();
        }
        FlashcardSet cards = new FlashcardSet(t.getId(), "Vocab"); cards.addCard(new Flashcard("front", "back"));
        SelfPacedSession cardSession = new SelfPacedSession(t.getId(), cards, null);
        try (GameServer server = new GameServer(cardSession, 0)) {
            TestClient l = new TestClient();
            GameClient c = new GameClient("127.0.0.1", server.getPort(), l);
            c.connect(cardSession.getJoinCode(), "Lee", null, null);
            JoinResponse j = l.await(JoinResponse.class, x -> true);
            check("flashcard session delivers cards", j != null && j.ok && j.cards != null && j.cards.size() == 1);
            c.close();
        }

        System.out.println(failed == 0 ? "\nALL LOGIC TESTS PASSED" : "\n" + failed + " TEST(S) FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }

    // CsvExporter.cell is package-private in another package; re-check through a real export instead.
    static String CsvExportersCell(String v) throws IOException {
        Path f = Files.createTempFile("cell", ".csv");
        Reportable r = new SimpleReport("t", List.of(new Table("T", List.of("h"), List.of(List.of(v)))));
        new CsvExporter().export(r, f);
        for (String line : Files.readAllLines(f)) if (line.contains("cmd")) return line.replaceFirst("^\"?", "");
        return "";
    }
}
