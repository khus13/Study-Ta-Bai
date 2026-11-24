package ph.studytabai.model;

import java.io.Serializable;

public final class Flashcard implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String front;
    private final String back;

    public Flashcard(String front, String back) {
        if (front == null || front.isBlank() || back == null || back.isBlank())
            throw new IllegalArgumentException("Both sides of a flashcard are required");
        this.front = front.trim();
        this.back = back.trim();
    }

    public String getFront() { return front; }
    public String getBack() { return back; }
}
