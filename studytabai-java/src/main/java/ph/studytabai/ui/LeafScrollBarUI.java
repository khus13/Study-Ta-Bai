package ph.studytabai.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/** Slim sand-coloured track with a rounded wooden/leaf thumb, no arrow buttons. */
public class LeafScrollBarUI extends BasicScrollBarUI {
    public static ComponentUI createUI(JComponent c) { return new LeafScrollBarUI(); }

    @Override protected JButton createDecreaseButton(int o) { return zero(); }
    @Override protected JButton createIncreaseButton(int o) { return zero(); }

    private static JButton zero() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == Adjustable.VERTICAL ? new Dimension(13, 48) : new Dimension(48, 13);
    }

    @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(Theme.isHighContrast() ? Color.BLACK : new Color(0xEADFC3));
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g = (Graphics2D) g0.create();
        Palette.aa(g);
        Color col = Theme.isHighContrast() ? Color.YELLOW : (isThumbRollover() || isDragging ? Palette.LEAF : Palette.WOOD);
        g.setColor(col);
        g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
        if (!Theme.isHighContrast()) {
            g.setColor(Palette.alpha(Color.WHITE, 70));
            g.fillRoundRect(r.x + 3, r.y + 3, Math.max(1, (r.width - 6) / 3), r.height - 6, 6, 6);
        }
        g.dispose();
    }
}
