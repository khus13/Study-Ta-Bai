package ph.studytabai.model;

import java.io.Serializable;

/** Immutable answer choice. */
public final class Option implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;
    private final boolean correct;

    public Option(String text, boolean correct) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Option text is required");
        if (text.length() > 500) throw new IllegalArgumentException("Option text too long (max 500)");
        this.text = text.trim();
        this.correct = correct;
    }

    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
}
