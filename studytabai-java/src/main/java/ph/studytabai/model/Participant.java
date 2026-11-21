package ph.studytabai.model;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/** A student in one session: nickname only, no personal data. The rejoin token is stored hashed. */
public final class Participant implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private final String nickname;
    private final String tokenHash;
    private final Instant joinedAt = Instant.now();
    private Accommodation accommodation;   // null = use the session default

    public Participant(String nickname, String token) {
        this.nickname = nickname;
        this.tokenHash = hash(token);
    }

    private static String hash(String t) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(t.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verifyToken(String token) {
        return token != null && MessageDigest.isEqual(tokenHash.getBytes(StandardCharsets.UTF_8), hash(token).getBytes(StandardCharsets.UTF_8));
    }

    @Override public String getId() { return id; }
    public String getNickname() { return nickname; }
    public Instant getJoinedAt() { return joinedAt; }
    public Accommodation getAccommodation() { return accommodation; }
    public void setAccommodation(Accommodation a) { this.accommodation = a; }
}
