package ph.studytabai.report;

import ph.studytabai.model.*;
import ph.studytabai.session.AnswerRecord;
import ph.studytabai.session.Session;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Builds class-level and student-level reports from a session's recorded answers. */
public final class ReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static String pct(int num, int den) { return den == 0 ? "-" : Math.round(100.0 * num / den) + "%"; }
    private static String secs(double ms) { return String.format(Locale.ROOT, "%.1f", ms / 1000.0); }
    private static List<String> row(Object... cells) {
        List<String> r = new ArrayList<>();
        for (Object c : cells) r.add(String.valueOf(c));
        return r;
    }

    public Reportable classReport(Session s) {
        List<Participant> ps = s.getParticipants();
        List<Question> qs = s.getQuestions();
        int n = ps.size(), completed = 0, totalPoints = 0, totalCorrect = 0;
        List<List<String>> studentRows = new ArrayList<>();

        for (Participant p : ps) {
            Map<String, AnswerRecord> recs = s.recordsFor(p.getId());
            int correct = (int) recs.values().stream().filter(AnswerRecord::isCorrect).count();
            double avgMs = recs.values().stream().mapToLong(AnswerRecord::getTimeMs).average().orElse(0);
            boolean done = s.isCompleted(p.getId());
            if (done) completed++;
            totalPoints += s.totalPoints(p.getId());
            totalCorrect += correct;
            studentRows.add(row(p.getNickname(), s.totalPoints(p.getId()), correct + "/" + qs.size(), pct(correct, qs.size()),
                    done ? "Yes" : "No", recs.isEmpty() ? "-" : secs(avgMs)));
        }
        studentRows.sort((a, b) -> Integer.compare(Integer.parseInt(b.get(1)), Integer.parseInt(a.get(1))));

        // question stats and standards mastery
        record QStat(Question q, int answered, int correct) {}
        List<QStat> stats = new ArrayList<>();
        Map<String, int[]> mastery = new TreeMap<>();
        for (Question q : qs) {
            int answered = 0, correct = 0;
            for (Participant p : ps) {
                AnswerRecord r = s.recordsFor(p.getId()).get(q.getId());
                if (r == null) continue;
                answered++;
                if (r.isCorrect()) correct++;
            }
            stats.add(new QStat(q, answered, correct));
            for (String code : q.getStandardCodes()) {
                int[] m = mastery.computeIfAbsent(code, k -> new int[2]);
                m[0] += correct;
                m[1] += answered;
            }
        }
        List<QStat> hardest = new ArrayList<>(stats.stream().filter(x -> x.answered() > 0).toList());
        hardest.sort(Comparator.comparingDouble(x -> (double) x.correct() / x.answered()));

        List<List<String>> hardRows = new ArrayList<>();
        for (QStat x : hardest.subList(0, Math.min(5, hardest.size())))
            hardRows.add(row(trim(x.q().getPrompt(), 60), x.q().getType(), x.answered(), pct(x.correct(), x.answered())));

        List<List<String>> stdRows = new ArrayList<>();
        for (Map.Entry<String, int[]> e : mastery.entrySet()) {
            int[] m = e.getValue();
            double p = m[1] == 0 ? -1 : 100.0 * m[0] / m[1];
            String level = m[1] == 0 ? "No data" : p >= 80 ? "Mastered" : p >= 50 ? "Developing" : "Needs support";
            stdRows.add(row(e.getKey(), m[1], pct(m[0], m[1]), level));
        }

        int possibleAnswers = n * qs.size();
        List<List<String>> summary = List.of(
                row("Session", s.getTitle()),
                row("Mode", s.getMode().equals("live") ? "Live" : "Self-paced"),
                row("Date", DATE.format(s.getCreatedAt())),
                row("Students", n),
                row("Average points", n == 0 ? "-" : Math.round((double) totalPoints / n)),
                row("Average % correct", pct(totalCorrect, possibleAnswers)),
                row("Completion rate", pct(completed, n)));

        List<Table> tables = new ArrayList<>();
        tables.add(new Table("Summary", List.of("Metric", "Value"), summary));
        tables.add(new Table("Hardest questions", List.of("Question", "Type", "Answered", "% correct"), hardRows));
        tables.add(new Table("Students", List.of("Nickname", "Points", "Correct", "% correct", "Completed", "Avg time (s)"), studentRows));
        if (!stdRows.isEmpty()) tables.add(new Table("Mastery by standard", List.of("Standard", "Answers", "% correct", "Level"), stdRows));
        return new SimpleReport("Class report: " + s.getTitle(), tables);
    }

    public Reportable studentReport(String nickname, Session focus, List<Session> allSessions) {
        Participant me = findByNickname(focus, nickname);
        if (me == null) throw new IllegalArgumentException("No student named " + nickname + " in this session.");
        List<Question> qs = focus.getQuestions();
        Map<String, AnswerRecord> recs = focus.recordsFor(me.getId());

        List<List<String>> qRows = new ArrayList<>();
        int i = 0;
        for (Question q : qs) {
            i++;
            AnswerRecord r = recs.get(q.getId());
            qRows.add(r == null ? row(i, trim(q.getPrompt(), 50), "No answer", "-", 0, "-")
                    : row(i, trim(q.getPrompt(), 50), r.isCorrect() ? "Correct" : "Wrong", trim(given(q, r), 30), r.getPoints(), secs(r.getTimeMs())));
        }

        List<Session> sorted = new ArrayList<>(allSessions);
        sorted.sort(Comparator.comparing(Session::getCreatedAt));
        List<List<String>> progress = new ArrayList<>();
        for (Session s : sorted) {
            Participant p = findByNickname(s, nickname);
            if (p == null || s.getQuestions().isEmpty()) continue;
            int correct = (int) s.recordsFor(p.getId()).values().stream().filter(AnswerRecord::isCorrect).count();
            progress.add(row(DATE.format(s.getCreatedAt()), s.getTitle(), correct + "/" + s.getQuestions().size(),
                    pct(correct, s.getQuestions().size()), s.totalPoints(p.getId())));
        }

        List<Table> tables = List.of(
                new Table("Per-question results", List.of("#", "Question", "Result", "Answer given", "Points", "Time (s)"), qRows),
                new Table("Progress over time", List.of("Date", "Session", "Correct", "% correct", "Points"), progress));
        return new SimpleReport("Student report: " + me.getNickname() + " - " + focus.getTitle(), tables);
    }

    private static Participant findByNickname(Session s, String nickname) {
        for (Participant p : s.getParticipants()) if (p.getNickname().equalsIgnoreCase(nickname)) return p;
        return null;
    }

    private static String given(Question q, AnswerRecord r) {
        if (q instanceof MultipleChoiceQuestion m) {
            int i = r.getOptionIndex();
            return i >= 0 && i < m.getOptions().size() ? m.getOptions().get(i).getText() : "-";
        }
        if (q instanceof TrueFalseQuestion) return r.getOptionIndex() == 0 ? "True" : "False";
        return r.getText() == null ? "-" : r.getText();
    }

    private static String trim(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }
}
