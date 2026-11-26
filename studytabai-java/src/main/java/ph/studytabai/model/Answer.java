package ph.studytabai.model;

import java.io.Serializable;

/** Immutable answer submitted by a student. Either an option index or free text. */
public final class Answer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int optionIndex;     // -1 when not an option answer
    private final String text;         // null when not a text answer
    private final long timeMs;

    private Answer(int optionIndex, String text, long timeMs) {
        if (timeMs < 0) throw new IllegalArgumentException("timeMs must be >= 0");
        this.optionIndex = optionIndex;
        this.text = text;
        this.timeMs = timeMs;
    }

    public static Answer ofOption(int optionIndex, long timeMs) {
        if (optionIndex < 0) throw new IllegalArgumentException("optionIndex must be >= 0");
        return new Answer(optionIndex, null, timeMs);
    }

    public static Answer ofText(String text, long timeMs) {
        if (text == null) throw new IllegalArgumentException("text is required");
        return new Answer(-1, text.length() > 500 ? text.substring(0, 500) : text, timeMs);
    }

    /** Copy of this answer with a server-trusted time. */
    public Answer withTimeMs(long ms) { return new Answer(optionIndex, text, ms); }

    public int getOptionIndex() { return optionIndex; }
    public String getText() { return text; }
    public long getTimeMs() { return timeMs; }
}
