package ph.studytabai.ui;

import ph.studytabai.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Add or edit one question (MCQ, true/false, or short answer). Returns null if cancelled. */
public final class QuestionDialog extends JDialog {
    private static final int MAX_OPTIONS = 6;

    private final String type;
    private final Question existing;
    private Question result;

    private final JTextArea prompt = Ui.textArea(3, 40);
    private final JSpinner points = new JSpinner(new SpinnerNumberModel(1000, 0, 10000, 100));
    private final JSpinner seconds = new JSpinner(new SpinnerNumberModel(20, 5, 600, 5));
    private final JTextField explanation = new JTextField(30);
    private final JTextField image = new JTextField(24);
    private final JTextField standards = new JTextField(24);
    private final JTextField[] optText = new JTextField[MAX_OPTIONS];
    private final JRadioButton[] optCorrect = new JRadioButton[MAX_OPTIONS];
    private final JRadioButton tfTrue = new JRadioButton("True", true), tfFalse = new JRadioButton("False");
    private final JTextArea accepted = Ui.textArea(4, 30);

    private QuestionDialog(Window owner, String type, Question existing, List<Standard> known) {
        super(owner, existing == null ? "Add question" : "Edit question", ModalityType.APPLICATION_MODAL);
        this.type = type;
        this.existing = existing;

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 6));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(Ui.labeled("Question", Ui.scroll(prompt)));

        switch (type) {
            case "MCQ" -> {
                ButtonGroup g = new ButtonGroup();
                for (int i = 0; i < MAX_OPTIONS; i++) {
                    optText[i] = new JTextField(30);
                    optCorrect[i] = new JRadioButton("Correct");
                    Ui.name(optCorrect[i], "Mark option " + (i + 1) + " as correct");
                    g.add(optCorrect[i]);
                    JPanel row = new JPanel(new BorderLayout(6, 0));
                    row.add(new JLabel("Option " + (i + 1) + (i < 2 ? " (required)" : " (optional)")), BorderLayout.WEST);
                    row.add(Ui.name(optText[i], "Option " + (i + 1) + " text"), BorderLayout.CENTER);
                    row.add(optCorrect[i], BorderLayout.EAST);
                    form.add(row);
                }
                optCorrect[0].setSelected(true);
            }
            case "TF" -> {
                ButtonGroup g = new ButtonGroup();
                g.add(tfTrue); g.add(tfFalse);
                form.add(Ui.flow(new JLabel("Correct answer:"), tfTrue, tfFalse));
            }
            default -> form.add(Ui.labeled("Accepted answers (one per line; case and punctuation are ignored)", Ui.scroll(accepted)));
        }

        form.add(Ui.flow(new JLabel("Points"), Ui.name(points, "Points"), new JLabel("Time limit (seconds)"), Ui.name(seconds, "Time limit in seconds")));
        form.add(Ui.labeled("Explanation shown after answering (optional)", explanation));
        JButton browse = Ui.button("Browse…", this::browseImage);
        form.add(Ui.flow(new JLabel("Image file (optional):"), Ui.name(image, "Image file path"), browse));
        StringBuilder hint = new StringBuilder("Standard codes, comma separated");
        if (!known.isEmpty()) {
            hint.append(" (known: ");
            for (int i = 0; i < Math.min(6, known.size()); i++) hint.append(i > 0 ? ", " : "").append(known.get(i).getCode());
            hint.append(")");
        }
        form.add(Ui.labeled(hint.toString(), standards));

        JPanel buttons = Ui.flow(Ui.button("Save question", this::ok), Ui.button("Cancel", this::dispose));
        getContentPane().add(Ui.scroll(form), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        fill();
        setSize(760, type.equals("MCQ") ? 700 : 520);
        setLocationRelativeTo(owner);
    }

    public static Question show(Window owner, String type, Question existing, List<Standard> known) {
        QuestionDialog d = new QuestionDialog(owner, type, existing, known);
        d.setVisible(true);
        return d.result;
    }

    private void fill() {
        if (existing == null) return;
        prompt.setText(existing.getPrompt());
        points.setValue(existing.getPoints());
        seconds.setValue(existing.getTimeLimitSec());
        explanation.setText(existing.getExplanation() == null ? "" : existing.getExplanation());
        image.setText(existing.getImagePath() == null ? "" : existing.getImagePath());
        standards.setText(String.join(", ", existing.getStandardCodes()));
        if (existing instanceof MultipleChoiceQuestion m) {
            for (int i = 0; i < m.getOptions().size(); i++) {
                optText[i].setText(m.getOptions().get(i).getText());
                optCorrect[i].setSelected(m.getOptions().get(i).isCorrect());
            }
        } else if (existing instanceof TrueFalseQuestion t) {
            tfTrue.setSelected(t.getCorrectValue());
            tfFalse.setSelected(!t.getCorrectValue());
        } else if (existing instanceof ShortAnswerQuestion s) {
            accepted.setText(String.join("\n", s.getAcceptedAnswers()));
        }
    }

    private void browseImage() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) image.setText(fc.getSelectedFile().getAbsolutePath());
    }

    private void ok() {
        try {
            Question q = existing;
            switch (type) {
                case "MCQ" -> {
                    List<Option> opts = new ArrayList<>();
                    for (int i = 0; i < MAX_OPTIONS; i++) {
                        String t = optText[i].getText().trim();
                        if (t.isEmpty()) {
                            if (optCorrect[i].isSelected()) throw new IllegalArgumentException("The correct option cannot be empty.");
                            continue;
                        }
                        opts.add(new Option(t, optCorrect[i].isSelected()));
                    }
                    if (q == null) q = new MultipleChoiceQuestion(prompt.getText(), opts);
                    else ((MultipleChoiceQuestion) q).setOptions(opts);
                }
                case "TF" -> {
                    if (q == null) q = new TrueFalseQuestion(prompt.getText(), tfTrue.isSelected());
                    else ((TrueFalseQuestion) q).setCorrectValue(tfTrue.isSelected());
                }
                default -> {
                    List<String> acc = List.of(accepted.getText().split("\\R"));
                    if (q == null) q = new ShortAnswerQuestion(prompt.getText(), acc);
                    else ((ShortAnswerQuestion) q).setAcceptedAnswers(acc);
                }
            }
            q.setPrompt(prompt.getText());
            q.setPoints((Integer) points.getValue());
            q.setTimeLimitSec((Integer) seconds.getValue());
            q.setExplanation(explanation.getText().isBlank() ? null : explanation.getText().trim());
            q.setImagePath(image.getText().isBlank() ? null : image.getText().trim());
            for (String c : q.getStandardCodes()) q.removeStandardCode(c);
            for (String c : standards.getText().split(",")) if (!c.isBlank()) q.addStandardCode(c);
            result = q;
            dispose();
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        }
    }
}
