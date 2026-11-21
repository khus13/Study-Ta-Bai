package ph.studytabai.ui;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Small helpers so the screens stay short and consistently accessible. */
public final class Ui {
    private Ui() {}

    public static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        b.getAccessibleContext().setAccessibleName(text);
        return b;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text) {
            @Override public void updateUI() {
                super.updateUI();
                Font f = getFont();
                if (f != null) setFont(new FontUIResource(f.deriveFont(Font.BOLD, f.getSize2D() * 1.5f)));
            }
        };
        l.getAccessibleContext().setAccessibleName(text);
        return l;
    }

    public static JPanel flow(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        for (Component c : parts) p.add(c);
        return p;
    }

    public static JTextArea textArea(int rows, int cols) {
        JTextArea a = new JTextArea(rows, cols);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static <T extends JComponent> T name(T c, String accessibleName) {
        c.getAccessibleContext().setAccessibleName(accessibleName);
        c.setToolTipText(accessibleName);
        return c;
    }

    public static JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(6, 2));
        JLabel l = new JLabel(label);
        l.setLabelFor(field);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        name(field, label);
        return p;
    }

    public static JTable table(List<String> header, List<List<String>> rows) {
        DefaultTableModel m = new DefaultTableModel(header.toArray(), 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (List<String> r : rows) m.addRow(r.toArray());
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(true);
        t.getAccessibleContext().setAccessibleName("Table");
        t.setRowHeight(Math.max(20, t.getFontMetrics(t.getFont()).getHeight() + 6));
        return t;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Study Ta Bai", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Study Ta Bai", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Runs slow work (AI, file parsing) off the Swing thread, then shows errors or hands the result back on the Swing thread. */
    public static <T> void background(Component parent, Callable<T> work, Consumer<T> onDone) {
        background(parent, work, onDone, ex -> error(parent, ex.getMessage() == null ? "Something went wrong." : ex.getMessage()));
    }

    public static <T> void background(Component parent, Callable<T> work, Consumer<T> onDone, Consumer<Throwable> onError) {
        Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (w != null) w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                if (w != null) w.setCursor(Cursor.getDefaultCursor());
                try {
                    onDone.accept(get());
                } catch (ExecutionException e) {
                    onError.accept(e.getCause() == null ? e : e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
