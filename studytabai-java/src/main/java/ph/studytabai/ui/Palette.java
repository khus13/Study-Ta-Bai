package ph.studytabai.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/** Light, tranquil Filipino-province palette (sky blues, leaf greens, earthy browns) plus small drawing helpers. */
public final class Palette {
    // sky & sea
    public static final Color SKY_TOP = new Color(0x5DB4E6);
    public static final Color SKY_LOW = new Color(0xCDEBF6);
    public static final Color SKY_BLUE = new Color(0x3C9BD1);
    public static final Color SEA = new Color(0x34B0C4);
    public static final Color SEA_DEEP = new Color(0x1F86A6);
    public static final Color SUN = new Color(0xFFCB3D);
    // greens
    public static final Color LEAF = new Color(0x2F7D4B);
    public static final Color LEAF_DARK = new Color(0x1F5A36);
    public static final Color LEAF_LIGHT = new Color(0x8FCB6B);
    public static final Color PADDY = new Color(0x9ED45C);
    // earth
    public static final Color WOOD = new Color(0x8A5A33);
    public static final Color WOOD_DARK = new Color(0x5B3A1F);
    public static final Color WOOD_LIGHT = new Color(0xC18A52);
    public static final Color BAMBOO = new Color(0xDDB872);
    public static final Color SAND = new Color(0xEBD9AE);
    public static final Color CREAM = new Color(0xF6EEDA);
    public static final Color PAPER = new Color(0xFFFCF2);
    public static final Color CLAY = new Color(0xC8553D);
    // text
    public static final Color TEXT = new Color(0x3A2814);
    public static final Color MUTED = new Color(0x7B6A52);
    public static final Color LINE = new Color(0xC9B68F);

    private Palette() {}

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Soft radial light/shade blob. */
    public static void spot(Graphics2D g, double cx, double cy, double r, Color c, int peakAlpha) {
        if (r <= 0) return;
        RadialGradientPaint p = new RadialGradientPaint((float) cx, (float) cy, (float) r,
                new float[]{0f, 1f}, new Color[]{alpha(c, peakAlpha), alpha(c, 0)});
        Paint old = g.getPaint();
        g.setPaint(p);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setPaint(old);
    }

    /** Sign-painter lettering: dark outline, solid fill. {@code (x, y)} is the baseline start. */
    public static void outlineText(Graphics2D g, String s, Font f, float x, float y, Color fill, Color outline, float r) {
        if (s == null || s.isEmpty()) return;
        g.setColor(outline);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            Fonts.draw(g, s, f, x + (float) (Math.cos(a) * r), y + (float) (Math.sin(a) * r));
        }
        g.setColor(fill);
        Fonts.draw(g, s, f, x, y);
    }

    public static void outlineTextCentered(Graphics2D g, String s, Font f, float cx, float y, Color fill, Color outline, float r) {
        outlineText(g, s, f, cx - Fonts.width(s, f) / 2f, y, fill, outline, r);
    }

    /** Text with a soft drop shadow. */
    public static void shadowText(Graphics2D g, String s, Font f, float x, float y, Color fill, Color shadow, float dy) {
        g.setColor(shadow);
        Fonts.draw(g, s, f, x, y + dy);
        g.setColor(fill);
        Fonts.draw(g, s, f, x, y);
    }
}
