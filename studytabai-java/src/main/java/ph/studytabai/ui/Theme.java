package ph.studytabai.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Filipino-province theme: light cream surfaces, leaf green, wood brown and sky blue, a slanted serif for
 * titles and a sturdy sign-painter sans for buttons/tabs. Keeps the accessibility settings (high contrast,
 * adjustable font size).
 */
public final class Theme {
    private static final Map<Object, Font> ORIGINAL = new HashMap<>();
    private static boolean highContrast;
    private static double scale = 1.0;

    private Theme() {}

    /** Metal colour theme so every stock widget (checkboxes, spinners, combos, dialogs) takes the light earthy palette. */
    private static final class ProvinceTheme extends DefaultMetalTheme {
        private final boolean hc;
        ProvinceTheme(boolean hc) { this.hc = hc; }
        @Override public String getName() { return hc ? "Study Ta Bai High Contrast" : "Study Ta Bai Province"; }
        @Override protected ColorUIResource getPrimary1() { return hc ? new ColorUIResource(Color.YELLOW) : new ColorUIResource(Palette.LEAF_DARK); }
        @Override protected ColorUIResource getPrimary2() { return hc ? new ColorUIResource(Color.YELLOW) : new ColorUIResource(Palette.WOOD); }
        @Override protected ColorUIResource getPrimary3() { return hc ? new ColorUIResource(Color.YELLOW) : new ColorUIResource(Palette.LEAF); }
        @Override protected ColorUIResource getSecondary1() { return hc ? new ColorUIResource(Color.WHITE) : new ColorUIResource(Palette.LINE); }
        @Override protected ColorUIResource getSecondary2() { return hc ? new ColorUIResource(Color.GRAY) : new ColorUIResource(Palette.WOOD_LIGHT); }
        @Override protected ColorUIResource getSecondary3() { return hc ? new ColorUIResource(Color.BLACK) : new ColorUIResource(Palette.CREAM); }
        @Override protected ColorUIResource getBlack() { return hc ? new ColorUIResource(Color.WHITE) : new ColorUIResource(Palette.TEXT); }
        @Override protected ColorUIResource getWhite() { return hc ? new ColorUIResource(Color.BLACK) : new ColorUIResource(Palette.PAPER); }
    }

