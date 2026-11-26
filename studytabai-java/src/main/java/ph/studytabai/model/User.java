package ph.studytabai.model;

import java.io.Serializable;
import java.util.UUID;

/** ABSTRACTION + INHERITANCE: common identity for everyone who uses the app. */
public abstract class User implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private String displayName;

    protected User(String displayName) { setDisplayName(displayName); }

    public abstract String getRole();

    @Override public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public final void setDisplayName(String name) {
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Display name is required (max 100 characters)");
        this.displayName = name.trim();
    }
}
