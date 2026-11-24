package ph.studytabai.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** A student's optional, local-only record of a finished activity ("save my progress"). */
public final class HistoryEntry implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private final String nickname;
    private final String title;
    private final int correct;
    private final int total;
    private final int points;
    private final Instant when = Instant.now();

    public HistoryEntry(String nickname, String title, int correct, int total, int points) {
        this.nickname = nickname;
        this.title = title;
        this.correct = correct;
        this.total = total;
        this.points = points;
    }

    @Override public String getId() { return id; }
    public String getNickname() { return nickname; }
    public String getTitle() { return title; }
    public int getCorrect() { return correct; }
    public int getTotal() { return total; }
    public int getPoints() { return points; }
    public Instant getWhen() { return when; }
}
