package ph.studytabai.session;

import ph.studytabai.model.Answer;
import ph.studytabai.model.Question;

/** Faster correct answers earn more (50-100% of the points). */
public final class SpeedScoring implements ScoringStrategy {
    @Override public int points(Question q, Answer a, int limit) { return q.score(a, limit); }
}
