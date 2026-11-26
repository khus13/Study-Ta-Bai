package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.ai.*;
import ph.studytabai.model.AiResource;
import ph.studytabai.model.Document;
import ph.studytabai.model.Teacher;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Comparator;

/** Study Ta Bai: teacher assistant. Results are always editable and only saved when the teacher says so. */
public final class StudyTaBaiPanel extends JPanel {
    private static final String[] TASKS = {"Generate a resource", "Differentiate (easier / on-level / advanced)", "Translate", "Adjust reading level"};

    private final AppContext ctx;
    private final Teacher teacher;
    private final JComboBox<String> task = new JComboBox<>(TASKS);
    private final JComboBox<Object> lesson = new JComboBox<>();
    private final JTextArea input = Ui.textArea(4, 60);
    private final JComboBox<String> resourceType = new JComboBox<>(new String[]{"worksheet", "lesson plan", "study guide", "exit ticket", "discussion questions", "rubric"});
    private final JComboBox<String> grade = new JComboBox<>(new String[]{"Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "College"});
    private final JTextField language = new JTextField("Filipino", 14);
    private final JTextArea output = Ui.textArea(12, 60);
    private final JTextField saveTitle = new JTextField(24);
    private final DefaultListModel<AiResource> savedModel = new DefaultListModel<>();
    private final JList<AiResource> saved = new JList<>(savedModel);

    public StudyTaBaiPanel(AppContext ctx, Teacher teacher) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        this.teacher = teacher;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        grade.setSelectedItem("Grade 8");

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(Ui.heading("Study Ta Bai – Teacher Assistant"));
        controls.add(Ui.labeled("What do you need?", task));
        controls.add(Ui.labeled("Use a lesson (optional)", lesson));
        controls.add(Ui.labeled("Text, topic, or instructions (the lesson text is added if you pick one)", Ui.scroll(input)));
        controls.add(Ui.flow(new JLabel("Resource type"), resourceType, new JLabel("Grade / reading level"), grade, new JLabel("Language"), language,
                Ui.button("✨ Run", this::run)));

        for (Component c : controls.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height + 4));
            }
        }

        JPanel result = new JPanel(new BorderLayout(4, 4));
        result.add(Ui.labeled("Result (edit freely before saving)", Ui.scroll(output)), BorderLayout.CENTER);
        result.add(Ui.flow(new JLabel("Title"), saveTitle, Ui.button("Save", this::save), Ui.button("Copy", this::copy)), BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(new JLabel("Saved resources"), BorderLayout.NORTH);
        right.add(Ui.scroll(Ui.name(saved, "Saved AI resources")), BorderLayout.CENTER);
        right.add(Ui.flow(Ui.button("Open", this::open), Ui.button("Delete", this::delete)), BorderLayout.SOUTH);
        right.setPreferredSize(new Dimension(280, 300));

        add(controls, BorderLayout.NORTH);
        add(result, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        reloadLessons();
        reloadSaved();
    }

    private void reloadLessons() {
        lesson.removeAllItems();
        lesson.addItem("(none)");
        ctx.documents.find(d -> d.getTeacherId().equals(teacher.getId())).forEach(lesson::addItem);
    }

    private void reloadSaved() {
        savedModel.clear();
        ctx.resources.find(r -> r.getTeacherId().equals(teacher.getId())).stream()
                .sorted(Comparator.comparing(AiResource::getCreatedAt).reversed()).forEach(savedModel::addElement);
    }

    private void run() {
        String typed = input.getText().trim();
        String lessonText = lesson.getSelectedItem() instanceof Document d ? d.getText() : "";
        String source = lessonText.isEmpty() ? typed : (typed.isEmpty() ? lessonText : typed + "\n\n" + lessonText);
        if (source.isBlank()) { Ui.error(this, "Type a topic or pick a lesson first."); return; }
        int which = task.getSelectedIndex();
        String rtype = (String) resourceType.getSelectedItem(), gr = (String) grade.getSelectedItem(), lang = language.getText();

        Ui.background(this, () -> {
            AiClient client = ctx.aiClient();
            TextTask t = switch (which) {
                case 0 -> new GenerateTask(source, rtype, gr);
                case 1 -> new DifferentiateTask(source);
                case 2 -> new TranslateTask(source, lang);
                default -> new ReadingLevelTask(source, gr);
            };
            return t.run(client);
        }, res -> {
            output.setText(res);
            output.setCaretPosition(0);
            if (saveTitle.getText().isBlank()) saveTitle.setText(TASKS[which].split(" ")[0] + " – " + java.time.LocalDate.now());
        });
    }

    private void save() {
        try {
            AiResource r = new AiResource(teacher.getId(), TASKS[task.getSelectedIndex()].split(" ")[0].toLowerCase(), saveTitle.getText(), output.getText());
            ctx.resources.save(r);
            reloadSaved();
            Ui.info(this, "Saved.");
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        }
    }

    private void copy() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(output.getText()), null);
    }

    private void open() {
        AiResource r = saved.getSelectedValue();
        if (r == null) return;
        output.setText(r.getContent());
        saveTitle.setText(r.getTitle());
        output.setCaretPosition(0);
    }

    private void delete() {
        AiResource r = saved.getSelectedValue();
        if (r != null && Ui.confirm(this, "Delete this saved resource?")) { ctx.resources.delete(r.getId()); reloadSaved(); }
    }
}
