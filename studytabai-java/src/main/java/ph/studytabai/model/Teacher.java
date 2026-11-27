package ph.studytabai.model;

/** Teachers have local accounts. Only a password HASH is ever stored (hashing arrives in Module 2). */
public final class Teacher extends User {
    private static final long serialVersionUID = 1L;

    private final String email;
    private String passwordHash;

    public Teacher(String displayName, String email, String passwordHash) {
        super(displayName);
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("A valid email is required");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("passwordHash is required");
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String h) {
        if (h == null || h.isBlank()) throw new IllegalArgumentException("passwordHash is required");
        this.passwordHash = h;
    }

    @Override public String getRole() { return "teacher"; }
}
