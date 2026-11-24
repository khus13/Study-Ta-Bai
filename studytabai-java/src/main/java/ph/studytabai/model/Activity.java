package ph.studytabai.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** ABSTRACTION: a teacher-owned learning activity. Quiz and FlashcardSet extend it. */
public abstract class Activity implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private final String teacherId;
    private final Instant createdAt = Instant.now();
    private String title;
    private String description;

    protected Activity(String teacherId, String title) {
        if (teacherId == null || teacherId.isBlank()) throw new IllegalArgumentException("teacherId is required");
        this.teacherId = teacherId;
        setTitle(title);
    }

    public abstract String getKind();
    public abstract int itemCount();

    @Override public String getId() { return id; }
    public String getTeacherId() { return teacherId; }
    public Instant getCreatedAt() { return createdAt; }

    public String getTitle() { return title; }
    public final void setTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (title.length() > 200) throw new IllegalArgumentException("Title too long (max 200)");
        this.title = title.trim();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