    /** Field outline: thin wooden edge that turns leaf-green on focus. */
    private static final class FieldBorder extends AbstractBorder {
        @Override public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            Palette.aa(g);
            Color col = highContrast ? Color.WHITE : (!c.isEnabled() ? Palette.LINE : (c instanceof JComponent jc && jc.hasFocus() ? Palette.LEAF : Palette.WOOD_LIGHT));
            g.setColor(col);
            g.setStroke(new BasicStroke(1.4f));
            g.drawRoundRect(x, y, w - 1, h - 1, 6, 6);
            g.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(5, 8, 5, 8); }
    }

    public static void install() {
        UIManager.put("ButtonUI", WoodButtonUI.class.getName());
        UIManager.put("TabbedPaneUI", WoodTabbedPaneUI.class.getName());
        UIManager.put("ScrollBarUI", LeafScrollBarUI.class.getName());
        MetalLookAndFeel.setCurrentTheme(new ProvinceTheme(false));
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) { }
        ORIGINAL.clear();
        UIDefaults defs = UIManager.getLookAndFeelDefaults();
        for (Object k : Collections.list(defs.keys())) {
            Object v = defs.get(k);
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
        MetalLookAndFeel.setCurrentTheme(new ProvinceTheme(highContrast));
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) { }

        // ---- fonts: system UI font for body text, poster font for buttons / tabs / table headers
        for (Map.Entry<Object, Font> e : ORIGINAL.entrySet())
            UIManager.put(e.getKey(), new FontUIResource(Fonts.body(Font.PLAIN, (float) (e.getValue().getSize2D() * 1.15 * scale))));
        float disp = (float) (12 * 1.15 * 1.02 * scale);
        UIManager.put("Button.font", new FontUIResource(Fonts.sign(disp)));
        UIManager.put("ToggleButton.font", new FontUIResource(Fonts.sign(disp)));
        UIManager.put("TabbedPane.font", new FontUIResource(Fonts.sign(disp)));
        UIManager.put("TableHeader.font", new FontUIResource(Fonts.sign((float) (12 * 1.15 * 0.95 * scale))));
        UIManager.put("TitledBorder.font", new FontUIResource(Fonts.display((float) (12 * 1.15 * 1.3 * scale))));

        Color bg, fg, field, fieldFg, btn, btnFg, sel, selFg, accent, grid, muted;
        if (highContrast) {
            bg = Color.BLACK; fg = Color.WHITE; field = Color.BLACK; fieldFg = Color.WHITE;
            btn = Color.YELLOW; btnFg = Color.BLACK; sel = Color.YELLOW; selFg = Color.BLACK;
            accent = Color.YELLOW; grid = Color.WHITE; muted = Color.LIGHT_GRAY;
        } else {
            bg = Palette.CREAM; fg = Palette.TEXT; field = Palette.PAPER; fieldFg = Palette.TEXT;
            btn = Palette.WOOD; btnFg = Color.WHITE; sel = Palette.LEAF; selFg = Color.WHITE;
            accent = Palette.WOOD; grid = Palette.LINE; muted = Palette.MUTED;
        }
        for (String k : new String[]{"Panel", "OptionPane", "ScrollPane", "Viewport", "TabbedPane", "CheckBox", "RadioButton",
                "SplitPane", "Label", "Spinner", "ColorChooser"}) {
            color(k + ".background", bg);
            color(k + ".foreground", fg);
        }
        for (String k : new String[]{"MenuBar", "Menu", "MenuItem", "CheckBoxMenuItem", "PopupMenu"}) {
            color(k + ".background", highContrast ? Color.BLACK : new Color(0xFBF4E0));
            color(k + ".foreground", fg);
            color(k + ".selectionBackground", sel);
            color(k + ".selectionForeground", selFg);
        }
        color("MenuItem.acceleratorForeground", muted);
        color("MenuItem.acceleratorSelectionForeground", selFg);
        UIManager.put("MenuBar.border", new BorderUIResource(BorderFactory.createMatteBorder(0, 0, 2, 0, accent)));
        UIManager.put("PopupMenu.border", new BorderUIResource(BorderFactory.createLineBorder(accent, 1)));
        color("OptionPane.messageForeground", fg);
        color("TabbedPane.selected", bg);
        color("TabbedPane.contentAreaColor", bg);
        color("TabbedPane.light", bg);
        color("TabbedPane.shadow", grid);
        color("TabbedPane.darkShadow", grid);
        color("TabbedPane.highlight", bg);
        color("TabbedPane.focus", accent);
        color("TabbedPane.borderHightlightColor", bg);
        for (String k : new String[]{"TextField", "TextArea", "TextPane", "PasswordField", "FormattedTextField", "List", "Table", "ComboBox", "EditorPane"}) {
            color(k + ".background", field);
            color(k + ".foreground", fieldFg);
            color(k + ".selectionBackground", sel);
            color(k + ".selectionForeground", selFg);
        }
        color("TextField.inactiveBackground", bg);
        color("TextField.inactiveForeground", muted);
        color("TextArea.inactiveForeground", muted);
        color("TextField.caretForeground", highContrast ? Color.WHITE : Palette.LEAF);
        color("TextArea.caretForeground", highContrast ? Color.WHITE : Palette.LEAF);
        color("PasswordField.caretForeground", highContrast ? Color.WHITE : Palette.LEAF);
        for (String k : new String[]{"TextField", "PasswordField", "FormattedTextField"})
            UIManager.put(k + ".border", new BorderUIResource(new FieldBorder()));
        UIManager.put("TextArea.border", new BorderUIResource(BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        UIManager.put("ScrollPane.border", new BorderUIResource(BorderFactory.createLineBorder(highContrast ? Color.WHITE : Palette.WOOD_LIGHT, 1)));
        UIManager.put("Table.scrollPaneBorder", new BorderUIResource(BorderFactory.createLineBorder(highContrast ? Color.WHITE : Palette.WOOD_LIGHT, 1)));
        color("Table.gridColor", grid);
        color("Table.focusCellBackground", field);
        color("TableHeader.background", highContrast ? Color.YELLOW : Palette.WOOD);
        color("TableHeader.foreground", highContrast ? Color.BLACK : new Color(0xFFF5DC));
        color("ComboBox.buttonBackground", highContrast ? Color.YELLOW : Palette.WOOD);
        color("ComboBox.buttonDarkShadow", accent);
        color("ComboBox.buttonHighlight", accent);
        color("ComboBox.buttonShadow", accent);
        color("ComboBox.disabledBackground", bg);
        color("ComboBox.disabledForeground", muted);
        for (String k : new String[]{"Button", "ToggleButton"}) {
            color(k + ".background", btn);
            color(k + ".foreground", btnFg);
            color(k + ".select", sel);
            color(k + ".focus", accent);
        }
        color("Button.disabledText", muted);
        color("CheckBox.focus", accent);
        color("RadioButton.focus", accent);
        color("TitledBorder.titleColor", highContrast ? Color.WHITE : Palette.LEAF);
        color("Separator.foreground", grid);
        color("Separator.background", bg);
        color("ProgressBar.foreground", accent);
        color("ProgressBar.background", field);
        color("ToolTip.background", highContrast ? Color.BLACK : new Color(0xFFF6DA));
        color("ToolTip.foreground", fg);
        UIManager.put("ToolTip.border", new BorderUIResource(BorderFactory.createLineBorder(accent, 1)));
        UIManager.put("Button.disabledToolBarBorderBackground", new ColorUIResource(bg));
        UIManager.put("swing.boldMetal", Boolean.FALSE);
    }

    /** Result colours: leaf green for right, clay red for wrong (dark enough for the light background). */
    public static Color feedbackColor(boolean good) {
        if (highContrast) return good ? new Color(120, 255, 120) : new Color(255, 140, 140);
        return good ? new Color(0x1F7A3F) : new Color(0xB8392F);
    }

    /** Thin wooden outline for panels. */
    public static Border woodLine() {
        return BorderFactory.createLineBorder(highContrast ? Color.WHITE : Palette.WOOD_LIGHT, 1);
    }
}
