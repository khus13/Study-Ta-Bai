package ph.studytabai;

import ph.studytabai.model.*;
import ph.studytabai.storage.DataStore;
import ph.studytabai.storage.SafeDeserializer;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Dependency-free tests. Run: java -cp target/classes:target/test-classes ph.studytabai.ModelTests */
public class ModelTests {
    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS" : "FAIL") + " - " + name);
        if (!ok) failed++;
    }

    static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    public static void main(String[] args) throws Exception {
        // Validation (encapsulation)
        check("MCQ rejects 1 option", throwsIAE(() -> new MultipleChoiceQuestion("Q?", List.of(new Option("a", true)))));
        check("MCQ rejects 2 correct", throwsIAE(() -> new MultipleChoiceQuestion("Q?", List.of(new Option("a", true), new Option("b", true)))));
        check("blank prompt rejected", throwsIAE(() -> new TrueFalseQuestion("  ", true)));
        check("negative points rejected", throwsIAE(() -> new TrueFalseQuestion("Q", true).setPoints(-1)));
        check("accommodation range", throwsIAE(() -> new Accommodation().setTimeMultiplier(5)));

        // Polymorphic grading
        MultipleChoiceQuestion mcq = new MultipleChoiceQuestion("Capital of France?",
                List.of(new Option("Paris", true), new Option("Rome", false)));
        TrueFalseQuestion tf = new TrueFalseQuestion("Sky is blue", true);
        ShortAnswerQuestion sa = new ShortAnswerQuestion("Pigment?", List.of("chlorophyll"));
        List<Question> qs = List.of(mcq, tf, sa);
        Answer[] right = { Answer.ofOption(0, 1000), Answer.ofOption(0, 1000), Answer.ofText("  Chlorophyll. ", 1000) };
        Answer[] wrong = { Answer.ofOption(1, 1000), Answer.ofOption(1, 1000), Answer.ofText("carotene", 1000) };
        boolean allRight = true, allWrong = true;
        for (int i = 0; i < qs.size(); i++) {
            allRight &= qs.get(i).isCorrect(right[i]);
            allWrong &= !qs.get(i).isCorrect(wrong[i]);
        }
        check("each type accepts correct answers", allRight);
        check("each type rejects wrong answers", allWrong);
        check("out-of-range option is wrong", !mcq.isCorrect(Answer.ofOption(9, 10)));

        // Scoring
        int fast = mcq.score(Answer.ofOption(0, 1000));
        int slow = mcq.score(Answer.ofOption(0, 19000));
        check("faster answer scores more", fast > slow && slow >= 500);
        check("wrong answer scores 0", mcq.score(Answer.ofOption(1, 100)) == 0);
        check("extended time raises score", mcq.score(Answer.ofOption(0, 20000), 40) > mcq.score(Answer.ofOption(0, 20000)));

        // Student view leaks nothing
        boolean leaks = false;
        for (java.lang.reflect.Field f : QuestionView.class.getDeclaredFields()) {
            String n = f.getName().toLowerCase();
            if (n.contains("correct") || n.contains("accepted")) leaks = true;
        }
        check("QuestionView has no answer fields", !leaks);
        check("SA view has no options", sa.toStudentView().getOptions().isEmpty());

        // Quiz ordering
        Quiz quiz = new Quiz("t1", "Mixed Quiz");
        qs.forEach(quiz::addQuestion);
        quiz.moveQuestion(0, 2);
        check("reorder works", quiz.getQuestions().get(2) == mcq);
        check("questions list is read-only", unmodifiable(quiz));

        // Serialization round trip
        Path dir = Files.createTempDirectory("stb");
        Path file = dir.resolve("activities.ser");
        DataStore<Activity> store = new DataStore<>(file, Activity.class);
        store.save(quiz);
        FlashcardSet fs = new FlashcardSet("t1", "Vocab");
        fs.addCard(new Flashcard("photo", "light"));
        store.save(fs);
        check("no leftover .tmp file", !Files.exists(dir.resolve("activities.ser.tmp")));

        DataStore<Activity> again = new DataStore<>(file, Activity.class);
        check("reload finds both activities", again.count() == 2);
        Quiz loaded = (Quiz) again.findById(quiz.getId()).orElseThrow();
        check("question count survives", loaded.itemCount() == 3);
        check("subclass types survive",
                loaded.getQuestions().get(0) instanceof TrueFalseQuestion
                && loaded.getQuestions().get(1) instanceof ShortAnswerQuestion
                && loaded.getQuestions().get(2) instanceof MultipleChoiceQuestion);
        check("grading still works after reload", loaded.getQuestions().get(1).isCorrect(Answer.ofText("CHLOROPHYLL", 5)));
        check("ids survive", loaded.getQuestions().get(2).getId().equals(mcq.getId()));
        check("delete persists", again.delete(fs.getId()) && new DataStore<>(file, Activity.class).count() == 1);

        // Teacher: hash stored, email normalized
        Teacher t = new Teacher("Ms. Cruz", "Cruz@School.PH", "hash123");
        check("email normalized", t.getEmail().equals("cruz@school.ph"));
        DataStore<Teacher> ts = new DataStore<>(dir.resolve("teachers.ser"), Teacher.class);
        ts.save(t);
        check("teacher round trip", new DataStore<>(dir.resolve("teachers.ser"), Teacher.class).findById(t.getId()).isPresent());

        // Deserialization filter blocks classes outside the allow-list
        Path evil = dir.resolve("evil.ser");
        try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(evil))) {
            o.writeObject(new java.sql.Timestamp(0L));
        }
        boolean blocked = false;
        try (InputStream in = Files.newInputStream(evil)) {
            SafeDeserializer.read(in);
        } catch (InvalidClassException e) {
            blocked = true;
        }
        check("filter blocks non-allow-listed class", blocked);

        System.out.println(failed == 0 ? "\nALL TESTS PASSED" : "\n" + failed + " TEST(S) FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }

    static boolean unmodifiable(Quiz q) {
        try { q.getQuestions().clear(); return false; } catch (UnsupportedOperationException e) { return true; }
    }
}
