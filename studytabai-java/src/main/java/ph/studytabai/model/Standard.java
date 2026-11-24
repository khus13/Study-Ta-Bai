package ph.studytabai.model;

import java.io.Serializable;

/** An academic standard (e.g. framework "DepEd MELC", code "S8LT-IIa-1"). */
public final class Standard implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String framework;
    private final String code;
    private final String description;

    public Standard(String framework, String code, String description) {
        if (framework == null || framework.isBlank() || code == null || code.isBlank())
            throw new IllegalArgumentException("Framework and code are required");
        this.framework = framework.trim();
        this.code = code.trim();
        this.description = description == null ? "" : description.trim();
    }

    @Override public String getId() { return framework + ":" + code; }
    public String getFramework() { return framework; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
}
