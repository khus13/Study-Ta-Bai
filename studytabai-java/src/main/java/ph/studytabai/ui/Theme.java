package ph.studytabai.ui;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Playful default theme plus accessibility settings (high contrast, adjustable font size). */
public final class Theme {
    private static final Map<Object, Font> ORIGINAL = new HashMap<>();
    private static boolean highContrast;
    private static double scale = 1.0;

    private Theme() {}

    public static void install() {
        try {
            UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) { }
        ORIGINAL.clear();
        for (Object k : Collections.list(UIManager.getDefaults().keys())) {
            Object v = UIManager.get(k);
            if (v instanceof Font f) ORIGINAL.put(k, f);
        }
        apply();
    }

    public static boolean isHighContrast() { return highContrast; }
    public static double getScale() { return scale; }

    public static void setHighContrast(boolean on) { highContrast = on; apply(); refresh(); }

    public static void setScale(double s) { scale = Math.max(0.8, Math.min(2.0, s)); apply(); refresh(); }

    public static void set(boolean contrast, double s) {
        highContrast = contrast;
        scale = Math.max(0.8, Math.min(2.0, s));
        apply();
        refresh();
    }

    private static void refresh() {
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            w.revalidate();
            w.repaint();
        }
    }

    private static void color(String key, Color c) { UIManager.put(key, new ColorUIResource(c)); }

    private static void apply() {
        for (Map.Entry<Object, Font> e : ORIGINAL.entrySet())
            UIManager.put(e.getKey(), new FontUIResource(e.getValue().deriveFont(Font.PLAIN, (float) (e.getValue().getSize2D() * 1.15 * scale))));

        Color bg, fg, field, fieldFg, btn, btnFg, sel, selFg, accent, grid;
        if (highContrast) {
            bg = Color.BLACK; fg = Color.WHITE; field = Color.BLACK; fieldFg = Color.WHITE;
            btn = new Color(255, 235, 0); btnFg = Color.BLACK; sel = new Color(255, 235, 0); selFg = Color.BLACK;
            accent = new Color(255, 235, 0); grid = Color.WHITE;
        } else {
            bg = new Color(255, 248, 236); fg = new Color(31, 35, 64); field = Color.WHITE; fieldFg = fg;
            btn = new Color(91, 61, 245); btnFg = Color.WHITE; sel = new Color(255, 183, 3); selFg = fg;
            accent = new Color(91, 61, 245); grid = new Color(215, 210, 196);
        }
        for (String k : new String[]{"Panel", "OptionPane", "ScrollPane", "Viewport", "TabbedPane", "CheckBox", "RadioButton",
                "SplitPane", "MenuBar", "Menu", "MenuItem", "CheckBoxMenuItem", "Label", "Spinner", "ToolTip"}) {
            color(k + ".background", bg);
            color(k + ".foreground", fg);
        }
        color("OptionPane.messageForeground", fg);
        color("TabbedPane.selected", highContrast ? new Color(60, 60, 0) : Color.WHITE);
        color("TabbedPane.contentAreaColor", bg);
        color("TabbedPane.light", bg);
        color("TabbedPane.shadow", grid);
        color("TabbedPane.darkShadow", grid);
        color("TabbedPane.highlight", bg);
        color("TabbedPane.focus", accent);
        for (String k : new String[]{"TextField", "TextArea", "TextPane", "PasswordField", "FormattedTextField", "List", "Table", "ComboBox"}) {
            color(k + ".background", field);
            color(k + ".foreground", fieldFg);
            color(k + ".selectionBackground", sel);
            color(k + ".selectionForeground", selFg);
        }
        color("TextField.caretForeground", fieldFg);
        color("TextArea.caretForeground", fieldFg);
        color("PasswordField.caretForeground", fieldFg);
        color("Table.gridColor", grid);
        color("TableHeader.background", btn);
        color("TableHeader.foreground", btnFg);
        for (String k : new String[]{"Button", "ToggleButton"}) {
            color(k + ".background", btn);
            color(k + ".foreground", btnFg);
            color(k + ".select", sel);
            color(k + ".focus", highContrast ? Color.WHITE : accent);
        }
        color("Button.disabledText", highContrast ? Color.LIGHT_GRAY : new Color(220, 215, 240));
        color("TitledBorder.titleColor", fg);
        color("Separator.foreground", grid);
        color("ProgressBar.foreground", accent);
        UIManager.put("Button.disabledToolBarBorderBackground", new ColorUIResource(bg));
        UIManager.put("swing.boldMetal", Boolean.FALSE);
    }

    public static Color feedbackColor(boolean good) {
        if (highContrast) return good ? new Color(120, 255, 120) : new Color(255, 140, 140);
        return good ? new Color(11, 107, 58) : new Color(179, 38, 30);
    }
}
