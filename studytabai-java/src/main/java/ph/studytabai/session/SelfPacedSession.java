package ph.studytabai.session;

import ph.studytabai.model.Activity;
import ph.studytabai.model.Answer;
import ph.studytabai.model.FlashcardSet;
import ph.studytabai.model.Question;
import ph.studytabai.model.Quiz;

import java.time.Instant;

/** Homework mode: students work at their own pace until the due date. Hosts a quiz or a flashcard set. */
public final class SelfPacedSession extends Session {
    private static final long serialVersionUID = 1L;

    private final Instant dueAt;   // null = no due date

    public SelfPacedSession(String teacherId, Activity activity, Instant dueAt) {
        super(teacherId, activity);
        if (!(activity instanceof Quiz) && !(activity instanceof FlashcardSet))
            throw new IllegalArgumentException("Unsupported activity type.");
        if (activity.itemCount() == 0) throw new IllegalArgumentException("Add some content before assigning it.");
        this.dueAt = dueAt;
        setStatus(SessionStatus.RUNNING);
    }

    @Override public String getMode() { return "self_paced"; }
    public Instant getDueAt() { return dueAt; }

    public boolean isPastDue() { return dueAt != null && now() > dueAt.toEpochMilli(); }

    @Override
    protected boolean canAnswer(Question q, int effectiveLimitSec) {
        return getStatus() == SessionStatus.RUNNING && !isPastDue();
    }

    /** The student's own timer is used, but clamped to the allowed range so scores stay fair. */
    @Override
    protected long effectiveTimeMs(Question q, Answer a, int effectiveLimitSec) {
        return Math.max(0, Math.min(a.getTimeMs(), effectiveLimitSec * 1000L));
    }
}
