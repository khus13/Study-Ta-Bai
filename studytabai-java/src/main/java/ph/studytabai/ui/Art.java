package ph.studytabai.ui;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

/** Procedural Filipino-province art: all original, drawn with Java2D (no image files). */
final class Art {
    private Art() {}

    private static final Color SKIN = new Color(0xC98F63);
    private static final Color SKIN_DARK = new Color(0xA8714A);
    private static final Color HAT = new Color(0xE3C072);
    private static final Color HAT_DARK = new Color(0xA98440);

    // ------------------------------------------------------------------ sky

    static void sun(Graphics2D g, double cx, double cy, double r, double t) {
        Palette.spot(g, cx, cy, r * 4.2, new Color(0xFFF1B0), 150);
        Palette.spot(g, cx, cy, r * 2.2, new Color(0xFFE27A), 170);
        Stroke old = g.getStroke();
        g.setColor(Palette.alpha(new Color(0xFFE58A), 120));
        g.setStroke(new BasicStroke((float) Math.max(1.5, r * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8 + t * 0.12;
            double r1 = r * 1.35, r2 = r * (1.75 + 0.25 * Math.sin(t * 1.5 + i * 1.3));
            g.draw(new Line2D.Double(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1, cx + Math.cos(a) * r2, cy + Math.sin(a) * r2));
        }
        g.setStroke(old);
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r, new float[]{0f, 0.8f, 1f},
                new Color[]{new Color(0xFFFBE0), new Color(0xFFD84F), new Color(0xFFC230)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    static void cloud(Graphics2D g, double cx, double cy, double w, int alpha) {
        Area a = new Area(new Ellipse2D.Double(cx - w * 0.5, cy - w * 0.07, w, w * 0.26));
        a.add(new Area(new Ellipse2D.Double(cx - w * 0.44, cy - w * 0.17, w * 0.30, w * 0.30)));
        a.add(new Area(new Ellipse2D.Double(cx - w * 0.26, cy - w * 0.30, w * 0.40, w * 0.40)));
        a.add(new Area(new Ellipse2D.Double(cx - w * 0.02, cy - w * 0.22, w * 0.34, w * 0.34)));
        a.add(new Area(new Ellipse2D.Double(cx + w * 0.16, cy - w * 0.12, w * 0.28, w * 0.28)));
        g.setPaint(new GradientPaint(0, (float) (cy - w * 0.30), new Color(255, 255, 255, alpha),
                0, (float) (cy + w * 0.19), new Color(214, 232, 244, alpha)));
        g.fill(a);
    }

    // ------------------------------------------------------------------ land

    /** Rolling tropical hills/mountains between x0 and x1 with their foot at baseY. */
    static void mountains(Graphics2D g, double x0, double x1, double baseY, double hMin, double hMax,
                          Color top, Color bottom, long seed, double rampLeft) {
        Random r = new Random(seed);
        double p1 = r.nextDouble() * 6, p2 = r.nextDouble() * 6, p3 = r.nextDouble() * 6;
        double f1 = 0.0035 + r.nextDouble() * 0.002, f2 = f1 * 2.3, f3 = f1 * 5.1;
        Path2D p = new Path2D.Double();
        p.moveTo(x0, baseY);
        for (double x = x0; x <= x1; x += 4) {
            double n = 0.5 + 0.34 * Math.sin(x * f1 + p1) + 0.18 * Math.sin(x * f2 + p2) + 0.08 * Math.sin(x * f3 + p3);
            n = Math.max(0.05, Math.min(1, n));
            double ramp = rampLeft > 0 ? Math.max(0, Math.min(1, (x - x0) / rampLeft)) : 1;
            p.lineTo(x, baseY - (hMin + (hMax - hMin) * n) * ramp);
        }
        p.lineTo(x1, baseY);
        p.closePath();
        g.setPaint(new GradientPaint(0, (float) (baseY - hMax), top, 0, (float) baseY, bottom));
        g.fill(p);
        // gentle ridge shading
        g.setColor(Palette.alpha(Palette.LEAF_DARK, 28));
        g.setStroke(new BasicStroke(1.2f));
        Rectangle2D clipB = p.getBounds2D();
        Shape oc = g.getClip();
        g.clip(p);
        for (double x = x0; x < x1; x += 22 + r.nextInt(22))
            g.draw(new Line2D.Double(x, baseY - hMax, x - 40 - r.nextInt(40), baseY));
        g.setClip(oc);
    }

    /** Terrace band with a wavy upper edge. */
    static void terrace(Graphics2D g, double x0, double x1, double topY, double botY, double wave, double phase,
                        Color c1, Color c2, boolean water, Random r) {
        Path2D p = new Path2D.Double();
        p.moveTo(x0, botY);
        for (double x = x0; x <= x1; x += 6)
            p.lineTo(x, topY + Math.sin(x * 0.011 + phase) * wave + Math.sin(x * 0.027 + phase * 2) * wave * 0.4);
        p.lineTo(x1, botY);
        p.closePath();
        g.setPaint(new GradientPaint(0, (float) (topY - wave), c1, 0, (float) botY, c2));
        g.fill(p);
        Shape oc = g.getClip();
        g.clip(p);
        if (water) {
            g.setColor(Palette.alpha(Color.WHITE, 70));
            for (double x = x0; x < x1; x += 40 + r.nextInt(50))
                g.fill(new Ellipse2D.Double(x, topY + (botY - topY) * (0.2 + 0.5 * r.nextDouble()), 30 + r.nextInt(40), 3));
        } else {
            g.setColor(Palette.alpha(Palette.LEAF_DARK, 55));
            g.setStroke(new BasicStroke(1f));
            for (double x = x0; x < x1; x += 7) {
                double yy = topY + wave + r.nextDouble() * (botY - topY - wave);
                g.draw(new Line2D.Double(x, yy, x + 2, yy - 5));
            }
        }
        g.setClip(oc);
        // bright top lip
        g.setColor(Palette.alpha(Color.WHITE, 60));
        g.setStroke(new BasicStroke(1.5f));
        Path2D lip = new Path2D.Double();
        boolean first = true;
        for (double x = x0; x <= x1; x += 6) {
            double y = topY + Math.sin(x * 0.011 + phase) * wave + Math.sin(x * 0.027 + phase * 2) * wave * 0.4;
            if (first) { lip.moveTo(x, y); first = false; } else lip.lineTo(x, y);
        }
        g.draw(lip);
    }

    // ------------------------------------------------------------------ palm

    static void palm(Graphics2D g, double x, double baseY, double h, double t, int seed, boolean lean) {
        double dir = lean ? 1 : -1;
        double lx = dir * h * 0.14;
        // trunk as tapered ribbon along a quad curve
        double cx = x + lx * 0.2, cy = baseY - h * 0.55;
        double tx = x + lx, ty = baseY - h;
        Path2D left = new Path2D.Double(), right = new Path2D.Double();
        int n = 14;
        double[][] pts = new double[n + 1][2];
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n, v = 1 - u;
            pts[i][0] = v * v * x + 2 * v * u * cx + u * u * tx;
            pts[i][1] = v * v * baseY + 2 * v * u * cy + u * u * ty;
        }
        for (int i = 0; i <= n; i++) {
            double wd = h * (0.032 - 0.014 * i / n);
            if (i == 0) left.moveTo(pts[i][0] - wd, pts[i][1]); else left.lineTo(pts[i][0] - wd, pts[i][1]);
        }
        for (int i = n; i >= 0; i--) {
            double wd = h * (0.032 - 0.014 * i / n);
            left.lineTo(pts[i][0] + wd, pts[i][1]);
        }
        left.closePath();
        g.setPaint(new GradientPaint((float) (x - h * 0.03), 0, new Color(0x8C6A45), (float) (x + h * 0.04), 0, new Color(0x5E4429)));
        g.fill(left);
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 110));
        g.setStroke(new BasicStroke((float) Math.max(0.8, h * 0.006)));
        for (int i = 1; i < n; i++) {
            double wd = h * (0.032 - 0.014 * i / n);
            g.draw(new Line2D.Double(pts[i][0] - wd, pts[i][1], pts[i][0] + wd, pts[i][1] - h * 0.006));
        }
        // fronds
        Random r = new Random(seed * 77L + 5);
        int fronds = 9;
        for (int i = 0; i < fronds; i++) {
            double a = Math.PI * (1.05 + 0.9 * i / (fronds - 1.0)) + (r.nextDouble() - 0.5) * 0.15;   // upward fan
            a += Math.sin(t * 1.1 + seed + i * 0.8) * 0.05;
            double len = h * (0.34 + r.nextDouble() * 0.12);
            double ex = tx + Math.cos(a) * len, ey = ty + Math.sin(a) * len * 0.75 + len * 0.42 * Math.abs(Math.cos(a));
            double mx = tx + Math.cos(a) * len * 0.55, my = ty + Math.sin(a) * len * 0.62 - len * 0.12;
            Path2D leaf = new Path2D.Double();
            double wd = len * 0.15;
            leaf.moveTo(tx, ty);
            leaf.quadTo(mx - Math.sin(a) * wd, my + Math.cos(a) * wd, ex, ey);
            leaf.quadTo(mx + Math.sin(a) * wd, my - Math.cos(a) * wd, tx, ty);
            leaf.closePath();
            Color c = i % 2 == 0 ? new Color(0x2F8A45) : new Color(0x3FA050);
            g.setColor(c);
            g.fill(leaf);
            g.setColor(Palette.alpha(Palette.LEAF_DARK, 150));
            g.setStroke(new BasicStroke((float) Math.max(0.8, h * 0.006)));
            g.draw(new QuadCurve2D.Double(tx, ty, mx, my, ex, ey));
        }
        g.setColor(new Color(0x6B4A25));
        for (int i = 0; i < 3; i++)
            g.fill(new Ellipse2D.Double(tx - h * 0.03 + i * h * 0.03, ty + h * 0.01 + (i % 2) * h * 0.012, h * 0.04, h * 0.04));
    }

    // ------------------------------------------------------------------ buildings

    /** Bahay kubo: bamboo hut on stilts with a nipa-thatch roof. */
    static void bahayKubo(Graphics2D g, double x, double baseY, double w) {
        double stilt = w * 0.26, wallH = w * 0.30, floorY = baseY - stilt, wallTop = floorY - wallH;
        // shadow
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 40));
        g.fill(new Ellipse2D.Double(x - w * 0.05, baseY - w * 0.03, w * 1.1, w * 0.07));
        // stilts
        g.setColor(new Color(0x7A5530));
        for (int i = 0; i < 4; i++)
            g.fill(new Rectangle2D.Double(x + w * (0.05 + i * 0.28), floorY, w * 0.035, stilt));
        // ladder
        g.setColor(new Color(0x8A6538));
        g.setStroke(new BasicStroke((float) Math.max(1, w * 0.012)));
        g.draw(new Line2D.Double(x - w * 0.02, baseY, x + w * 0.10, floorY));
        g.draw(new Line2D.Double(x + w * 0.05, baseY, x + w * 0.16, floorY));
        for (int i = 1; i < 5; i++) {
            double u = i / 5.0;
            g.draw(new Line2D.Double(x - w * 0.02 + w * 0.12 * u, baseY - stilt * u, x + w * 0.05 + w * 0.11 * u, baseY - stilt * u));
        }
        // floor + bamboo walls
        g.setColor(new Color(0x9B7040));
        g.fill(new Rectangle2D.Double(x, floorY - w * 0.02, w * 0.96, w * 0.03));
        Rectangle2D wall = new Rectangle2D.Double(x + w * 0.05, wallTop, w * 0.86, wallH);
        g.setPaint(new GradientPaint(0, (float) wallTop, new Color(0xE9CC8A), 0, (float) floorY, new Color(0xCFA765)));
        g.fill(wall);
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 70));
        g.setStroke(new BasicStroke(1f));
        for (double bx = x + w * 0.05; bx < x + w * 0.91; bx += w * 0.05) g.draw(new Line2D.Double(bx, wallTop, bx, floorY));
        // window + door
        g.setColor(new Color(0x3A2A18));
        g.fill(new Rectangle2D.Double(x + w * 0.18, wallTop + wallH * 0.2, w * 0.17, wallH * 0.38));
        g.fill(new Rectangle2D.Double(x + w * 0.62, wallTop + wallH * 0.12, w * 0.15, wallH * 0.88));
        // thatch roof
        Path2D roof = new Path2D.Double();
        roof.moveTo(x - w * 0.12, wallTop + wallH * 0.12);
        roof.lineTo(x + w * 0.18, wallTop - w * 0.38);
        roof.lineTo(x + w * 0.80, wallTop - w * 0.38);
        roof.lineTo(x + w * 1.08, wallTop + wallH * 0.12);
        roof.closePath();
        g.setPaint(new GradientPaint(0, (float) (wallTop - w * 0.38), new Color(0xC79B55), 0, (float) wallTop, new Color(0x8B6430)));
        g.fill(roof);
        g.setColor(Palette.alpha(new Color(0x5A3F1E), 140));
        g.setStroke(new BasicStroke((float) Math.max(1, w * 0.008)));
        Shape oc = g.getClip();
        g.clip(roof);
        for (double rx = x - w * 0.1; rx < x + w * 1.1; rx += w * 0.045)
            g.draw(new Line2D.Double(rx + w * 0.05, wallTop - w * 0.38, rx - w * 0.04, wallTop + wallH * 0.15));
        g.setClip(oc);
        // fringe
        g.setColor(new Color(0x6F5027));
        Path2D fringe = new Path2D.Double();
        fringe.moveTo(x - w * 0.12, wallTop + wallH * 0.12);
        for (double fx = x - w * 0.12; fx < x + w * 1.08; fx += w * 0.03) {
            fringe.lineTo(fx + w * 0.015, wallTop + wallH * 0.12 + w * 0.025);
            fringe.lineTo(fx + w * 0.03, wallTop + wallH * 0.12);
        }
        fringe.closePath();
        g.fill(fringe);
    }

    private static final Color[] SNACKS = {new Color(0xF2B636), new Color(0xD8483A), new Color(0x3C9A56), new Color(0x2F7FC2), new Color(0xEF8A2F), new Color(0xF4E2B0)};

    /** Wooden sari-sari store: signboard, GI-sheet awning, shelves of goods, hanging sachets, a smiling tindera. */
    static void sariSari(Graphics2D g, double x, double baseY, double w, double h, boolean detail) {
        double top = baseY - h;
        double awnY = top + h * 0.26, awnH = h * 0.10, counterY = baseY - h * 0.30;
        // ground shadow
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 45));
        g.fill(new Ellipse2D.Double(x - w * 0.04, baseY - h * 0.03, w * 1.08, h * 0.07));
        // back wall (planks)
        Rectangle2D wall = new Rectangle2D.Double(x, top + h * 0.05, w, h * 0.95);
        g.setPaint(new GradientPaint(0, (float) top, new Color(0xB07A45), 0, (float) baseY, new Color(0x8A5A33)));
        g.fill(wall);
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 80));
        g.setStroke(new BasicStroke(1f));
        for (double px = x + w * 0.06; px < x + w; px += w * 0.06) g.draw(new Line2D.Double(px, top + h * 0.05, px, baseY));
        // dark shop opening
        Rectangle2D open = new Rectangle2D.Double(x + w * 0.06, awnY + awnH, w * 0.88, counterY - awnY - awnH);
        g.setPaint(new GradientPaint(0, (float) open.getY(), new Color(0x4A3322), 0, (float) (open.getY() + open.getHeight()), new Color(0x2C1E12)));
        g.fill(open);
        // shelves + goods
        double sy1 = open.getY() + open.getHeight() * 0.40, sy2 = open.getY() + open.getHeight() * 0.80;
        g.setColor(new Color(0xC79A62));
        g.fill(new Rectangle2D.Double(open.getX(), sy1, open.getWidth(), h * 0.012));
        g.fill(new Rectangle2D.Double(open.getX(), sy2, open.getWidth(), h * 0.012));
        Random r = new Random(11);
        double gx = open.getX() + w * 0.02;
        while (gx < open.getMaxX() - w * 0.06) {
            // snack bag row (upper shelf)
            Color c = SNACKS[r.nextInt(SNACKS.length)];
            double bw = w * 0.052, bh = (sy1 - open.getY()) * (0.52 + r.nextDouble() * 0.2);
            Path2D bag = new Path2D.Double();
            bag.moveTo(gx, sy1);
            bag.lineTo(gx + bw * 0.06, sy1 - bh);
            bag.lineTo(gx + bw * 0.94, sy1 - bh);
            bag.lineTo(gx + bw, sy1);
            bag.closePath();
            g.setColor(c);
            g.fill(bag);
            g.setColor(Palette.alpha(Color.WHITE, 150));
            g.fill(new Rectangle2D.Double(gx + bw * 0.2, sy1 - bh * 0.62, bw * 0.6, bh * 0.22));
            g.setColor(Palette.alpha(Palette.WOOD_DARK, 120));
            g.fill(new Rectangle2D.Double(gx, sy1 - bh, bw, bh * 0.07));
            gx += bw + w * 0.008;
        }
        gx = open.getX() + w * 0.03;
        while (gx < open.getMaxX() - w * 0.06) {
            if (r.nextBoolean()) {   // bottle
                Color c = new Color[]{new Color(0x8A3B1E), new Color(0x4FA66B), new Color(0xE9A23B), new Color(0x6FB4D8)}[r.nextInt(4)];
                double bw = w * 0.034, bh = (sy2 - sy1) * 0.72;
                g.setColor(c);
                g.fill(new RoundRectangle2D.Double(gx, sy2 - bh, bw, bh, bw * 0.5, bw * 0.5));
                g.fill(new Rectangle2D.Double(gx + bw * 0.3, sy2 - bh - bh * 0.22, bw * 0.4, bh * 0.25));
                g.setColor(Palette.alpha(Color.WHITE, 140));
                g.fill(new Rectangle2D.Double(gx + bw * 0.1, sy2 - bh * 0.7, bw * 0.8, bh * 0.25));
                gx += bw + w * 0.012;
            } else {                 // can / noodle cup
                Color c = SNACKS[r.nextInt(SNACKS.length)];
                double bw = w * 0.04, bh = (sy2 - sy1) * 0.5;
                g.setColor(c);
                g.fill(new RoundRectangle2D.Double(gx, sy2 - bh, bw, bh, 3, 3));
                g.setColor(Palette.alpha(Color.WHITE, 120));
                g.fill(new Rectangle2D.Double(gx, sy2 - bh * 0.7, bw, bh * 0.14));
                gx += bw + w * 0.012;
            }
        }
        // floor of the shop opening: sacks of rice
        double lowY = open.getMaxY();
        for (int i = 0; i < 3; i++) {
            double sx = open.getX() + w * (0.04 + i * 0.12);
            g.setColor(new Color(0xE6D5AE));
            g.fill(new RoundRectangle2D.Double(sx, sy2 + h * 0.02, w * 0.10, lowY - sy2 - h * 0.02, 8, 8));
            g.setColor(Palette.alpha(Palette.WOOD_DARK, 60));
            g.draw(new Line2D.Double(sx + w * 0.02, sy2 + h * 0.05, sx + w * 0.08, sy2 + h * 0.05));
        }
        // tindera (shopkeeper)
        if (detail) {
            double vx = open.getX() + open.getWidth() * 0.72, vy = open.getMaxY();
            double s = h * 0.34;
            g.setColor(new Color(0x2B1A12));
            g.fill(new Ellipse2D.Double(vx - s * 0.2, vy - s * 1.0, s * 0.4, s * 0.42));      // hair
            g.fill(new Ellipse2D.Double(vx - s * 0.1, vy - s * 1.12, s * 0.22, s * 0.22));    // bun
            g.setColor(new Color(0xE05D78));
            g.fill(new RoundRectangle2D.Double(vx - s * 0.3, vy - s * 0.62, s * 0.6, s * 0.62, 14, 14));  // blouse
            g.setColor(Palette.alpha(Color.WHITE, 200));
            for (int i = 0; i < 6; i++) g.fill(new Ellipse2D.Double(vx - s * 0.24 + (i % 3) * s * 0.17, vy - s * 0.5 + (i / 3) * s * 0.2, s * 0.07, s * 0.07));
            g.setColor(SKIN);
            g.fill(new Ellipse2D.Double(vx - s * 0.16, vy - s * 0.93, s * 0.32, s * 0.36));   // face
            g.setColor(new Color(0x2B1A12));
            g.fill(new Ellipse2D.Double(vx - s * 0.08, vy - s * 0.8, s * 0.04, s * 0.04));
            g.fill(new Ellipse2D.Double(vx + s * 0.04, vy - s * 0.8, s * 0.04, s * 0.04));
            g.setStroke(new BasicStroke((float) Math.max(1, s * 0.015), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(vx - s * 0.07, vy - s * 0.75, s * 0.14, s * 0.1, 200, 140, Arc2D.OPEN));
        }
        // counter
        Rectangle2D counter = new Rectangle2D.Double(x - w * 0.02, counterY, w * 1.04, baseY - counterY);
        g.setPaint(new GradientPaint(0, (float) counterY, new Color(0xA9763F), 0, (float) baseY, new Color(0x7A4E2D)));
        g.fill(counter);
        g.setColor(new Color(0xD5A86A));
        g.fill(new Rectangle2D.Double(x - w * 0.03, counterY - h * 0.02, w * 1.06, h * 0.03));
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 90));
        for (double px = x + w * 0.04; px < x + w; px += w * 0.09) g.draw(new Line2D.Double(px, counterY + h * 0.02, px, baseY));
        // jars and egg tray on counter
        Random r2 = new Random(5);
        for (int i = 0; i < 4; i++) {
            double jx = x + w * (0.08 + i * 0.075);
            g.setColor(Palette.alpha(new Color(0xE9F4F7), 220));
            g.fill(new RoundRectangle2D.Double(jx, counterY - h * 0.105, w * 0.055, h * 0.085, 8, 8));
            g.setColor(SNACKS[r2.nextInt(SNACKS.length)]);
            g.fill(new RoundRectangle2D.Double(jx + w * 0.006, counterY - h * 0.075, w * 0.043, h * 0.05, 6, 6));
            g.setColor(Palette.WOOD_DARK);
            g.fill(new Rectangle2D.Double(jx + w * 0.005, counterY - h * 0.115, w * 0.045, h * 0.014));
        }
        g.setColor(new Color(0xF8EFD6));
        for (int i = 0; i < 6; i++) g.fill(new Ellipse2D.Double(x + w * (0.60 + i * 0.045), counterY - h * 0.045, w * 0.036, h * 0.045));
        // awning (corrugated GI sheet)
        Path2D awn = new Path2D.Double();
        awn.moveTo(x - w * 0.07, awnY + awnH);
        awn.lineTo(x + w * 0.02, awnY - h * 0.01);
        awn.lineTo(x + w * 0.98, awnY - h * 0.01);
        awn.lineTo(x + w * 1.07, awnY + awnH);
        awn.closePath();
        g.setPaint(new GradientPaint(0, (float) awnY, new Color(0xC4624A), 0, (float) (awnY + awnH), new Color(0x93432F)));
        g.fill(awn);
        g.setColor(Palette.alpha(Color.WHITE, 60));
        g.setStroke(new BasicStroke(1.2f));
        for (double px = x - w * 0.05; px < x + w * 1.05; px += w * 0.022) g.draw(new Line2D.Double(px + w * 0.01, awnY, px, awnY + awnH));
        // hanging sachets + bananas under the awning
        for (int s = 0; s < 7; s++) {
            double sx = x + w * (0.10 + s * 0.12);
            double len = h * (0.10 + (s % 3) * 0.035);
            g.setColor(Palette.alpha(Palette.WOOD_DARK, 160));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(sx, awnY + awnH, sx, awnY + awnH + len));
            int k = (int) (len / (h * 0.022));
            for (int j = 0; j < k; j++) {
                g.setColor(SNACKS[(s + j) % SNACKS.length]);
                g.fill(new Rectangle2D.Double(sx - w * 0.008, awnY + awnH + j * h * 0.022, w * 0.016, h * 0.02));
            }
        }
        // signboard on top
        double sbW = w * 0.90, sbH = h * 0.22, sbX = x + w * 0.05, sbY = top - h * 0.02;
        sign(g, sbX, sbY, sbW, sbH, "TINDAHAN NI ALING MARIA", "SARI-SARI STORE", new Color(0xFBF1D3), new Color(0x1F5A36), new Color(0xB5432E));
        // little hanging LOAD sign
        double lw = w * 0.19, lh = h * 0.12, lx = x + w * 0.80, ly = awnY + awnH + h * 0.03;
        g.setColor(Palette.WOOD_DARK);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Line2D.Double(lx + lw * 0.2, awnY + awnH, lx + lw * 0.2, ly));
        g.draw(new Line2D.Double(lx + lw * 0.8, awnY + awnH, lx + lw * 0.8, ly));
        Rectangle2D ls = new Rectangle2D.Double(lx, ly, lw, lh);
        g.setColor(new Color(0xFFD347));
        g.fill(ls);
        g.setColor(new Color(0x2467B0));
        g.setStroke(new BasicStroke(2f));
        g.draw(ls);
        Font lf = Fonts.sign((float) (lh * 0.55));
        Palette.outlineTextCentered(g, "LOAD", lf, (float) (lx + lw / 2), (float) (ly + lh * 0.68), new Color(0x2467B0), new Color(0xFFD347), 0.6f);
        // side posts
        g.setColor(new Color(0x6A4526));
        g.fill(new Rectangle2D.Double(x - w * 0.025, top + h * 0.05, w * 0.03, h * 0.95));
        g.fill(new Rectangle2D.Double(x + w * 0.995, top + h * 0.05, w * 0.03, h * 0.95));
    }

    /** Hand-painted signboard with up to two lines of lettering. */
    static void sign(Graphics2D g, double x, double y, double w, double h, String line1, String line2, Color board, Color ink, Color border) {
        RoundRectangle2D rr = new RoundRectangle2D.Double(x, y, w, h, h * 0.18, h * 0.18);
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 60));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 3, w, h, h * 0.18, h * 0.18));
        g.setPaint(new GradientPaint(0, (float) y, board, 0, (float) (y + h), Palette.mix(board, new Color(0xD8C08A), 0.5f)));
        g.fill(rr);
        g.setColor(border);
        g.setStroke(new BasicStroke((float) Math.max(2, h * 0.07)));
        g.draw(new RoundRectangle2D.Double(x + h * 0.06, y + h * 0.06, w - h * 0.12, h - h * 0.12, h * 0.14, h * 0.14));
        double sizeA = h * (line2 == null ? 0.50 : 0.36);
        Font f1 = Fonts.display((float) sizeA);
        float tw = Fonts.width(line1, f1);
        if (tw > w * 0.84) f1 = f1.deriveFont((float) (f1.getSize2D() * w * 0.84 / tw));
        g.setColor(ink);
        Fonts.draw(g, line1, f1, (float) (x + w / 2 - Fonts.width(line1, f1) / 2), (float) (y + h * (line2 == null ? 0.66 : 0.50)));
        if (line2 != null) {
            Font f2 = Fonts.sign((float) (h * 0.16));
            float t2 = Fonts.width(line2, f2);
            if (t2 > w * 0.7) f2 = f2.deriveFont((float) (f2.getSize2D() * w * 0.7 / t2));
            g.setColor(border);
            Fonts.draw(g, line2, f2, (float) (x + w / 2 - Fonts.width(line2, f2) / 2), (float) (y + h * 0.80));
        }
    }

    // ------------------------------------------------------------------ people

    /** Salakot: the wide conical farmer's hat. (cx, cy) is the crown base centre; r is the brim radius. */
    static void salakot(Graphics2D g, double cx, double cy, double r) {
        Path2D hat = new Path2D.Double();
        hat.moveTo(cx - r, cy);
        hat.quadTo(cx - r * 0.35, cy - r * 0.18, cx, cy - r * 0.62);
        hat.quadTo(cx + r * 0.35, cy - r * 0.18, cx + r, cy);
        hat.quadTo(cx, cy + r * 0.18, cx - r, cy);
        hat.closePath();
        g.setPaint(new GradientPaint((float) (cx - r), 0, HAT, (float) (cx + r), 0, new Color(0xC79C4D)));
        g.fill(hat);
        g.setColor(Palette.alpha(HAT_DARK, 170));
        g.setStroke(new BasicStroke((float) Math.max(0.7, r * 0.03)));
        for (int i = 1; i < 6; i++) {
            double u = i / 6.0;
            g.draw(new Line2D.Double(cx, cy - r * 0.62, cx - r + 2 * r * u, cy + r * 0.04 * Math.sin(Math.PI * u)));
        }
        g.draw(new Line2D.Double(cx - r * 0.55, cy - r * 0.12, cx + r * 0.55, cy - r * 0.12));
        g.setColor(HAT_DARK);
        g.setStroke(new BasicStroke((float) Math.max(1, r * 0.05)));
        g.draw(new QuadCurve2D.Double(cx - r, cy, cx, cy + r * 0.18, cx + r, cy));
    }

    /** Farmer bending to plant rice seedlings. {@code s} is body height; the farmer faces {@code dir} (+1 right, -1 left). */
    static void farmerPlanting(Graphics2D g, double x, double y, double s, double t, double phase, int dir, Color shirt) {
        double bend = 1.0 + 0.14 * Math.sin(t * 1.6 + phase);
        double hipX = x, hipY = y - s * 0.42;
        double shX = hipX + dir * Math.sin(bend) * s * 0.34, shY = hipY - Math.cos(bend) * s * 0.34;
        // water ring
        g.setColor(Palette.alpha(Color.WHITE, 90));
        g.setStroke(new BasicStroke(1.2f));
        double rip = (t * 0.6 + phase) % 1;
        g.draw(new Ellipse2D.Double(x - s * (0.22 + rip * 0.2), y - s * 0.03, s * (0.44 + rip * 0.4), s * 0.07));
        // legs
        g.setColor(new Color(0x4B6C8F));
        g.setStroke(new BasicStroke((float) (s * 0.10), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(hipX - s * 0.07, y - s * 0.02, hipX - s * 0.02, hipY));
        g.draw(new Line2D.Double(hipX + s * 0.09, y - s * 0.02, hipX + s * 0.02, hipY));
        g.setColor(SKIN_DARK);
        g.fill(new Ellipse2D.Double(hipX - s * 0.12, y - s * 0.08, s * 0.1, s * 0.07));
        g.fill(new Ellipse2D.Double(hipX + s * 0.05, y - s * 0.08, s * 0.1, s * 0.07));
        // torso
        g.setColor(shirt);
        g.setStroke(new BasicStroke((float) (s * 0.21), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(hipX, hipY, shX, shY));
        // arms reaching down to the paddy
        double handX = shX + dir * s * 0.10, handY = y - s * 0.07 - Math.max(0, Math.sin(t * 1.6 + phase)) * s * 0.03;
        g.setColor(SKIN);
        g.setStroke(new BasicStroke((float) (s * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(shX, shY + s * 0.03, handX, handY));
        // seedlings
        g.setColor(new Color(0x4FAE3C));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -1; i <= 1; i++) g.draw(new Line2D.Double(handX + i * s * 0.02, handY, handX + i * s * 0.05, handY - s * 0.10));
        // head + salakot
        double hx = shX + dir * s * 0.08, hy = shY - s * 0.04;
        g.setColor(SKIN);
        g.fill(new Ellipse2D.Double(hx - s * 0.09, hy - s * 0.09, s * 0.18, s * 0.18));
        salakot(g, hx, hy - s * 0.04, s * 0.27);
    }

    /** Pose ids for {@link #farmer}. */
    static final int PLANT = 0, WAVE = 1, HOE = 2, SICKLE = 3, CARRY = 4, REST = 5, POSES = 6;

    private static void limb(Graphics2D g, Color c, double w, double x1, double y1, double x2, double y2) {
        g.setColor(c);
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    /**
     * A farmer in one of several poses (planting, waving, hoeing, cutting rice with a sickle, carrying baskets on a
     * pole, resting on a hoe). {@code s} is body height, {@code dir} is +1 to face right, -1 to face left.
     */
    static void farmer(Graphics2D g, double x, double y, double s, double t, double phase, int dir, Color shirt, int pose) {
        if (pose == PLANT) { farmerPlanting(g, x, y, s, t, phase, dir, shirt); return; }
        double a = t * 1.8 + phase, sw = Math.sin(a);
        Color pants = new Color(0x4B6C8F);
        double hipY = y - s * 0.44, shY = y - s * 0.76, hipX = x, shX = x;
        double lean = 0, bob = 0;
        if (pose == SICKLE) { lean = 0.30 + 0.05 * sw; shY = y - s * 0.66; }
        if (pose == CARRY) bob = Math.abs(Math.sin(a * 1.2)) * s * 0.03;
        if (pose == HOE) lean = 0.10;
        shX = x + dir * lean * s;
        shY -= bob; hipY -= bob * 0.5;
        // legs
        double sp = pose == CARRY ? Math.sin(a * 1.2) * s * 0.12 : (pose == SICKLE ? s * 0.12 : s * 0.07);
        limb(g, pants, s * 0.11, hipX, hipY, x - sp, y - s * 0.03);
        limb(g, pants, s * 0.11, hipX, hipY, x + sp + (pose == SICKLE ? dir * s * 0.06 : 0), y - s * 0.03);
        g.setColor(SKIN_DARK);
        g.fill(new Ellipse2D.Double(x - sp - s * 0.06, y - s * 0.06, s * 0.13, s * 0.06));
        g.fill(new Ellipse2D.Double(x + sp - s * 0.06 + (pose == SICKLE ? dir * s * 0.06 : 0), y - s * 0.06, s * 0.13, s * 0.06));
        // torso
        limb(g, shirt, s * 0.23, hipX, hipY, shX, shY);
        double ax = shX, ay = shY + s * 0.03;
        switch (pose) {
            case WAVE -> {
                limb(g, SKIN, s * 0.065, ax, ay, ax - dir * s * 0.13, hipY - s * 0.03);                       // resting arm
                double hx = ax + dir * s * 0.16 + sw * s * 0.07, hy = shY - s * 0.30;                          // waving arm
                limb(g, SKIN, s * 0.065, ax, ay, hx, hy);
                g.setColor(SKIN);
                g.fill(new Ellipse2D.Double(hx - s * 0.04, hy - s * 0.04, s * 0.08, s * 0.08));
            }
            case HOE -> {
                double sw2 = Math.sin(a * 0.9), up = (sw2 + 1) / 2;                                            // 0 = down, 1 = raised
                double hx = ax + dir * s * (0.12 + 0.12 * (1 - up)), hy = shY - s * (0.30 * up) + s * 0.10 * (1 - up);
                double tipX = hx + dir * s * (0.14 + 0.20 * (1 - up)), tipY = hy + s * (0.30 - 0.18 * up) + s * 0.30 * (1 - up);
                limb(g, new Color(0x7A5230), s * 0.04, hx - dir * s * 0.10, hy - s * (0.22 * up), tipX, Math.min(tipY, y - s * 0.02));
                limb(g, new Color(0x5C5C5C), s * 0.07, tipX, Math.min(tipY, y - s * 0.02), tipX + dir * s * 0.09, Math.min(tipY, y - s * 0.02));
                limb(g, SKIN, s * 0.065, ax, ay, hx, hy);
            }
            case SICKLE -> {
                double hx = ax + dir * s * 0.20, hy = y - s * 0.26 + sw * s * 0.08;
                limb(g, SKIN, s * 0.065, ax, ay, hx, hy);
                g.setColor(new Color(0x9AA3A8));
                g.setStroke(new BasicStroke((float) (s * 0.03), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(hx - s * 0.08 + (dir > 0 ? s * 0.03 : -s * 0.03), hy - s * 0.10, s * 0.16, s * 0.16, dir > 0 ? 200 : -20, 150 * dir, Arc2D.OPEN));
                // cut rice stalks in a bundle
                g.setColor(new Color(0xD9B84A));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = -2; i <= 2; i++) g.draw(new Line2D.Double(hx + dir * s * 0.15 + i * s * 0.015, y - s * 0.03, hx + dir * s * (0.17 + 0.02 * i), y - s * 0.30));
                limb(g, SKIN, s * 0.06, ax, ay, hx - dir * s * 0.02, y - s * 0.22);
            }
            case CARRY -> {
                // bamboo pole across the shoulders, a basket at each end
                double px = shX, py = shY - s * 0.04, half = s * 0.46, ty = py + sw * s * 0.015;
                limb(g, new Color(0x9B7B3F), s * 0.035, px - half, ty, px + half, ty);
                for (int e : new int[]{-1, 1}) {
                    double ex = px + e * half, ey = ty + s * 0.20 + Math.sin(a * 1.2 + e) * s * 0.012;
                    g.setColor(new Color(0x6E5A33));
                    g.setStroke(new BasicStroke(1f));
                    g.draw(new Line2D.Double(ex, ty, ex - s * 0.07, ey));
                    g.draw(new Line2D.Double(ex, ty, ex + s * 0.07, ey));
                    g.setColor(new Color(0xB98B4A));
                    g.fill(new Arc2D.Double(ex - s * 0.09, ey - s * 0.08, s * 0.18, s * 0.16, 180, 180, Arc2D.CHORD));
                    g.setColor(e < 0 ? new Color(0xE08A2E) : new Color(0x6FB04A));                               // mangoes / greens
                    g.fill(new Ellipse2D.Double(ex - s * 0.06, ey - s * 0.11, s * 0.06, s * 0.06));
                    g.fill(new Ellipse2D.Double(ex, ey - s * 0.115, s * 0.06, s * 0.06));
                }
                limb(g, SKIN, s * 0.06, ax, ay, px + dir * s * 0.20, ty);
            }
            default -> {   // REST: leaning on a hoe, wiping brow
                double hx = ax + dir * s * 0.18, hy = hipY - s * 0.02;
                limb(g, new Color(0x7A5230), s * 0.04, hx + dir * s * 0.02, y - s * 0.02, hx - dir * s * 0.02, shY - s * 0.20);
                limb(g, SKIN, s * 0.065, ax, ay, hx, hy);
                double bx2 = ax - dir * s * 0.05, by2 = shY - s * 0.20 + Math.max(0, sw) * s * 0.02;
                limb(g, SKIN, s * 0.06, ax - dir * s * 0.03, ay, bx2 - dir * s * 0.05, by2);
            }
        }
        // head + salakot
        double hdx = shX + dir * s * 0.03, hdy = shY - s * 0.14;
        g.setColor(SKIN);
        g.fill(new Ellipse2D.Double(hdx - s * 0.09, hdy - s * 0.09, s * 0.18, s * 0.18));
        salakot(g, hdx, hdy - s * 0.03, s * 0.28);
    }

    /** Standing / walking farmer (used beside the cart). */
    static void farmerStanding(Graphics2D g, double x, double y, double s, double stride, int dir, Color shirt) {
        g.setColor(new Color(0x4B6C8F));
        g.setStroke(new BasicStroke((float) (s * 0.11), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double hipY = y - s * 0.46;
        g.draw(new Line2D.Double(x, hipY, x + stride * s * 0.14, y - s * 0.03));
        g.draw(new Line2D.Double(x, hipY, x - stride * s * 0.14, y - s * 0.03));
        g.setColor(SKIN_DARK);
        g.fill(new Ellipse2D.Double(x + stride * s * 0.14 - s * 0.05, y - s * 0.06, s * 0.13, s * 0.06));
        g.fill(new Ellipse2D.Double(x - stride * s * 0.14 - s * 0.05, y - s * 0.06, s * 0.13, s * 0.06));
        g.setColor(shirt);
        g.setStroke(new BasicStroke((float) (s * 0.24), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x, hipY, x + dir * s * 0.02, y - s * 0.74));
        g.setColor(SKIN);
        g.setStroke(new BasicStroke((float) (s * 0.065), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x + dir * s * 0.02, y - s * 0.70, x + dir * s * 0.20, y - s * (0.48 + 0.06 * stride)));
        g.fill(new Ellipse2D.Double(x + dir * s * 0.02 - s * 0.1, y - s * 0.92, s * 0.2, s * 0.2));
        salakot(g, x + dir * s * 0.02, y - s * 0.86, s * 0.30);
    }

    // ------------------------------------------------------------------ carabao + cart

    /** Carabao (water buffalo) walking, unit space: length 1, facing +x. */
    private static void carabaoUnit(Graphics2D g, double t) {
        double ph = t * 3.2;
        // legs (far pair first, darker)
        leg(g, ph + Math.PI, 0.20, new Color(0x2E2C2B));
        leg(g, ph, 0.75, new Color(0x2E2C2B));
        // tail
        g.setColor(new Color(0x3A3836));
        g.setStroke(new BasicStroke(0.012f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double sw = Math.sin(t * 3 + 1) * 0.04;
        g.draw(new CubicCurve2D.Double(0.04, 0.55, -0.03, 0.46 + sw, -0.03, 0.34, -0.015 + sw, 0.22));
        // body
        Path2D body = new Path2D.Double();
        body.moveTo(0.02, 0.50);
        body.quadTo(0.10, 0.66, 0.30, 0.66);
        body.quadTo(0.52, 0.70, 0.64, 0.66);     // hump
        body.quadTo(0.76, 0.62, 0.84, 0.52);     // neck down
        body.quadTo(0.92, 0.48, 1.00, 0.36);     // nose
        body.quadTo(1.00, 0.30, 0.94, 0.31);
        body.quadTo(0.84, 0.32, 0.78, 0.36);     // throat
        body.quadTo(0.70, 0.30, 0.62, 0.27);     // chest/belly
        body.quadTo(0.40, 0.24, 0.20, 0.27);
        body.quadTo(0.04, 0.30, 0.02, 0.50);
        body.closePath();
        g.setPaint(new GradientPaint(0, 0.70f, new Color(0x6A6866), 0, 0.24f, new Color(0x3B3938)));
        g.fill(body);
        g.setColor(Palette.alpha(Color.WHITE, 40));
        g.fill(new Ellipse2D.Double(0.18, 0.54, 0.40, 0.08));
        // pale chevron on the throat
        g.setColor(new Color(0xC9C3B8));
        g.setStroke(new BasicStroke(0.012f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0.80, 0.38, 0.84, 0.43));
        g.draw(new Line2D.Double(0.84, 0.43, 0.88, 0.36));
        // horns (swept back)
        Path2D horn = new Path2D.Double();
        horn.moveTo(0.86, 0.60);
        horn.quadTo(0.80, 0.70, 0.70, 0.66);
        horn.quadTo(0.76, 0.64, 0.83, 0.55);
        horn.closePath();
        g.setColor(new Color(0xD9CDB0));
        g.fill(horn);
        g.setColor(new Color(0x2E2C2B));
        g.fill(new Ellipse2D.Double(0.80, 0.54, 0.05, 0.035));          // ear
        g.setColor(new Color(0x15110F));
        g.fill(new Ellipse2D.Double(0.915, 0.48, 0.016, 0.016));        // eye
        g.fill(new Ellipse2D.Double(0.985, 0.355, 0.012, 0.012));       // nostril
        // front legs
        leg(g, ph, 0.14, new Color(0x4A4745));
        leg(g, ph + Math.PI, 0.69, new Color(0x4A4745));
    }

    /** One leg with a walking swing; hx is the hip x position. */
    private static void leg(Graphics2D g, double phase, double hx, Color c) {
        double swing = Math.sin(phase) * 0.05, lift = Math.max(0, Math.cos(phase)) * 0.03;
        double hipY = 0.36, kneeX = hx + swing * 0.5, kneeY = 0.19 + lift * 0.5, footX = hx + swing, footY = 0.012 + lift;
        g.setColor(c);
        g.setStroke(new BasicStroke(0.06f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(hx, hipY, kneeX, kneeY));
        g.setStroke(new BasicStroke(0.045f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(kneeX, kneeY, footX, footY));
        g.setColor(new Color(0x1E1B19));
        g.fill(new Ellipse2D.Double(footX - 0.03, footY - 0.012, 0.06, 0.03));
    }

    /**
     * Carabao pulling a wooden cart of produce with a farmer aboard. The group is about 1.75 x {@code len} long;
     * (x, groundY) is the carabao's rear when facing right (its front when facing left).
     */
    static void carabaoCart(Graphics2D g, double x, double groundY, double len, boolean right, double t) {
        AffineTransform keep = g.getTransform();
        g.translate(x, groundY);
        g.scale(right ? 1 : -1, 1);
        // soft ground shadow
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 50));
        g.fill(new Ellipse2D.Double(-len * 0.7, -len * 0.02, len * 1.75, len * 0.06));
        // cart sits behind (to the left of) the carabao
        double cartX = -len * 0.72, bedY = -len * 0.26;
        // shafts
        g.setColor(new Color(0x7A5530));
        g.setStroke(new BasicStroke((float) Math.max(2, len * 0.025), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cartX + len * 0.45, bedY + len * 0.04, len * 0.70, -len * 0.50));
        // bed
        Path2D bed = new Path2D.Double();
        bed.moveTo(cartX, bedY);
        bed.lineTo(cartX + len * 0.55, bedY);
        bed.lineTo(cartX + len * 0.50, bedY + len * 0.10);
        bed.lineTo(cartX + len * 0.05, bedY + len * 0.10);
        bed.closePath();
        g.setPaint(new GradientPaint(0, (float) bedY, new Color(0xB27B43), 0, (float) (bedY + len * 0.10), new Color(0x7F5530)));
        g.fill(bed);
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 130));
        g.setStroke(new BasicStroke(1f));
        for (int i = 1; i < 5; i++) g.draw(new Line2D.Double(cartX + len * 0.11 * i, bedY, cartX + len * (0.11 * i - 0.005), bedY + len * 0.10));
        // produce baskets
        Color[] veg = {new Color(0xE8872E), new Color(0x4FA64A), new Color(0xE9C23A), new Color(0xD8483A), new Color(0x7DBE4A)};
        for (int i = 0; i < 4; i++) {
            double bx = cartX + len * (0.04 + i * 0.125);
            g.setColor(new Color(0xC99A55));
            g.fill(new Arc2D.Double(bx, bedY - len * 0.07, len * 0.11, len * 0.14, 180, 180, Arc2D.CHORD));
            for (int k = 0; k < 4; k++) {
                g.setColor(veg[(i + k) % veg.length]);
                g.fill(new Ellipse2D.Double(bx + len * (0.01 + k * 0.022), bedY - len * (0.095 + 0.015 * (k % 2)), len * 0.035, len * 0.035));
            }
        }
        // wheel (wooden, spoked)
        double wr = len * 0.14, wx = cartX + len * 0.26, wy = -wr;
        g.setColor(new Color(0x6B4526));
        g.setStroke(new BasicStroke((float) Math.max(2, len * 0.03)));
        g.draw(new Ellipse2D.Double(wx - wr, wy - wr, wr * 2, wr * 2));
        g.setStroke(new BasicStroke(Math.max(1.2f, (float) (len * 0.012))));
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + t * 1.5;
            g.draw(new Line2D.Double(wx, wy, wx + Math.cos(a) * wr, wy + Math.sin(a) * wr));
        }
        g.fill(new Ellipse2D.Double(wx - len * 0.02, wy - len * 0.02, len * 0.04, len * 0.04));
        // farmer riding on the cart
        double fs = len * 0.52;
        double fx = cartX + len * 0.38, fy = bedY;
        g.setColor(new Color(0x4B6C8F));
        g.setStroke(new BasicStroke((float) (fs * 0.11), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(fx, fy - fs * 0.22, fx + fs * 0.20, fy - fs * 0.08));
        g.draw(new Line2D.Double(fx + fs * 0.20, fy - fs * 0.08, fx + fs * 0.22, fy + fs * 0.02));
        g.setColor(new Color(0x4C9A6A));
        g.setStroke(new BasicStroke((float) (fs * 0.25), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(fx, fy - fs * 0.24, fx + fs * 0.03, fy - fs * 0.62));
        g.setColor(SKIN);
        g.fill(new Ellipse2D.Double(fx + fs * 0.03 - fs * 0.1, fy - fs * 0.84, fs * 0.2, fs * 0.2));
        salakot(g, fx + fs * 0.03, fy - fs * 0.78, fs * 0.30);
        // carabao
        g.scale(len, -len);
        carabaoUnit(g, t);
        g.setTransform(keep);
    }

    // ------------------------------------------------------------------ jeepney

    /** Colourful jeepney, side view. {@code len} = pixel length; spans from x toward the facing direction. */
    static void jeepney(Graphics2D g, double x, double groundY, double len, boolean right, double t) {
        AffineTransform keep = g.getTransform();
        g.translate(x, groundY);
        g.scale(right ? len : -len, -len);
        float px = (float) (1.0 / len);
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 60));
        g.fill(new Ellipse2D.Double(-0.02, -0.012, 1.04, 0.03));
        // roof sign (route board) base + rack
        g.setColor(new Color(0xF4F0E4));
        g.fill(new Rectangle2D.Double(0.58, 0.405, 0.24, 0.045));
        // main body
        Path2D body = new Path2D.Double();
        body.moveTo(0.00, 0.07);
        body.lineTo(0.00, 0.34);
        body.lineTo(0.02, 0.37);
        body.lineTo(0.60, 0.37);
        body.quadTo(0.66, 0.40, 0.70, 0.39);
        body.lineTo(0.78, 0.30);
        body.lineTo(0.92, 0.27);       // hood top
        body.lineTo(0.99, 0.25);
        body.lineTo(0.99, 0.09);
        body.lineTo(0.95, 0.07);
        body.closePath();
        g.setPaint(new GradientPaint(0, 0.37f, new Color(0x3A8AD4), 0, 0.07f, new Color(0x1F5FA3)));
        g.fill(body);
        // roof overhang
        g.setColor(new Color(0xF6F2E6));
        g.fill(new RoundRectangle2D.Double(-0.01, 0.365, 0.72, 0.03, 0.02, 0.02));
        // colour stripes along the flank
        g.setColor(new Color(0xFFD347));
        g.fill(new Rectangle2D.Double(0.0, 0.215, 0.985, 0.018));
        g.setColor(new Color(0xE2493C));
        g.fill(new Rectangle2D.Double(0.0, 0.19, 0.985, 0.014));
        g.setColor(new Color(0xF6F2E6));
        g.fill(new Rectangle2D.Double(0.0, 0.17, 0.985, 0.012));
        // open passenger windows with passengers
        double[] hx = {0.08, 0.20, 0.32, 0.44};
        Color[] shirts = {new Color(0xE05D78), new Color(0xF2B636), new Color(0x4FA66B), new Color(0xA67AD0)};
        g.setColor(new Color(0x1A2438));
        g.fill(new Rectangle2D.Double(0.04, 0.255, 0.50, 0.095));
        for (int i = 0; i < 4; i++) {
            g.setColor(shirts[i]);
            g.fill(new RoundRectangle2D.Double(hx[i] - 0.025, 0.255, 0.05, 0.05, 0.02, 0.02));
            g.setColor(SKIN);
            g.fill(new Ellipse2D.Double(hx[i] - 0.018, 0.29 + 0.0, 0.036, 0.038));
            g.setColor(new Color(0x2B1A12));
            g.fill(new Arc2D.Double(hx[i] - 0.019, 0.305, 0.038, 0.03, 0, 180, Arc2D.CHORD));
        }
        // cab window + driver
        g.setColor(new Color(0xAFD8EE));
        Path2D cab = new Path2D.Double();
        cab.moveTo(0.58, 0.255);
        cab.lineTo(0.58, 0.35);
        cab.lineTo(0.68, 0.35);
        cab.lineTo(0.755, 0.28);
        cab.lineTo(0.755, 0.255);
        cab.closePath();
        g.fill(cab);
        g.setColor(SKIN);
        g.fill(new Ellipse2D.Double(0.62, 0.285, 0.04, 0.042));
        g.setColor(new Color(0x2B1A12));
        g.fill(new Arc2D.Double(0.62, 0.305, 0.04, 0.03, 0, 180, Arc2D.CHORD));
        // door seam + hood louvres
        g.setColor(Palette.alpha(Color.BLACK, 90));
        g.setStroke(new BasicStroke(0.004f));
        g.draw(new Line2D.Double(0.56, 0.07, 0.56, 0.35));
        for (int i = 0; i < 4; i++) g.draw(new Line2D.Double(0.80 + i * 0.035, 0.12, 0.81 + i * 0.035, 0.24));
        // chrome grille, bumper, headlight
        g.setPaint(new GradientPaint(0.95f, 0.25f, new Color(0xFFFFFF), 0.95f, 0.08f, new Color(0x9AA3AE)));
        g.fill(new Rectangle2D.Double(0.955, 0.10, 0.035, 0.14));
        g.fill(new RoundRectangle2D.Double(0.945, 0.065, 0.065, 0.028, 0.015, 0.015));
        g.setColor(new Color(0xFFF6B8));
        g.fill(new Ellipse2D.Double(0.955, 0.205, 0.035, 0.035));
        g.setColor(new Color(0xD8483A));
        g.fill(new Rectangle2D.Double(-0.004, 0.18, 0.012, 0.05));
        // hood ornament horses (little chrome shapes)
        g.setColor(new Color(0xE8ECF2));
        g.fill(new Rectangle2D.Double(0.90, 0.27, 0.012, 0.03));
        g.fill(new Ellipse2D.Double(0.895, 0.295, 0.025, 0.018));
        // wheels
        wheel(g, 0.19, t, len);
        wheel(g, 0.80, t, len);
        g.setTransform(keep);
        // lettering stays upright (drawn in pixel space)
        double dir = right ? 1 : -1;
        Font rf = Fonts.sign((float) (len * 0.030));
        Font sf = Fonts.display((float) (len * 0.062));
        g.setColor(new Color(0x1F5A36));
        Fonts.draw(g, "PALENGKE - BAYAN", rf, (float) (x + dir * len * 0.70 - Fonts.width("PALENGKE - BAYAN", rf) / 2), (float) (groundY - len * 0.418));
        g.setColor(new Color(0xFFF3C4));
        Fonts.draw(g, "Salamat po", sf, (float) (x + dir * len * 0.30 - (right ? 0 : Fonts.width("Salamat po", sf))), (float) (groundY - len * 0.105));
    }

    private static void wheel(Graphics2D g, double cx, double t, double len) {
        double cy = 0.07, r = 0.07;
        g.setColor(new Color(0x1F1D1C));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setPaint(new GradientPaint((float) (cx - 0.04), (float) (cy + 0.04), new Color(0xF2F4F7), (float) (cx + 0.04), (float) (cy - 0.04), new Color(0x7C8591)));
        g.fill(new Ellipse2D.Double(cx - 0.042, cy - 0.042, 0.084, 0.084));
        g.setColor(new Color(0x39404A));
        g.setStroke(new BasicStroke(0.007f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            double a = t * 12 + i * Math.PI * 2 / 5;
            g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * 0.04, cy + Math.sin(a) * 0.04));
        }
    }

    // ------------------------------------------------------------------ sea, boats, fiesta

    /** Bangka: outrigger boat with a bright sail, bobbing on the waves. */
    static void bangka(Graphics2D g, double cx, double cy, double len, double t) {
        AffineTransform keep = g.getTransform();
        g.translate(cx, cy + Math.sin(t * 1.3) * len * 0.02);
        g.rotate(Math.sin(t * 1.1) * 0.03);
        // sail
        Path2D sail = new Path2D.Double();
        sail.moveTo(0, -len * 0.08);
        sail.lineTo(0, -len * 0.62);
        sail.lineTo(len * 0.30, -len * 0.10);
        sail.closePath();
        g.setColor(new Color(0xF6EFDC));
        g.fill(sail);
        g.setColor(new Color(0xE2493C));
        g.fill(new Polygon(new int[]{0, 0, (int) (len * 0.12)}, new int[]{(int) (-len * 0.40), (int) (-len * 0.62), (int) (-len * 0.40)}, 3));
        g.setColor(Palette.WOOD_DARK);
        g.setStroke(new BasicStroke((float) Math.max(1, len * 0.015)));
        g.draw(new Line2D.Double(0, -len * 0.64, 0, -len * 0.05));
        // hull
        Path2D hull = new Path2D.Double();
        hull.moveTo(-len * 0.50, -len * 0.06);
        hull.quadTo(-len * 0.30, len * 0.07, 0, len * 0.07);
        hull.quadTo(len * 0.30, len * 0.07, len * 0.50, -len * 0.09);
        hull.quadTo(len * 0.30, -len * 0.04, 0, -len * 0.04);
        hull.quadTo(-len * 0.30, -len * 0.04, -len * 0.50, -len * 0.06);
        g.setColor(new Color(0x2F7FC2));
        g.fill(hull);
        g.setColor(new Color(0xF4E2B0));
        g.setStroke(new BasicStroke((float) Math.max(1, len * 0.012)));
        g.draw(new QuadCurve2D.Double(-len * 0.44, -len * 0.04, 0, len * 0.01, len * 0.44, -len * 0.07));
        // outrigger poles and float
        g.setColor(new Color(0xC79A55));
        g.setStroke(new BasicStroke((float) Math.max(1, len * 0.018), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(-len * 0.20, -len * 0.04, -len * 0.26, len * 0.10));
        g.draw(new Line2D.Double(len * 0.20, -len * 0.04, len * 0.26, len * 0.10));
        g.draw(new Line2D.Double(-len * 0.36, len * 0.10, len * 0.36, len * 0.10));
        g.setTransform(keep);
    }

    /** String of triangular fiesta flags (banderitas). */
    static void bunting(Graphics2D g, double x1, double y1, double x2, double y2, double sag, double t, int n, double flag) {
        Color[] cols = {new Color(0xD8483A), new Color(0xFFCB3D), new Color(0x2F7FC2), new Color(0x4FA66B), new Color(0xFFFFFF), new Color(0xEF8A2F)};
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 170));
        g.setStroke(new BasicStroke(1.3f));
        g.draw(new QuadCurve2D.Double(x1, y1, (x1 + x2) / 2, Math.max(y1, y2) + sag * 2, x2, y2));
        for (int i = 1; i < n; i++) {
            double u = i / (double) n, v = 1 - u;
            double fx = v * v * x1 + 2 * v * u * (x1 + x2) / 2 + u * u * x2;
            double fy = v * v * y1 + 2 * v * u * (Math.max(y1, y2) + sag * 2) + u * u * y2;
            double sway = Math.sin(t * 2 + i * 0.9) * flag * 0.10;
            Path2D f = new Path2D.Double();
            f.moveTo(fx - flag * 0.42, fy);
            f.lineTo(fx + flag * 0.42, fy);
            f.lineTo(fx + sway, fy + flag);
            f.closePath();
            g.setColor(cols[i % cols.length]);
            g.fill(f);
        }
    }

    static void bird(Graphics2D g, double x, double y, double s, double t) {
        double f = Math.sin(t * 7) * s * 0.35;
        g.setColor(new Color(0x2E3A44));
        g.setStroke(new BasicStroke((float) Math.max(1.2, s * 0.14), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(x - s, y - f * 0.4, x - s * 0.5, y - s * 0.4 - f, x, y));
        g.draw(new QuadCurve2D.Double(x, y, x + s * 0.5, y - s * 0.4 - f, x + s, y - f * 0.4));
    }

    static void butterfly(Graphics2D g, double x, double y, double s, double t, Color c) {
        double f = 0.35 + 0.65 * Math.abs(Math.sin(t * 9));
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x - s * f, y - s * 0.5, s * f, s));
        g.fill(new Ellipse2D.Double(x, y - s * 0.5, s * f, s));
        g.setColor(new Color(0x3A2814));
        g.fill(new Ellipse2D.Double(x - s * 0.06, y - s * 0.4, s * 0.12, s * 0.8));
    }
}
