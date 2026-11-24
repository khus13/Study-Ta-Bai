package ph.studytabai.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/** Tabs as slanted wooden signboards. The selected tab turns leaf-green. */
public class WoodTabbedPaneUI extends BasicTabbedPaneUI {
    public static ComponentUI createUI(JComponent c) { return new WoodTabbedPaneUI(); }

    @Override protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(7, 20, 7, 20);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(8, 6, 0, 6);
        contentBorderInsets = new Insets(4, 0, 0, 0);
        tabPane.setOpaque(false);
    }

    @Override protected int getTabLabelShiftX(int p, int i, boolean sel) { return 0; }
    @Override protected int getTabLabelShiftY(int p, int i, boolean sel) { return 0; }
    @Override protected void paintFocusIndicator(Graphics g, int p, Rectangle[] r, int i, Rectangle ir, Rectangle tr, boolean sel) { }

    private static Path2D plate(int x, int y, int w, int h) {
        double s = h * 0.28;
        Path2D p = new Path2D.Double();
        p.moveTo(x + s, y);
        p.lineTo(x + w, y);
        p.lineTo(x + w - s, y + h);
        p.lineTo(x, y + h);
        p.closePath();
        return p;
    }

    @Override protected void paintTabBackground(Graphics g0, int tp, int i, int x, int y, int w, int h, boolean sel) {
        Graphics2D g = (Graphics2D) g0.create();
        Palette.aa(g);
        Path2D p = plate(x + 2, y + 2, w - 5, h - 2);
        if (Theme.isHighContrast()) {
            g.setColor(sel ? Color.YELLOW : Color.BLACK);
            g.fill(p);
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2f));
            g.draw(p);
        } else if (sel) {
            g.setPaint(new GradientPaint(0, y, new Color(0x5DB35E), 0, y + h, Palette.LEAF));
            g.fill(p);
            g.setColor(Palette.LEAF_DARK);
            g.setStroke(new BasicStroke(2f));
            g.draw(p);
        } else {
            g.setPaint(new GradientPaint(0, y, new Color(0xE2C38C), 0, y + h, new Color(0xC79C5F)));
            g.fill(p);
            g.setColor(Palette.alpha(Palette.WOOD_DARK, 70));
            Shape oc = g.getClip();
            g.clip(p);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(x, y + h * 0.5, x + w, y + h * 0.5 - 1));
            g.setClip(oc);
            g.setColor(Palette.WOOD);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(p);
        }
        g.dispose();
    }

    @Override protected void paintTabBorder(Graphics g, int tp, int i, int x, int y, int w, int h, boolean sel) { }

    @Override protected void paintText(Graphics g0, int tp, Font font, FontMetrics fm, int i, String title, Rectangle tr, boolean sel) {
        Graphics2D g = (Graphics2D) g0.create();
        Palette.aa(g);
        boolean on = tabPane.isEnabledAt(i);
        Color c = Theme.isHighContrast() ? (sel ? Color.BLACK : Color.YELLOW) : (sel ? Color.WHITE : (on ? Palette.WOOD_DARK : Palette.MUTED));
        if (sel && !Theme.isHighContrast()) {
            g.setColor(Palette.alpha(Palette.LEAF_DARK, 190));
            Fonts.draw(g, title, font, tr.x, tr.y + fm.getAscent() + 1.5f);
        }
        g.setColor(c);
        Fonts.draw(g, title, font, tr.x, tr.y + fm.getAscent());
        g.dispose();
    }

    @Override protected void paintContentBorder(Graphics g0, int tp, int sel) {
        Graphics2D g = (Graphics2D) g0.create();
        Palette.aa(g);
        Insets ins = tabPane.getInsets();
        int y = calculateTabAreaHeight(tp, runCount, maxTabHeight) + ins.top;
        g.setColor(Theme.isHighContrast() ? Color.YELLOW : Palette.WOOD);
        g.fillRect(ins.left, y, tabPane.getWidth() - ins.left - ins.right, 4);
        if (!Theme.isHighContrast()) {
            g.setColor(Palette.alpha(Color.WHITE, 90));
            g.fillRect(ins.left, y, tabPane.getWidth() - ins.left - ins.right, 1);
        }
        g.dispose();
    }
}
