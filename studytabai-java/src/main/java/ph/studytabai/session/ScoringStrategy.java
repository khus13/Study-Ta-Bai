package ph.studytabai.session;

import ph.studytabai.model.Answer;
import ph.studytabai.model.Question;

/** POLYMORPHISM: different game modes score the same answer differently. */
public interface ScoringStrategy {
    int points(Question q, Answer a, int effectiveLimitSec);
}
