package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Teacher;

import javax.swing.*;
import java.awt.*;

public final class TeacherHome extends JPanel {
    private final SessionsPanel sessions;

    public TeacherHome(MainFrame frame, AppContext ctx, Teacher teacher) {
        super(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("  Welcome, " + teacher.getDisplayName() + "!"), BorderLayout.WEST);
        top.add(Ui.flow(Ui.button("Log out", () -> frame.logoutTeacher())), BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        sessions = new SessionsPanel(ctx, teacher);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Lessons", new LessonsPanel(ctx, teacher));
        tabs.addTab("Quizzes & flashcards", new ActivitiesPanel(ctx, teacher));
        tabs.addTab("Wayground AI", new WaygroundPanel(ctx, teacher));
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
