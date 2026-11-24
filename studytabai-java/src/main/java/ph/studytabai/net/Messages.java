package ph.studytabai.net;

import ph.studytabai.model.Accommodation;
import ph.studytabai.model.Answer;
import ph.studytabai.model.Flashcard;
import ph.studytabai.model.QuestionView;
import ph.studytabai.session.LeaderboardEntry;

import java.io.Serializable;
import java.util.List;

/** All network messages. They are plain serializable DTOs; students never receive correct answers up front. */
public final class Messages {
    private Messages() {}

    public abstract static class Msg implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    // ---- student -> server ----
    public static final class JoinRequest extends Msg {
        private static final long serialVersionUID = 1L;
        public final String code, nickname, participantId, token;
        public JoinRequest(String code, String nickname, String participantId, String token) {
            this.code = code; this.nickname = nickname; this.participantId = participantId; this.token = token;
        }
    }

    public static final class AnswerSubmit extends Msg {
        private static final long serialVersionUID = 1L;
        public final String questionId;
        public final Answer answer;
        public AnswerSubmit(String questionId, Answer answer) { this.questionId = questionId; this.answer = answer; }
    }

    public static final class ReactionMsg extends Msg {
        private static final long serialVersionUID = 1L;
        public final String emoji;
        public ReactionMsg(String emoji) { this.emoji = emoji; }
    }

    public static final class Leave extends Msg {
        private static final long serialVersionUID = 1L;
    }

    // ---- server -> student ----
    public static final class JoinResponse extends Msg {
        private static final long serialVersionUID = 1L;
        public final boolean ok;
        public final String error, participantId, token, mode, title;
        public final List<QuestionView> questions;   // self-paced quiz
        public final List<Flashcard> cards;          // flashcard set
        public final Accommodation accommodation;
        public JoinResponse(boolean ok, String error, String participantId, String token, String mode, String title,
                            List<QuestionView> questions, List<Flashcard> cards, Accommodation accommodation) {
            this.ok = ok; this.error = error; this.participantId = participantId; this.token = token; this.mode = mode;
            this.title = title; this.questions = questions; this.cards = cards; this.accommodation = accommodation;
        }
        public static JoinResponse fail(String error) { return new JoinResponse(false, error, null, null, null, null, null, null, null); }
    }

    public static final class StateUpdate extends Msg {
        private static final long serialVersionUID = 1L;
        public final String status;
        public final int index, total, participantCount;
        public final QuestionView current;
        public final long remainingMs;
        public final boolean answered;
        public final List<LeaderboardEntry> leaderboard;
        public final Accommodation accommodation;
        public final boolean musicHint;
        public StateUpdate(String status, int index, int total, int participantCount, QuestionView current, long remainingMs,
                           boolean answered, List<LeaderboardEntry> leaderboard, Accommodation accommodation, boolean musicHint) {
            this.status = status; this.index = index; this.total = total; this.participantCount = participantCount;
            this.current = current; this.remainingMs = remainingMs; this.answered = answered;
            this.leaderboard = leaderboard; this.accommodation = accommodation; this.musicHint = musicHint;
        }
    }

    public static final class AnswerResult extends Msg {
        private static final long serialVersionUID = 1L;
        public final String questionId, correctText, explanation, error;
        public final boolean accepted, correct;
        public final int points, total;
        public AnswerResult(String questionId, boolean accepted, boolean correct, int points, int total,
                            String correctText, String explanation, String error) {
            this.questionId = questionId; this.accepted = accepted; this.correct = correct; this.points = points;
            this.total = total; this.correctText = correctText; this.explanation = explanation; this.error = error;
        }
    }

    public static final class ReactionBroadcast extends Msg {
        private static final long serialVersionUID = 1L;
        public final String nickname, emoji;
        public ReactionBroadcast(String nickname, String emoji) { this.nickname = nickname; this.emoji = emoji; }
    }

    public static final class ErrorMsg extends Msg {
        private static final long serialVersionUID = 1L;
        public final String message;
        public ErrorMsg(String message) { this.message = message; }
    }
}
