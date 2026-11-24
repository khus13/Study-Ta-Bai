package ph.studytabai.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Cream paper panel in a wooden frame with clipped corners and little nail heads. Lets the scene show faintly through. */
public class WoodCard extends JPanel {
    public WoodCard(LayoutManager lm) {
        super(lm);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 30, 24, 30));
    }

    static Path2D chamfer(double x, double y, double w, double h, double c) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + c, y);
        p.lineTo(x + w, y);
        p.lineTo(x + w, y + h - c);
        p.lineTo(x + w - c, y + h);
        p.lineTo(x, y + h);
        p.lineTo(x, y + c);
        p.closePath();
        return p;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Palette.aa(g);
        int w = getWidth() - 14, h = getHeight() - 14;
        Path2D s = chamfer(7, 7, w, h, 18);
        if (Theme.isHighContrast()) {
            g.setColor(Color.BLACK);
            g.fill(s);
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(s);
        } else {
            g.setColor(Palette.alpha(Palette.WOOD_DARK, 60));
            g.translate(2, 5);
            g.fill(s);
            g.translate(-2, -5);
            g.setPaint(new GradientPaint(0, 0, new Color(0xFF, 0xFC, 0xF0, 242), 0, getHeight(), new Color(0xF4, 0xE9, 0xCF, 245)));
            g.fill(s);
            g.setColor(Palette.WOOD);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g.draw(s);
            g.setColor(new Color(0xD7A869));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(chamfer(10.5, 10.5, w - 7, h - 7, 15));
            g.setColor(Palette.WOOD_DARK);
            for (double[] n : new double[][]{{26, 12}, {7 + w - 8, 14}, {12, 7 + h - 8}, {7 + w - 20, 7 + h - 3}})
                g.fill(new Ellipse2D.Double(n[0] - 2.5, n[1] - 2.5, 5, 5));
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
