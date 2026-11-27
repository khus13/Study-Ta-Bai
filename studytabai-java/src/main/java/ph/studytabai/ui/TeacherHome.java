package ph.studytabai.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ph.studytabai.AppContext;
import ph.studytabai.model.Teacher;

public final class TeacherHome extends JPanel {
    private final SessionsPanel sessions;

    public TeacherHome(MainFrame frame, AppContext ctx, Teacher teacher) {
        super(new BorderLayout());
        ProvinceScene top = ProvinceScene.banner();
        top.setLayout(new BorderLayout());
        JLabel welcome = new JLabel(("Welcome, " + teacher.getDisplayName() + "!").toUpperCase(java.util.Locale.ROOT)) {
            @Override public void updateUI() {
                super.updateUI();
                setFont(Fonts.sign((float) (12 * 1.15 * 0.95 * Theme.getScale())));
            }
        };
        welcome.setForeground(Theme.isHighContrast() ? Color.WHITE : Palette.TEXT);
        welcome.setOpaque(true);
        welcome.setBackground(Theme.isHighContrast() ? Color.BLACK : new Color(0xFFFCF0));
        welcome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.isHighContrast() ? Color.WHITE : Palette.WOOD, 2),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        JPanel right = Ui.flow(welcome, Ui.button("LOG OUT", () -> { if (frame != null) frame.logoutTeacher(); }));
        right.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 8));
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        sessions = new SessionsPanel(ctx, teacher);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Lessons", new LessonsPanel(ctx, teacher));
        tabs.addTab("Quizzes & Flashcards", new ActivitiesPanel(ctx, teacher));
        tabs.addTab("Run a Session", sessions);
        ReportsPanel reports = new ReportsPanel(ctx, teacher);
        tabs.addTab("Reports", reports);
        tabs.addChangeListener(e -> { if (tabs.getSelectedComponent() == reports) reports.refresh(); });
        for (int i = 0; i < Math.min(9, tabs.getTabCount()); i++) tabs.setMnemonicAt(i, '1' + i);
        add(tabs, BorderLayout.CENTER);
    }

    public void shutdown() { sessions.shutdown(); }
}
