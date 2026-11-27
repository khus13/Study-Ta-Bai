package ph.studytabai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public final class Quiz extends Activity {
    private static final long serialVersionUID = 1L;

    private final List<Question> questions = new ArrayList<>();

    public Quiz(String teacherId, String title) { super(teacherId, title); }

    public void addQuestion(Question q) {
        if (q == null) throw new IllegalArgumentException("question is required");
        questions.add(q);
    }

    public boolean removeQuestion(String questionId) {
        return questions.removeIf(q -> q.getId().equals(questionId));
    }

    /** Reorder: move the question at 'from' to position 'to'. */
    public void moveQuestion(int from, int to) {
        if (from < 0 || from >= questions.size() || to < 0 || to >= questions.size())
            throw new IndexOutOfBoundsException("Invalid position");
        questions.add(to, questions.remove(from));
    }

    public List<Question> getQuestions() { return Collections.unmodifiableList(questions); }

    @Override public String getKind() { return "quiz"; }
    @Override public int itemCount() { return questions.size(); }
}
