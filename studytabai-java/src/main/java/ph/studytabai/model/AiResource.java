package ph.studytabai.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** A saved Wayground AI result (editable before it is used). */
public final class AiResource implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private final String teacherId;
    private final String kind;
    private final Instant createdAt = Instant.now();
    private String title;
    private String content;

    public AiResource(String teacherId, String kind, String title, String content) {
        if (teacherId == null || teacherId.isBlank()) throw new IllegalArgumentException("teacherId is required");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Content is required");
        this.teacherId = teacherId;
        this.kind = kind;
        this.title = title == null || title.isBlank() ? kind : title.trim();
        this.content = content;
    }

    @Override public String getId() { return id; }
    public String getTeacherId() { return teacherId; }
    public String getKind() { return kind; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public void setTitle(String t) { if (t != null && !t.isBlank()) this.title = t.trim(); }
    public void setContent(String c) {
        if (c == null || c.isBlank()) throw new IllegalArgumentException("Content is required");
        this.content = c;
    }
    @Override public String toString() { return "[" + kind + "] " + title; }
}
