package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Teacher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

/** One window, several screens: role chooser, teacher login, teacher home, student. */
public final class MainFrame extends JFrame {
    private final AppContext ctx;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final Map<String, JComponent> screens = new HashMap<>();
    private TeacherHome teacherHome;
    private StudentPanel studentPanel;

    public MainFrame(AppContext ctx) {
        super("Study Ta Bai");
        this.ctx = ctx;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1120, 740);
        setLocationRelativeTo(null);
        setJMenuBar(buildMenu());
        setContentPane(root);
        put("role", buildRoleScreen());
        put("login", new LoginPanel(this, ctx));
        showRole();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); dispose(); System.exit(0); }
        });
    }

    private void put(String name, JComponent c) {
        JComponent old = screens.put(name, c);
        if (old != null) root.remove(old);
        root.add(c, name);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        JCheckBoxMenuItem contrast = new JCheckBoxMenuItem("High contrast");
        contrast.setMnemonic(KeyEvent.VK_H);
        contrast.addActionListener(e -> Theme.setHighContrast(contrast.isSelected()));
        JMenuItem bigger = new JMenuItem("Larger text");
        bigger.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        bigger.addActionListener(e -> Theme.setScale(Theme.getScale() + 0.1));
        JMenuItem smaller = new JMenuItem("Smaller text");
        smaller.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        smaller.addActionListener(e -> Theme.setScale(Theme.getScale() - 0.1));
        JMenuItem reset = new JMenuItem("Reset text size");
        reset.addActionListener(e -> Theme.setScale(1.0));
        view.add(contrast); view.add(bigger); view.add(smaller); view.add(reset);
        JMenu go = new JMenu("Go");
        go.setMnemonic(KeyEvent.VK_G);
        JMenuItem home = new JMenuItem("Choose role");
        home.addActionListener(e -> showRole());
        go.add(home);
        bar.add(view);
        bar.add(go);
        return bar;
    }

    private JComponent buildRoleScreen() {
        JPanel p = new JPanel(new GridBagLayout());
        JPanel box = new JPanel(new GridLayout(0, 1, 12, 12));
        JLabel title = Ui.heading("📚 Study Ta Bai");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel sub = new JLabel("Quizzes, flashcards, and live games for the whole class", SwingConstants.CENTER);
        JButton teacher = Ui.button("I'm a teacher", this::showLogin);
        JButton student = Ui.button("I'm a student (no account needed)", this::showStudent);
        teacher.setMnemonic(KeyEvent.VK_T);
        student.setMnemonic(KeyEvent.VK_S);
        teacher.setFont(teacher.getFont().deriveFont(Font.BOLD, teacher.getFont().getSize2D() * 1.3f));
        student.setFont(student.getFont().deriveFont(Font.BOLD, student.getFont().getSize2D() * 1.3f));
        box.add(title); box.add(sub); box.add(teacher); box.add(student);
        p.add(box);
        return p;
    }

    public void showRole() { cards.show(root, "role"); }
    public void showLogin() { cards.show(root, "login"); }

    public void showTeacher(Teacher t) {
        if (teacherHome != null) teacherHome.shutdown();
        teacherHome = new TeacherHome(this, ctx, t);
        put("teacher", teacherHome);
        cards.show(root, "teacher");
    }

    public void logoutTeacher() {
        if (teacherHome != null) { teacherHome.shutdown(); teacherHome = null; }
        showRole();
    }

    public void showStudent() {
        if (studentPanel != null) studentPanel.shutdown();
        studentPanel = new StudentPanel(this, ctx);
        put("student", studentPanel);
        cards.show(root, "student");
    }

    private void shutdown() {
        if (teacherHome != null) teacherHome.shutdown();
        if (studentPanel != null) studentPanel.shutdown();
    }
}
