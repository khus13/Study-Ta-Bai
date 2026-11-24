package ph.studytabai.service;

import ph.studytabai.model.Teacher;
import ph.studytabai.storage.DataStore;

import java.util.HashMap;
import java.util.Map;

/** Teacher registration and login with a simple lockout against password guessing. */
public final class AuthService {
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 60_000;

    private static final class Attempts { int fails; long lockedUntil; }

    private final DataStore<Teacher> teachers;
    private final Map<String, Attempts> attempts = new HashMap<>();
    private String dummyHash;

    public AuthService(DataStore<Teacher> teachers) { this.teachers = teachers; }

    public synchronized Teacher register(String name, String email, String password) throws AuthException {
        String e = email == null ? "" : email.trim().toLowerCase();
        if (name == null || name.isBlank() || name.length() > 100) throw new AuthException("Enter a display name (max 100 characters).");
        if (!e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || e.length() > 190) throw new AuthException("Enter a valid email address.");
        if (password == null || password.length() < 8) throw new AuthException("Password must be at least 8 characters.");
        if (!teachers.find(t -> t.getEmail().equals(e)).isEmpty()) throw new AuthException("That email is already registered.");
        Teacher t = new Teacher(name, e, PasswordHasher.hash(password));
        teachers.save(t);
        return t;
    }

    public synchronized Teacher login(String email, String password) throws AuthException {
        String e = email == null ? "" : email.trim().toLowerCase();
        Attempts a = attempts.computeIfAbsent(e, k -> new Attempts());
        long now = System.currentTimeMillis();
        if (a.lockedUntil > now) throw new AuthException("Too many attempts. Try again in a minute.");
        Teacher t = teachers.find(x -> x.getEmail().equals(e)).stream().findFirst().orElse(null);
        // Do the same amount of hashing work whether or not the account exists.
        if (dummyHash == null) dummyHash = PasswordHasher.hash("dummy-password");
        boolean ok = PasswordHasher.verify(password == null ? "" : password, t != null ? t.getPasswordHash() : dummyHash);
        if (t != null && ok) {
            attempts.remove(e);
            return t;
        }
        if (++a.fails >= MAX_FAILS) { a.lockedUntil = now + LOCK_MS; a.fails = 0; }
        throw new AuthException("Incorrect email or password.");
    }
}
