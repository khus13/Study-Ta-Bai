package ph.studytabai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 2-6 options, exactly one correct. */
@SuppressWarnings("serial")
public class MultipleChoiceQuestion extends Question {
    private static final long serialVersionUID = 1L;

    private final List<Option> options = new ArrayList<>();

    public MultipleChoiceQuestion(String prompt, List<Option> options) {
        super(prompt);
        setOptions(options);
    }

    public final void setOptions(List<Option> newOptions) {
        if (newOptions == null || newOptions.size() < 2 || newOptions.size() > 6)
            throw new IllegalArgumentException("A multiple-choice question needs 2-6 options");
        long correct = newOptions.stream().filter(Option::isCorrect).count();
        if (correct != 1) throw new IllegalArgumentException("Exactly one option must be correct");
        options.clear();
        options.addAll(newOptions);
    }

    public List<Option> getOptions() { return Collections.unmodifiableList(options); }

    @Override
    public boolean isCorrect(Answer a) {
        int i = a.getOptionIndex();
        return i >= 0 && i < options.size() && options.get(i).isCorrect();
    }

    @Override
    public QuestionView toStudentView() {
        List<String> texts = options.stream().map(Option::getText).toList();
        return new QuestionView(getId(), getType(), getPrompt(), texts, getPoints(), getTimeLimitSec(), getImagePath());
    }

    @Override public String getType() { return "MCQ"; }
    @Override public String getCorrectAnswerText() {
        return options.stream().filter(Option::isCorrect).findFirst().map(Option::getText).orElse("");
    }
}
