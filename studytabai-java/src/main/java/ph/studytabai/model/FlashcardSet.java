package ph.studytabai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public final class FlashcardSet extends Activity {
    private static final long serialVersionUID = 1L;

    private final List<Flashcard> cards = new ArrayList<>();

    public FlashcardSet(String teacherId, String title) { super(teacherId, title); }

    public void addCard(Flashcard c) {
        if (c == null) throw new IllegalArgumentException("card is required");
        cards.add(c);
    }

    public void removeCard(int index) { cards.remove(index); }

    public void setCard(int index, Flashcard c) {
        if (c == null) throw new IllegalArgumentException("card is required");
        cards.set(index, c);
    }

    public List<Flashcard> getCards() { return Collections.unmodifiableList(cards); }

    @Override public String getKind() { return "flashcards"; }
    @Override public int itemCount() { return cards.size(); }
}
