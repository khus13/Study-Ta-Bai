package ph.studytabai.session;

import ph.studytabai.model.Answer;
import ph.studytabai.model.Question;

/** Practice mode: full points for any correct answer, no time pressure. */
public final class FlatScoring implements ScoringStrategy {
    @Override public int points(Question q, Answer a, int limit) { return q.isCorrect(a) ? q.getPoints() : 0; }
}
