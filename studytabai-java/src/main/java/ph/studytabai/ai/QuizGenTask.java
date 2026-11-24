package ph.studytabai.ai;

import ph.studytabai.model.*;
import ph.studytabai.util.Json;

import java.util.*;

/** Lesson text -> list of validated questions (mcq / tf / short). Invalid items are skipped, not trusted. */
public final class QuizGenTask extends AiTask<List<Question>> {
    private final String lesson;
    private final int count;
    private final Set<String> types;
    private final String difficulty;

    public QuizGenTask(String lesson, int count, Set<String> types, String difficulty) {
        this.lesson = lesson;
        this.count = Math.max(1, Math.min(count, 30));
        this.types = types.isEmpty() ? Set.of("mcq", "tf", "short") : types;
        this.difficulty = difficulty;
    }

    @Override protected String promptName() { return "quiz"; }
    @Override protected String content() { return lesson; }

    @Override protected Map<String, String> variables() {
        return Map.of("count", String.valueOf(count), "difficulty", difficulty, "types", String.join(", ", types));
    }

    @Override
    protected List<Question> parse(String raw) throws AiException {
        List<Object> items;
        try {
            items = Json.asList(Json.extract(raw));
        } catch (IllegalArgumentException e) {
            throw new AiException("The AI reply was not valid. Please try again.", e);
        }
        List<Question> out = new ArrayList<>();
        for (Object o : items) {
            try {
                Question q = toQuestion(Json.asMap(o));
                if (q != null) out.add(q);
            } catch (RuntimeException ignored) {
                // skip malformed question
            }
        }
        if (out.isEmpty()) throw new AiException("The AI did not return any usable questions. Please try again.");
        return out.size() > count ? out.subList(0, count) : out;
    }

    private Question toQuestion(Map<String, Object> m) {
        String type = String.valueOf(m.get("type")).toLowerCase(Locale.ROOT);
        String prompt = Json.str(m.get("prompt"));
        Question q;
        switch (type) {
            case "mcq" -> {
                List<Option> opts = new ArrayList<>();
                List<Object> raw = Json.asList(m.get("options"));
                int ci = m.get("correctIndex") instanceof Number n ? n.intValue() : -1;
                for (int i = 0; i < raw.size() && i < 6; i++) opts.add(new Option(String.valueOf(raw.get(i)), i == ci));
                q = new MultipleChoiceQuestion(prompt, opts);
            }
            case "tf" -> q = new TrueFalseQuestion(prompt, Boolean.TRUE.equals(m.get("answer")));
            case "short" -> {
                List<String> acc = new ArrayList<>();
                for (Object a : Json.asList(m.get("accepted"))) acc.add(String.valueOf(a));
                q = new ShortAnswerQuestion(prompt, acc);
            }
            default -> { return null; }
        }
        String ex = Json.str(m.get("explanation"));
        if (ex != null && !ex.isBlank()) q.setExplanation(ex.trim());
        return q;
    }
}
