package ph.studytabai.session;

import ph.studytabai.model.Answer;
import ph.studytabai.model.Question;
import ph.studytabai.model.Quiz;

/** Teacher-paced game: one question at a time, with start / pause / resume / next / end. */
public final class LiveSession extends Session {
    private static final long serialVersionUID = 1L;
    private static final long GRACE_MS = 1500;

    private int currentIndex = -1;
    private long startedAtMs;
    private long pausedElapsedMs;

    public LiveSession(String teacherId, Quiz quiz) {
        super(teacherId, quiz);
        if (quiz.itemCount() == 0) throw new IllegalArgumentException("Add at least one question before hosting a live game.");
    }

    @Override public String getMode() { return "live"; }

    public synchronized void start() {
        if (getStatus() != SessionStatus.LOBBY) throw new IllegalStateException("The game has already started.");
        currentIndex = 0;
        startedAtMs = now();
        setStatus(SessionStatus.RUNNING);
    }

    public synchronized void pause() {
        if (getStatus() != SessionStatus.RUNNING) throw new IllegalStateException("The game is not running.");
        pausedElapsedMs = now() - startedAtMs;
        setStatus(SessionStatus.PAUSED);
    }

    public synchronized void resume() {
        if (getStatus() != SessionStatus.PAUSED) throw new IllegalStateException("The game is not paused.");
        startedAtMs = now() - pausedElapsedMs;
        setStatus(SessionStatus.RUNNING);
    }

    /** Skip / advance. Ends the game after the last question. */
    public synchronized void next() {
        SessionStatus s = getStatus();
        if (s != SessionStatus.RUNNING && s != SessionStatus.PAUSED) throw new IllegalStateException("The game is not running.");
        if (currentIndex + 1 >= getQuestions().size()) { end(); return; }
        currentIndex++;
        startedAtMs = now();
        pausedElapsedMs = 0;
        setStatus(SessionStatus.RUNNING);
    }

    /** Serialize under the session lock so saving never races with live answers. */
    private synchronized void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    public synchronized int getCurrentIndex() { return currentIndex; }

    public synchronized Question currentQuestion() {
        SessionStatus s = getStatus();
        boolean live = s == SessionStatus.RUNNING || s == SessionStatus.PAUSED;
        return live && currentIndex >= 0 && currentIndex < getQuestions().size() ? getQuestions().get(currentIndex) : null;
    }

    public synchronized long elapsedMs() {
        return getStatus() == SessionStatus.RUNNING ? now() - startedAtMs : pausedElapsedMs;
    }

    /** Milliseconds left for this participant (accommodations can extend their time). */
    public synchronized long remainingMs(String participantId) {
        Question q = currentQuestion();
        if (q == null) return 0;
        long limit = accommodationFor(participantId).adjustedTimeSec(q.getTimeLimitSec()) * 1000L;
        return Math.max(0, limit - elapsedMs());
    }

    @Override
    protected synchronized boolean canAnswer(Question q, int effectiveLimitSec) {
        Question cur = currentQuestion();
        return getStatus() == SessionStatus.RUNNING && cur != null && cur.getId().equals(q.getId())
                && elapsedMs() <= effectiveLimitSec * 1000L + GRACE_MS;
    }

    /** Server-measured time: the client cannot fake a faster answer. */
    @Override
    protected long effectiveTimeMs(Question q, Answer a, int effectiveLimitSec) { return elapsedMs(); }
}
