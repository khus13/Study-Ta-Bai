package ph.studytabai.session;

import ph.studytabai.model.*;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.function.LongSupplier;

/**
 * ABSTRACTION: shared rules for any session (joining, grading, leaderboard, accommodations).
 * LiveSession and SelfPacedSession (INHERITANCE) decide WHEN answers are accepted and how time is measured.
 */
public abstract class Session implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Result of joining: the participant plus the one-time secret token the client keeps for rejoining. */
    public static final class Joined {
        public final Participant participant;
        public final String token;
        Joined(Participant p, String t) { participant = p; token = t; }
    }

    private final String id = UUID.randomUUID().toString();
    private final String teacherId;
    private final Activity activity;
    private final String joinCode = newCode();
    private final Instant createdAt = Instant.now();
    private SessionStatus status = SessionStatus.LOBBY;
    private Instant endedAt;
    private ScoringMode scoringMode = ScoringMode.SPEED;
    private boolean reactionsEnabled = true;
    private boolean musicEnabled;
    private Accommodation sessionAccommodation = new Accommodation();
    private final Map<String, Participant> participants = new LinkedHashMap<>();
    private final Map<String, Map<String, AnswerRecord>> records = new HashMap<>();
    private transient LongSupplier clock;

    protected Session(String teacherId, Activity activity) {
        if (teacherId == null || activity == null) throw new IllegalArgumentException("teacher and activity are required");
        this.teacherId = teacherId;
        this.activity = activity;
    }

    // ---- differences between modes ----
    public abstract String getMode();
    protected abstract boolean canAnswer(Question q, int effectiveLimitSec);
    protected abstract long effectiveTimeMs(Question q, Answer a, int effectiveLimitSec);

    // ---- time ----
    protected final long now() { return (clock != null ? clock : (clock = System::currentTimeMillis)).getAsLong(); }
    public final void setClock(LongSupplier c) { this.clock = c; }

    // ---- state ----
    @Override public String getId() { return id; }
    public String getTeacherId() { return teacherId; }
    public Activity getActivity() { return activity; }
    public String getTitle() { return activity.getTitle(); }
    public String getJoinCode() { return joinCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEndedAt() { return endedAt; }
    public synchronized SessionStatus getStatus() { return status; }
    protected synchronized void setStatus(SessionStatus s) { this.status = s; }

    public synchronized void end() {
        if (status == SessionStatus.ENDED) return;
        status = SessionStatus.ENDED;
        endedAt = Instant.now();
    }

    public ScoringMode getScoringMode() { return scoringMode; }
    public void setScoringMode(ScoringMode m) { this.scoringMode = m == null ? ScoringMode.SPEED : m; }
    public boolean isReactionsEnabled() { return reactionsEnabled; }
    public void setReactionsEnabled(boolean v) { this.reactionsEnabled = v; }
    public boolean isMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(boolean v) { this.musicEnabled = v; }

    public List<Question> getQuestions() {
        return activity instanceof Quiz q ? q.getQuestions() : List.of();
    }

    public Question findQuestion(String questionId) {
        for (Question q : getQuestions()) if (q.getId().equals(questionId)) return q;
        return null;
    }

    // ---- participants ----
    public synchronized Joined join(String nickname) {
        if (status == SessionStatus.ENDED) throw new IllegalStateException("This session has ended.");
        String nick = nickname == null ? "" : nickname.trim().replaceAll("\\s+", " ");
        if (!nick.matches("^[\\p{L}\\p{N} _\\-]{1,20}$"))
            throw new IllegalArgumentException("Nickname must be 1-20 letters, numbers, spaces, - or _.");
        for (Participant p : participants.values())
            if (p.getNickname().equalsIgnoreCase(nick)) throw new IllegalArgumentException("That nickname is taken. Pick another.");
        byte[] raw = new byte[16];
        RANDOM.nextBytes(raw);
        String token = HexFormat.of().formatHex(raw);
        Participant p = new Participant(nick, token);
        participants.put(p.getId(), p);
        return new Joined(p, token);
    }

    public synchronized Participant rejoin(String participantId, String token) {
        Participant p = participants.get(participantId);
        return p != null && p.verifyToken(token) ? p : null;
    }

    public synchronized List<Participant> getParticipants() { return List.copyOf(participants.values()); }
    public synchronized Participant getParticipant(String pid) { return participants.get(pid); }
    public synchronized int participantCount() { return participants.size(); }

    // ---- accommodations ----
    public synchronized Accommodation getSessionAccommodation() { return sessionAccommodation; }
    public synchronized void setSessionAccommodation(Accommodation a) { this.sessionAccommodation = a == null ? new Accommodation() : a; }

    public synchronized void setParticipantAccommodation(String pid, Accommodation a) {
        Participant p = participants.get(pid);
        if (p != null) p.setAccommodation(a);
    }

    public synchronized Accommodation accommodationFor(String pid) {
        Participant p = participants.get(pid);
        return p != null && p.getAccommodation() != null ? p.getAccommodation() : sessionAccommodation;
    }

    // ---- answering ----
    public synchronized AnswerRecord submit(String participantId, String questionId, Answer raw) {
        if (!participants.containsKey(participantId)) throw new IllegalArgumentException("Unknown participant.");
        Question q = findQuestion(questionId);
        if (q == null) throw new IllegalArgumentException("Unknown question.");
        Map<String, AnswerRecord> mine = records.computeIfAbsent(participantId, k -> new LinkedHashMap<>());
        if (mine.containsKey(questionId)) throw new IllegalStateException("You already answered this question.");
        int limit = accommodationFor(participantId).adjustedTimeSec(q.getTimeLimitSec());
        if (!canAnswer(q, limit)) throw new IllegalStateException("This question is not open for answers.");
        Answer timed = raw.withTimeMs(effectiveTimeMs(q, raw, limit));
        boolean correct = q.isCorrect(timed);
        int pts = scoringMode.strategy().points(q, timed, limit);
        AnswerRecord rec = new AnswerRecord(questionId, timed.getOptionIndex(), timed.getText(), correct, pts, timed.getTimeMs());
        mine.put(questionId, rec);
        return rec;
    }

    public synchronized boolean hasAnswered(String pid, String questionId) {
        Map<String, AnswerRecord> m = records.get(pid);
        return m != null && m.containsKey(questionId);
    }

    public synchronized Map<String, AnswerRecord> recordsFor(String pid) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(records.getOrDefault(pid, Map.of())));
    }

    public synchronized int totalPoints(String pid) {
        return records.getOrDefault(pid, Map.of()).values().stream().mapToInt(AnswerRecord::getPoints).sum();
    }

    public synchronized boolean isCompleted(String pid) {
        int total = getQuestions().size();
        return total > 0 && records.getOrDefault(pid, Map.of()).size() >= total;
    }

    public synchronized List<LeaderboardEntry> leaderboard() {
        List<LeaderboardEntry> out = new ArrayList<>();
        for (Participant p : participants.values())
            out.add(new LeaderboardEntry(p.getNickname(), totalPoints(p.getId()), records.getOrDefault(p.getId(), Map.of()).size()));
        out.sort(Comparator.comparingInt(LeaderboardEntry::getPoints).reversed()
                .thenComparing(LeaderboardEntry::getNickname, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** Serialize under the session lock so saving never races with live answers. */
    private synchronized void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    private static String newCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }
}
