package ph.studytabai.ai;

import ph.studytabai.model.Flashcard;
import ph.studytabai.util.Json;

import java.util.*;

public final class FlashcardGenTask extends AiTask<List<Flashcard>> {
    private final String lesson;
    private final int count;

    public FlashcardGenTask(String lesson, int count) {
        this.lesson = lesson;
        this.count = Math.max(1, Math.min(count, 50));
    }

    @Override protected String promptName() { return "flashcards"; }
    @Override protected String content() { return lesson; }
    @Override protected Map<String, String> variables() { return Map.of("count", String.valueOf(count)); }

    @Override
    protected List<Flashcard> parse(String raw) throws AiException {
        List<Object> items;
        try {
            items = Json.asList(Json.extract(raw));
        } catch (IllegalArgumentException e) {
            throw new AiException("The AI reply was not valid. Please try again.", e);
        }
        List<Flashcard> out = new ArrayList<>();
        for (Object o : items) {
            Map<String, Object> m = Json.asMap(o);
            try {
                out.add(new Flashcard(Json.str(m.get("front")), Json.str(m.get("back"))));
            } catch (RuntimeException ignored) { }
        }
        if (out.isEmpty()) throw new AiException("The AI did not return any usable flashcards. Please try again.");
        return out.size() > count ? out.subList(0, count) : out;
    }
}
