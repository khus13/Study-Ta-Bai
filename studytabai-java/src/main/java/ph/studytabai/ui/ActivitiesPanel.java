package ph.studytabai.ui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ph.studytabai.AppContext;
import ph.studytabai.model.Activity;
import ph.studytabai.model.FlashcardSet;
import ph.studytabai.model.Quiz;
import ph.studytabai.model.Teacher;

/** Quizzes and flashcard sets: create, AI-generate from a lesson, edit, delete. */
public final class ActivitiesPanel extends JPanel {
    private final AppContext ctx;
    private final Teacher teacher;
    private final ShelfPanel<Activity> shelf = new ShelfPanel<>(
            Activity::getTitle,
            a -> count(a.itemCount(), a instanceof Quiz ? "question" : "card"),
            a -> a instanceof Quiz ? "Quiz" : "Cards",
            3, "The shelves are empty!\nAdd a New Quiz or a New Flashcard Set from the counter.");

    /** "(1 question)" / "(2 questions)": singular for exactly one, plural otherwise. */
    static String count(int n, String noun) {
        return "(" + n + " " + noun + (n == 1 ? "" : "s") + ")";
    }

    public ActivitiesPanel(AppContext ctx, Teacher teacher) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        this.teacher = teacher;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        shelf.keyBy(Activity::getId);
        shelf.onActivate(a -> edit());
        add(Ui.heading("Quizzes & Flashcards"), BorderLayout.NORTH);
        add(shelf, BorderLayout.CENTER);
                shelf.counter(Ui.button("New Quiz", this::newQuiz), Ui.button("New Flashcard Set", this::newCards),
                Ui.button("Edit", this::edit), Ui.button("Delete", this::delete));
        reload();
    }

    private Window owner() { return SwingUtilities.getWindowAncestor(this); }

    private void reload() {
        shelf.setItems(ctx.activities.find(a -> a.getTeacherId().equals(teacher.getId())).stream()
                .sorted(Comparator.comparing(Activity::getCreatedAt).reversed()).toList());
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
        Activity a = shelf.getSelected();
        if (a == null) { Ui.error(this, "Select something to edit."); return; }
        if (a instanceof Quiz q) new QuizEditorDialog(owner(), ctx, q).setVisible(true);
        else if (a instanceof FlashcardSet f) new FlashcardEditorDialog(owner(), ctx, f).setVisible(true);
        reload();
    }

    private void delete() {
        Activity a = shelf.getSelected();
        if (a != null && Ui.confirm(this, "Delete \"" + a.getTitle() + "\"?")) { ctx.activities.delete(a.getId()); reload(); }
    }
}
