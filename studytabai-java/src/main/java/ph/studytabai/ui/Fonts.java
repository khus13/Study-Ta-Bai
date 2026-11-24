package ph.studytabai.ui;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.io.InputStream;

/**
 * Fonts for the Filipino-province look.
 * <ul>
 *   <li><b>Display</b>: Crimson Text Bold Italic (bundled, SIL OFL), a traditional slanted serif like hand-painted
 *       signboards and old fiesta posters. Used for titles and headings.</li>
 *   <li><b>Sign</b>: Fira Sans ExtraBold (bundled, SIL OFL), a sturdy sans like the lettering on sari-sari stores
 *       and jeepneys. Used for buttons, tabs and table headers.</li>
 *   <li><b>Body</b>: the system UI font, so Filipino text and emoji always render.</li>
 * </ul>
 * Bundled fonts have no glyph fallback, so {@link #draw} and {@link #width} hand glyphs a font cannot show
 * (emoji, arrows) to the system font.
 */
public final class Fonts {
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static final Font SERIF = load("/fonts/CrimsonText-BoldItalic.ttf");
    private static final Font SIGN = load("/fonts/FiraSans-ExtraBold.ttf");

    private Fonts() {}

    private static Font load(String resource) {
        try (InputStream in = Fonts.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f;
        } catch (Exception e) {
            return null;   // fall back to a system face; the app still works
        }
    }

    /** Slanted serif for titles and headings. */
    public static Font display(float size) {
        if (SERIF != null) return SERIF.deriveFont(Font.PLAIN, size);
        return new Font(Font.SERIF, Font.BOLD | Font.ITALIC, Math.round(size)).deriveFont(size);
    }

    /** Heavy sans for buttons, tabs and sign lettering. */
    public static Font sign(float size) {
        if (SIGN != null) return SIGN.deriveFont(Font.PLAIN, size);
        return new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size)).deriveFont(Font.BOLD, size);
    }

    /** System UI font. Always has the glyphs. */
    public static Font body(int style, float size) {
        return new Font(Font.SANS_SERIF, style, Math.round(size)).deriveFont(style, size);
    }

    private static boolean shows(Font f, int cp) {
        return Character.isWhitespace(cp) || f.canDisplay(cp);
    }

    public static float width(String s, Font f) {
        Font fb = body(Font.BOLD, f.getSize2D());
        float w = 0;
        int i = 0;
        while (i < s.length()) {
            boolean ok = shows(f, s.codePointAt(i));
            int j = i;
            while (j < s.length()) {
                int cp = s.codePointAt(j);
                if (shows(f, cp) != ok) break;
                j += Character.charCount(cp);
            }
            w += (float) (ok ? f : fb).getStringBounds(s, i, j, FRC).getWidth();
            i = j;
        }
        return w;
    }

    public static void draw(Graphics2D g, String s, Font f, float x, float y) {
        Font fb = body(Font.BOLD, f.getSize2D());
        Font old = g.getFont();
        float cx = x;
        int i = 0;
        while (i < s.length()) {
            boolean ok = shows(f, s.codePointAt(i));
            int j = i;
            while (j < s.length()) {
                int cp = s.codePointAt(j);
                if (shows(f, cp) != ok) break;
                j += Character.charCount(cp);
            }
            String run = s.substring(i, j);
            Font use = ok ? f : fb;
            g.setFont(use);
            g.drawString(run, cx, y);
            cx += (float) use.getStringBounds(run, FRC).getWidth();
            i = j;
        }
        g.setFont(old);
    }
}
