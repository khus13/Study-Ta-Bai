package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Document;
import ph.studytabai.model.Teacher;
import ph.studytabai.service.DocumentException;
import ph.studytabai.service.DocumentParser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.util.Comparator;

/** Upload PDF/DOCX/TXT lessons, review and fix the extracted text. */
public final class LessonsPanel extends JPanel {
    private final AppContext ctx;
    private final Teacher teacher;
    private final ShelfPanel<Document> shelf = new ShelfPanel<>(
            Document::getTitle,
            d -> ActivitiesPanel.count(d.getText() == null || d.getText().isBlank() ? 0 : d.getText().trim().split("\\s+").length, "word"),
            d -> "Lesson", 1, "No lessons yet!\nUse Upload Lesson on the counter.");
    private final JTextField title = new JTextField(30);
    private final JTextArea text = Ui.textArea(20, 60);

    public LessonsPanel(AppContext ctx, Teacher teacher) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        this.teacher = teacher;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        shelf.keyBy(Document::getId);
        shelf.onSelect(d -> showSelected());
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(Ui.heading("Lessons"), BorderLayout.NORTH);
        left.add(shelf, BorderLayout.CENTER);
        shelf.counter(Ui.button("Upload Lesson…", this::upload), Ui.button("Delete", this::delete));
        left.setPreferredSize(new Dimension(380, 400));

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(Ui.labeled("Title", title), BorderLayout.NORTH);
        right.add(Ui.scroll(Ui.name(text, "Extracted lesson text (editable)")), BorderLayout.CENTER);
        right.add(Ui.flow(Ui.button("Save Changes", this::save),
                new JLabel("Tip: fix any extraction mistakes here before generating quizzes.")), BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
        reload();
    }

    private void reload() {
        shelf.setItems(ctx.documents.find(d -> d.getTeacherId().equals(teacher.getId())).stream()
                .sorted(Comparator.comparing(Document::getCreatedAt).reversed()).toList());
        showSelected();
    }

    private void showSelected() {
        Document d = shelf.getSelected();
        title.setText(d == null ? "" : d.getTitle());
        text.setText(d == null ? "" : d.getText());
        text.setCaretPosition(0);
    }

    private void upload() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Lessons (PDF, DOCX, TXT)", "pdf", "docx", "txt"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        Ui.background(this, () -> {
            String t = new DocumentParser().parse(p);
            String n = p.getFileName().toString();
            String base = n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n;
            return new Document(teacher.getId(), base.length() > 200 ? base.substring(0, 200) : base, n, t);
        }, doc -> {
            ctx.documents.save(doc);
            reload();
            shelf.setSelected(doc);
        });
    }

    private void save() {
        Document d = shelf.getSelected();
        if (d == null) { Ui.error(this, "Select a lesson first."); return; }
        try {
            d.setTitle(title.getText());
            d.setText(text.getText());
            ctx.documents.save(d);
            shelf.repaint();
            Ui.info(this, "Saved.");
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        }
    }

    private void delete() {
        Document d = shelf.getSelected();
        if (d == null || !Ui.confirm(this, "Delete \"" + d.getTitle() + "\"?")) return;
        ctx.documents.delete(d.getId());
        reload();
    }
}
