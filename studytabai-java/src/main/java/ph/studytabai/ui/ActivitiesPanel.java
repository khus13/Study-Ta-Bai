package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.ai.FlashcardGenTask;
import ph.studytabai.ai.QuizGenTask;
import ph.studytabai.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/** Quizzes and flashcard sets: create, AI-generate from a lesson, edit, delete. */
public final class ActivitiesPanel extends JPanel {
    private final AppContext ctx;
    private final Teacher teacher;
    private final DefaultListModel<Activity> model = new DefaultListModel<>();
    private final JList<Activity> list = new JList<>(model);

    public ActivitiesPanel(AppContext ctx, Teacher teacher) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        this.teacher = teacher;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                Activity a = (Activity) v;
                return super.getListCellRendererComponent(l, (a instanceof Quiz ? "📝 " : "🃏 ") + a.getTitle() + "  (" + a.itemCount()
                        + (a instanceof Quiz ? " questions)" : " cards)"), i, s, f);
            }
        });
        Ui.name(list, "Your quizzes and flashcard sets");
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) edit(); }
        });
        add(Ui.heading("Quizzes & flashcards"), BorderLayout.NORTH);
        add(Ui.scroll(list), BorderLayout.CENTER);
        add(Ui.flow(Ui.button("New quiz", this::newQuiz), Ui.button("New flashcard set", this::newCards),
                Ui.button("✨ Generate from lesson (AI)", this::generate), Ui.button("Edit", this::edit),
                Ui.button("Delete", this::delete)), BorderLayout.SOUTH);
        reload();
    }

    private Window owner() { return SwingUtilities.getWindowAncestor(this); }

    private void reload() {
        model.clear();
        ctx.activities.find(a -> a.getTeacherId().equals(teacher.getId())).stream()
                .sorted(Comparator.comparing(Activity::getCreatedAt).reversed()).forEach(model::addElement);
    }

    private String askTitle(String dflt) {
        String t = JOptionPane.showInputDialog(this, "Title:", dflt);
        return t == null || t.isBlank() ? null : t.trim();
    }

    private void newQuiz() {
        String t = askTitle("My quiz");
        if (t == null) return;
        Quiz q = new Quiz(teacher.getId(), t);
        ctx.activities.save(q);
        new QuizEditorDialog(owner(), ctx, q).setVisible(true);
        reload();
    }

    private void newCards() {
        String t = askTitle("My flashcards");
        if (t == null) return;
        FlashcardSet s = new FlashcardSet(teacher.getId(), t);
        ctx.activities.save(s);
        new FlashcardEditorDialog(owner(), ctx, s).setVisible(true);
        reload();
    }

    private void edit() {
        Activity a = list.getSelectedValue();
        if (a == null) { Ui.error(this, "Select something to edit."); return; }
        if (a instanceof Quiz q) new QuizEditorDialog(owner(), ctx, q).setVisible(true);
        else if (a instanceof FlashcardSet f) new FlashcardEditorDialog(owner(), ctx, f).setVisible(true);
        reload();
    }

    private void delete() {
        Activity a = list.getSelectedValue();
        if (a != null && Ui.confirm(this, "Delete \"" + a.getTitle() + "\"?")) { ctx.activities.delete(a.getId()); reload(); }
    }

    private void generate() {
        var lessons = ctx.documents.find(d -> d.getTeacherId().equals(teacher.getId()));
        if (lessons.isEmpty()) { Ui.error(this, "Upload a lesson first (Lessons tab)."); return; }
        JComboBox<Document> lesson = new JComboBox<>(lessons.toArray(new Document[0]));
        JComboBox<String> kind = new JComboBox<>(new String[]{"Quiz", "Flashcards"});
        JSpinner count = new JSpinner(new SpinnerNumberModel(8, 1, 30, 1));
        JComboBox<String> diff = new JComboBox<>(new String[]{"easy", "medium", "hard"});
        diff.setSelectedItem("medium");
        JCheckBox mcq = new JCheckBox("Multiple choice", true), tf = new JCheckBox("True/False", true), sa = new JCheckBox("Short answer", true);
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(Ui.labeled("Lesson", lesson));
        p.add(Ui.labeled("Create", kind));
        p.add(Ui.labeled("How many", count));
        p.add(Ui.labeled("Difficulty (quiz only)", diff));
        p.add(Ui.flow(mcq, tf, sa));
        if (JOptionPane.showConfirmDialog(this, p, "Generate with AI", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        Document doc = (Document) lesson.getSelectedItem();
        int n = (Integer) count.getValue();
        Set<String> types = new LinkedHashSet<>();
        if (mcq.isSelected()) types.add("mcq");
        if (tf.isSelected()) types.add("tf");
        if (sa.isSelected()) types.add("short");
        boolean quiz = kind.getSelectedIndex() == 0;
        String difficulty = (String) diff.getSelectedItem();

        Ui.background(this, () -> {
            var client = ctx.aiClient();
            if (quiz) {
                Quiz q = new Quiz(teacher.getId(), doc.getTitle() + " – quiz (AI draft)");
                new QuizGenTask(doc.getText(), n, types, difficulty).run(client).forEach(q::addQuestion);
                return (Activity) q;
            }
            FlashcardSet s = new FlashcardSet(teacher.getId(), doc.getTitle() + " – flashcards (AI draft)");
            new FlashcardGenTask(doc.getText(), n).run(client).forEach(s::addCard);
            return (Activity) s;
        }, draft -> {
            Ui.info(this, "Draft ready! Review and edit it, then press \"Save & close\" to keep it.");
            if (draft instanceof Quiz q) new QuizEditorDialog(owner(), ctx, q).setVisible(true);
            else new FlashcardEditorDialog(owner(), ctx, (FlashcardSet) draft).setVisible(true);
            reload();
        });
    }
}
