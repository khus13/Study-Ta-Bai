package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Teacher;

import javax.swing.*;
import java.awt.*;

public final class TeacherHome extends JPanel {
    private final SessionsPanel sessions;

    public TeacherHome(MainFrame frame, AppContext ctx, Teacher teacher) {
        super(new BorderLayout());
        ProvinceScene top = ProvinceScene.banner();
        top.setLayout(new BorderLayout());
        JLabel welcome = new JLabel("Welcome, " + teacher.getDisplayName() + "!  ");
        welcome.setForeground(Theme.isHighContrast() ? Color.WHITE : Palette.TEXT);
        welcome.setOpaque(true);
        welcome.setBackground(Theme.isHighContrast() ? Color.BLACK : new Color(0xFFFCF0));
        welcome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.isHighContrast() ? Color.WHITE : Palette.WOOD, 2),
                BorderFactory.createEmptyBorder(4, 10, 4, 6)));
        JPanel right = Ui.flow(welcome, Ui.button("Log out", () -> { if (frame != null) frame.logoutTeacher(); }));
        right.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 8));
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        sessions = new SessionsPanel(ctx, teacher);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Lessons", new LessonsPanel(ctx, teacher));
        tabs.addTab("Quizzes & flashcards", new ActivitiesPanel(ctx, teacher));
        tabs.addTab("Study Ta Bai", new StudyTaBaiPanel(ctx, teacher));
        tabs.addTab("Run a session", sessions);
        ReportsPanel reports = new ReportsPanel(ctx, teacher);
        tabs.addTab("Reports", reports);
        tabs.addTab("Standards", new StandardsPanel(ctx));
        tabs.addChangeListener(e -> { if (tabs.getSelectedComponent() == reports) reports.refresh(); });
        for (int i = 0; i < Math.min(9, tabs.getTabCount()); i++) tabs.setMnemonicAt(i, '1' + i);
        add(tabs, BorderLayout.CENTER);
    }

    public void shutdown() { sessions.shutdown(); }
}
