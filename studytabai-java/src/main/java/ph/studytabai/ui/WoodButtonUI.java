package ph.studytabai.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/** Slanted painted-wood signboard buttons. They turn leaf-green on hover/press. */
public class WoodButtonUI extends BasicButtonUI {
    private static final int PAD = 5;

    public static ComponentUI createUI(JComponent c) {
        // Only real push buttons get the signboard look. Internal buttons (combo-box and spinner arrows) keep the stock Metal look.
        if (c.getClass() != JButton.class) return javax.swing.plaf.metal.MetalButtonUI.createUI(c);
        return new WoodButtonUI();
    }

    @Override public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(8 + PAD, 22 + PAD, 8 + PAD, 22 + PAD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override public Dimension getPreferredSize(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Font f = b.getFont();
        FontMetrics fm = b.getFontMetrics(f);
        Insets in = b.getInsets();
        String t = b.getText() == null ? "" : b.getText();
        int iw = b.getIcon() == null ? 0 : b.getIcon().getIconWidth() + 6;
        return new Dimension((int) Math.ceil(Fonts.width(t, f)) + iw + in.left + in.right + (int) (fm.getHeight() * 0.3), fm.getHeight() + in.top + in.bottom);
    }

    @Override public void paint(Graphics g0, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g = (Graphics2D) g0.create();
        Palette.aa(g);
        boolean hc = Theme.isHighContrast();
        ButtonModel m = b.getModel();
        boolean on = b.isEnabled();
        boolean hot = on && (m.isRollover() || m.isArmed() || m.isPressed() || b.hasFocus());
        boolean press = on && m.isArmed() && m.isPressed();
        int x = PAD, y = PAD, w = c.getWidth() - PAD * 2 - 1, h = c.getHeight() - PAD * 2 - 1 - (press ? 0 : 0);
        double slant = h * 0.22;
        Path2D shape = new Path2D.Double();
        shape.moveTo(x + slant, y);
        shape.lineTo(x + w, y);
        shape.lineTo(x + w - slant, y + h);
        shape.lineTo(x, y + h);
        shape.closePath();

        Color textColor, textShadow = null;
        if (hc) {
            g.setColor(hot ? Color.YELLOW : Color.BLACK);
            g.fill(shape);
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(shape);
            textColor = hot ? Color.BLACK : Color.YELLOW;
        } else {
            int dy = press ? 1 : 0;
            g.translate(0, dy);
            // soft shadow under the board
            if (on) {
                g.setColor(Palette.alpha(Palette.WOOD_DARK, 55));
                g.translate(1.5, 3);
                g.fill(shape);
                g.translate(-1.5, -3);
            }
            Color top, bot, edge;
            if (!on) { top = new Color(0xE2D6B8); bot = new Color(0xD1C29C); edge = new Color(0xB4A57F); textColor = Palette.MUTED; }
            else if (hot) { top = new Color(0x5DB35E); bot = new Color(0x2F7D4B); edge = Palette.LEAF_DARK; textColor = Color.WHITE; textShadow = Palette.alpha(Palette.LEAF_DARK, 200); }
            else { top = new Color(0xC98F57); bot = new Color(0x93602F); edge = Palette.WOOD_DARK; textColor = new Color(0xFFF5DC); textShadow = Palette.alpha(Palette.WOOD_DARK, 190); }
            g.setPaint(new GradientPaint(0, y, top, 0, y + h, bot));
            g.fill(shape);
            // wood grain
            Shape oc = g.getClip();
            g.clip(shape);
            g.setColor(Palette.alpha(edge, 55));
            g.setStroke(new BasicStroke(1f));
            for (int i = 1; i < 4; i++) g.draw(new Line2D.Double(x, y + h * i / 4.0 + (i % 2), x + w, y + h * i / 4.0 - (i % 2)));
            g.setColor(Palette.alpha(Color.WHITE, 55));
            g.fill(new java.awt.geom.Rectangle2D.Double(x, y, w, h * 0.12));
            g.setClip(oc);
            g.setColor(edge);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shape);
            g.translate(0, -dy);
            g.translate(0, dy);
            if (hot) { /* label is drawn below with the same offset */ }
        }
        // label
        Font f = b.getFont();
        FontMetrics fm = g.getFontMetrics(f);
        String t = b.getText() == null ? "" : b.getText();
        float tw = Fonts.width(t, f);
        int iw = b.getIcon() == null ? 0 : b.getIcon().getIconWidth() + 6;
        float tx = x + (w - tw - iw) / 2f;
        float ty = y + (h - fm.getHeight()) / 2f + fm.getAscent();
        if (b.getIcon() != null) { b.getIcon().paintIcon(b, g, Math.round(tx), y + (h - b.getIcon().getIconHeight()) / 2); tx += iw; }
        if (textShadow != null) { g.setColor(textShadow); Fonts.draw(g, t, f, tx, ty + 1.5f); }
        g.setColor(textColor);
        Fonts.draw(g, t, f, tx, ty);
        g.dispose();
    }
}
