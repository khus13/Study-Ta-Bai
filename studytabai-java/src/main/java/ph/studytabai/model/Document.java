package ph.studytabai.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** An uploaded lesson. Only the extracted text is kept (the original file is not stored). */
public final class Document implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private final String teacherId;
    private final String originalName;
    private final Instant createdAt = Instant.now();
    private String title;
    private String text;

    public Document(String teacherId, String title, String originalName, String text) {
        if (teacherId == null || teacherId.isBlank()) throw new IllegalArgumentException("teacherId is required");
        this.teacherId = teacherId;
        this.originalName = originalName == null ? "" : originalName;
        setTitle(title);
        setText(text);
    }

    @Override public String getId() { return id; }
    public String getTeacherId() { return teacherId; }
    public String getOriginalName() { return originalName; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTitle() { return title; }
    public String getText() { return text; }

    public final void setTitle(String t) {
        if (t == null || t.isBlank() || t.length() > 200) throw new IllegalArgumentException("Title is required (max 200 characters)");
        this.title = t.trim();
    }

    public final void setText(String t) {
        if (t == null || t.isBlank()) throw new IllegalArgumentException("Text is required");
        this.text = t.length() > 400_000 ? t.substring(0, 400_000) : t;
    }

    @Override public String toString() { return title; }
}
