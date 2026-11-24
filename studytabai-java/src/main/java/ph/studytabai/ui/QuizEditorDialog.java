package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.*;

import javax.swing.*;
import java.awt.*;

/** Manual quiz editor: add, edit, reorder, delete questions. Nothing is saved until "Save & close". */
public final class QuizEditorDialog extends JDialog {
    private final AppContext ctx;
    private final Quiz quiz;
    private final DefaultListModel<Question> model = new DefaultListModel<>();
    private final JList<Question> list = new JList<>(model);
    private final JTextField title = new JTextField(30);

    public QuizEditorDialog(Window owner, AppContext ctx, Quiz quiz) {
        super(owner, "Quiz editor", ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.quiz = quiz;
        title.setText(quiz.getTitle());

        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                Question q = (Question) v;
                String t = (i + 1) + ". [" + q.getType() + "] " + q.getPrompt() + "  (" + q.getPoints() + " pts, " + q.getTimeLimitSec() + "s)";
                return super.getListCellRendererComponent(l, t, i, s, f);
            }
        });
        Ui.name(list, "Questions in this quiz");
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) edit(); }
        });

        JPanel north = new JPanel(new BorderLayout());
        north.add(Ui.labeled("Quiz title", title), BorderLayout.CENTER);
        JPanel side = new JPanel(new GridLayout(0, 1, 4, 4));
        side.add(Ui.button("Add multiple choice", () -> add("MCQ")));
        side.add(Ui.button("Add true / false", () -> add("TF")));
        side.add(Ui.button("Add short answer", () -> add("SHORT")));
        side.add(Ui.button("Edit selected", this::edit));
        side.add(Ui.button("Move up", () -> move(-1)));
        side.add(Ui.button("Move down", () -> move(1)));
        side.add(Ui.button("Delete selected", this::delete));
        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.add(side, BorderLayout.NORTH);

        JPanel bottom = Ui.flow(Ui.button("Save & close", this::saveAndClose), Ui.button("Close without saving", this::dispose));
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(north, BorderLayout.NORTH);
        p.add(Ui.scroll(list), BorderLayout.CENTER);
        p.add(sideWrap, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);
        setContentPane(p);
        reload();
        setSize(960, 600);
        setLocationRelativeTo(owner);
    }

    private void reload() {
        int sel = list.getSelectedIndex();
        model.clear();
        quiz.getQuestions().forEach(model::addElement);
        if (sel >= 0 && sel < model.size()) list.setSelectedIndex(sel);
    }

    private void add(String type) {
        Question q = QuestionDialog.show(this, type, null, ctx.standards.findAll());
        if (q != null) { quiz.addQuestion(q); reload(); list.setSelectedIndex(model.size() - 1); }
    }

    private void edit() {
        Question q = list.getSelectedValue();
        if (q == null) { Ui.error(this, "Select a question first."); return; }
        QuestionDialog.show(this, q.getType(), q, ctx.standards.findAll());
        reload();
    }

    private void move(int delta) {
        int i = list.getSelectedIndex();
        if (i < 0 || i + delta < 0 || i + delta >= model.size()) return;
        quiz.moveQuestion(i, i + delta);
        reload();
        list.setSelectedIndex(i + delta);
    }

    private void delete() {
        Question q = list.getSelectedValue();
        if (q != null && Ui.confirm(this, "Delete this question?")) { quiz.removeQuestion(q.getId()); reload(); }
    }

    private void saveAndClose() {
        try {
            quiz.setTitle(title.getText());
        } catch (IllegalArgumentException e) { Ui.error(this, e.getMessage()); return; }
        ctx.activities.save(quiz);
        dispose();
    }
}
