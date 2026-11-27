package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Participant;
import ph.studytabai.model.Teacher;
import ph.studytabai.report.*;
import ph.studytabai.session.Session;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Class and student reports with CSV / PDF export. */
public final class ReportsPanel extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final AppContext ctx;
    private final Teacher teacher;
    private final ReportService service = new ReportService();
    private final DefaultListModel<Session> model = new DefaultListModel<>();
    private final JList<Session> list = new JList<>(model);
    private final JComboBox<String> students = new JComboBox<>();
    private final JPanel view = new JPanel();
    private Reportable current;

    public ReportsPanel(AppContext ctx, Teacher teacher) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        this.teacher = teacher;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                Session x = (Session) v;
                return super.getListCellRendererComponent(l, x.getTitle() + " · " + (x.getMode().equals("live") ? "Live" : "Homework")
                        + " · " + DATE.format(x.getCreatedAt()) + " · " + x.participantCount() + " students", i, s, f);
            }
        });
        Ui.name(list, "Past sessions");
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) sessionSelected(); });
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(Ui.heading("Sessions"), BorderLayout.NORTH);
        left.add(Ui.scroll(list), BorderLayout.CENTER);
        left.add(Ui.flow(Ui.button("Refresh", this::refresh)), BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(340, 300));

        view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
        JPanel bar = Ui.flow(Ui.button("Class Report", this::showClass), new JLabel("Student:"), Ui.name(students, "Choose a student"),
                Ui.button("Student Report", this::showStudent), Ui.button("Export CSV", () -> export(new CsvExporter())),
                Ui.button("Export PDF", () -> export(new PdfExporter())));
        add(left, BorderLayout.WEST);
        add(Ui.scroll(view), BorderLayout.CENTER);
        add(bar, BorderLayout.NORTH);
        refresh();
    }

    public void refresh() {
        Session sel = list.getSelectedValue();
        model.clear();
        ctx.sessions.find(s -> s.getTeacherId().equals(teacher.getId())).stream()
                .sorted(Comparator.comparing(Session::getCreatedAt).reversed()).forEach(model::addElement);
        if (sel != null) list.setSelectedValue(sel, true);
    }

    private void sessionSelected() {
        students.removeAllItems();
        Session s = list.getSelectedValue();
        if (s == null) return;
        for (Participant p : s.getParticipants()) students.addItem(p.getNickname());
        showClass();
    }

    private void showClass() {
        Session s = list.getSelectedValue();
        if (s == null) { Ui.error(this, "Select a session first."); return; }
        display(service.classReport(s));
    }

    private void showStudent() {
        Session s = list.getSelectedValue();
        String nick = (String) students.getSelectedItem();
        if (s == null || nick == null) { Ui.error(this, "Select a session and a student first."); return; }
        List<Session> mine = ctx.sessions.find(x -> x.getTeacherId().equals(teacher.getId()));
        display(service.studentReport(nick, s, mine));
    }

    private void display(Reportable r) {
        current = r;
        view.removeAll();
        view.add(Ui.heading(r.getTitle()));
        for (Table t : r.getTables()) {
            JLabel l = new JLabel(t.getTitle());
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            l.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
            l.setAlignmentX(LEFT_ALIGNMENT);
            view.add(l);
            JTable table = Ui.table(t.getHeader(), t.getRows());
            table.getAccessibleContext().setAccessibleName(t.getTitle());
            JPanel holder = new JPanel(new BorderLayout());
            holder.add(table.getTableHeader(), BorderLayout.NORTH);
            holder.add(table, BorderLayout.CENTER);
            holder.setAlignmentX(LEFT_ALIGNMENT);
            holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, (t.getRows().size() + 1) * table.getRowHeight() + 12));
            view.add(holder);
        }
        view.revalidate();
        view.repaint();
    }

    private void export(Exporter exporter) {
        if (current == null) { Ui.error(this, "Open a report first."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("report." + exporter.extension()));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        if (!p.getFileName().toString().toLowerCase().endsWith("." + exporter.extension()))
            p = p.resolveSibling(p.getFileName() + "." + exporter.extension());
        if (Files.exists(p) && !Ui.confirm(this, "Replace the existing file?")) return;
        try {
            exporter.export(current, p);
            Ui.info(this, "Saved to " + p);
        } catch (IOException e) {
            Ui.error(this, "Could not save the file: " + e.getMessage());
        }
    }
}
