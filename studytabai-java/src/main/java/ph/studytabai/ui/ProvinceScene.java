package ph.studytabai.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A sunny day in a Filipino province: sky and drifting clouds, sea with a bangka, hills and mountains, rice
 * terraces with farmers in salakots, a bahay kubo, a wooden sari-sari store, palms, banderitas, and a carabao
 * cart and a jeepney travelling the road. The static landscape is cached in an image; only the moving parts
 * are redrawn each frame (about 30 fps, paused when the panel is not showing).
 * Use {@link Mode#FULL} for whole screens and {@link Mode#BANNER} for the strip across the top.
 */
public class ProvinceScene extends JPanel {
    public enum Mode { FULL, BANNER }

    private final Mode mode;
    private final boolean bigTitle;
    private BufferedImage cache, overlay;
    private int cw, ch;
    private final long t0 = System.nanoTime() - 3_000_000_000L;
    private double fixedTime = Double.NaN;
    private final Timer timer = new Timer(33, e -> { if (isShowing()) repaint(); });
    private double topInsetFraction = 0;

    /** Full-screen scene, optionally with the big STUDY TA BAI signboard. */
    public ProvinceScene(boolean bigTitle) { this(Mode.FULL, bigTitle); }

    /** Slim strip with the brand on the left. */
    public static ProvinceScene banner() { return new ProvinceScene(Mode.BANNER, false); }

    private ProvinceScene(Mode mode, boolean bigTitle) {
        this.mode = mode;
        this.bigTitle = bigTitle;
        setOpaque(true);
        setBackground(Palette.SKY_LOW);
        if (mode == Mode.BANNER) setPreferredSize(new Dimension(400, 116));
    }

    /** Child components start this fraction of the height down (leaves room for the big signboard). */
    public ProvinceScene contentTop(double fraction) { this.topInsetFraction = fraction; return this; }

    @Override public Insets getInsets() {
        Insets b = super.getInsets();
        return new Insets(b.top + (int) (getHeight() * topInsetFraction), b.left, b.bottom, b.right);
    }

    /** For screenshots and tests: freeze the animation clock. */
    void setFixedTime(double seconds) { fixedTime = seconds; }

    @Override public void addNotify() { super.addNotify(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

    private double now() { return Double.isNaN(fixedTime) ? (System.nanoTime() - t0) / 1e9 : fixedTime; }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int W = getWidth(), H = getHeight();
        if (Theme.isHighContrast() || W < 4 || H < 4) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, W, H);
            if (mode == Mode.BANNER && W > 4) {
                g.setColor(Color.WHITE);
                Palette.aa(g);
                Fonts.draw(g, "STUDY TA BAI", Fonts.display(H * 0.42f), H * 0.35f, H * 0.62f);
            }
            g.dispose();
            return;
        }
        Palette.aa(g);
        if (cache == null || cw != W || ch != H) rebuild(W, H);
        g.drawImage(cache, 0, 0, null);
        dynamic(g, W, H, now());
        if (overlay != null) g.drawImage(overlay, 0, 0, null);   // signboard / brand sit above the clouds
        g.dispose();
    }

    // ------------------------------------------------------------------ layout numbers

    private boolean full() { return mode == Mode.FULL; }
    /** y of the sea horizon. */
    private double seaY(int H) { return full() ? H * 0.47 : H * 0.50; }
    /** y where the terraces start. */
    private double terraceTop(int H) { return full() ? H * 0.60 : H * 0.66; }
    /** y where the road starts. */
    private double roadTop(int H) { return full() ? H * 0.885 : H * 0.90; }

    // ------------------------------------------------------------------ static layer

    private void rebuild(int W, int H) {
        cw = W; ch = H;
        cache = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cache.createGraphics();
        Palette.aa(g);
        boolean full = full();
        double sea = seaY(H), tTop = terraceTop(H), road = roadTop(H);
        Random r = new Random(2024);

        // sky
        g.setPaint(new LinearGradientPaint(0, 0, 0, (float) sea, new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(0x56B0E6), new Color(0xA9DBF2), new Color(0xE3F5F7)}));
        g.fill(new Rectangle2D.Double(0, 0, W, sea + 1));
        // far, faint clouds baked into the sky
        double cs = full ? W * 0.14 : W * 0.08;
        for (int i = 0; i < (full ? 6 : 5); i++)
            Art.cloud(g, W * (0.08 + 0.18 * i) + r.nextInt(40), sea * (0.55 + 0.3 * r.nextDouble()), cs * (0.7 + r.nextDouble() * 0.6), 120);

        // sea
        g.setPaint(new GradientPaint(0, (float) sea, new Color(0x9ADCE6), 0, (float) tTop, new Color(0x2BA6BF)));
        g.fill(new Rectangle2D.Double(0, sea, W, tTop - sea + 2));
        g.setColor(Palette.alpha(Color.WHITE, 140));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Line2D.Double(0, sea, W, sea));

        // mountains and hills
        double ramp = W * 0.14;
        double hf = full ? H : H * 1.15;
        Art.mountains(g, W * 0.26, W, tTop + 2, hf * 0.10, hf * 0.26, new Color(0xA6CBD8), new Color(0x7DB6A6), 11, ramp);
        Art.mountains(g, W * 0.34, W, tTop + 2, hf * 0.06, hf * 0.17, new Color(0x7DB87F), new Color(0x4F9A5C), 23, ramp);
        // headland with a sandy beach on the left (like a quiet cove)
        Art.mountains(g, W * 0.02, W * 0.36, tTop + 2, 0, hf * 0.085, new Color(0x6DB06F), new Color(0x3F8F52), 5, W * 0.10);
        g.setPaint(new GradientPaint(0, (float) (tTop - H * 0.012), new Color(0xF2E2B4), 0, (float) (tTop + H * 0.02), new Color(0xD8C28A)));
        g.fill(new Ellipse2D.Double(W * 0.03, tTop - H * 0.008, W * 0.28, H * 0.03));
        // haze where land meets sea
        g.setPaint(new GradientPaint(0, (float) (tTop - H * 0.1), Palette.alpha(Color.WHITE, 0), 0, (float) tTop, Palette.alpha(Color.WHITE, 90)));
        g.fill(new Rectangle2D.Double(0, tTop - H * 0.1, W, H * 0.1));

        // rice terraces
        Color[][] bands = {
                {new Color(0xA8DA64), new Color(0x86C650)}, {new Color(0xBFE3EC), new Color(0x8FC8DA)},
                {new Color(0x94D058), new Color(0x72B945)}, {new Color(0xE2D26A), new Color(0xC9B44B)},
                {new Color(0x86C850), new Color(0x62AD40)}, {new Color(0xBBE0EA), new Color(0x8AC3D6)},
                {new Color(0x8DCB52), new Color(0x66AF40)}, {new Color(0x76BC49), new Color(0x4F9A3A)}};
        int nb = full ? 8 : 4;
        double bandH = (road - tTop) / nb;
        for (int i = 0; i < nb; i++) {
            Color[] c = bands[i % bands.length];
            double top = tTop + i * bandH, bot = Math.min(road + 4, top + bandH * 1.9);
            Art.terrace(g, 0, W, top, bot, bandH * 0.32, i * 1.7 + 0.5, c[0], c[1], c[0].getBlue() > c[0].getRed() + 20, r);
        }

        // distant palms along the terraces
        double[] fx = full ? new double[]{0.38, 0.43, 0.62, 0.67, 0.90} : new double[]{0.30, 0.34, 0.62, 0.75};
        for (int i = 0; i < fx.length; i++)
            Art.palm(g, W * fx[i], tTop + (full ? H * 0.09 : H * 0.05), full ? H * 0.17 : H * 0.34, 0, 40 + i, i % 2 == 0);

        // bahay kubo
        if (full) Art.bahayKubo(g, W * 0.025, H * 0.725, W * 0.125);
        else Art.bahayKubo(g, W * 0.45, H * 0.90, H * 0.62);

        // road
        g.setPaint(new GradientPaint(0, (float) road, new Color(0xA8A196), 0, H, new Color(0x7B766C)));
        g.fill(new Rectangle2D.Double(0, road, W, H - road));
        g.setColor(new Color(0xE8DFC8));
        g.fill(new Rectangle2D.Double(0, road, W, Math.max(2, H * 0.006)));
        g.setColor(new Color(0xF2C23A));
        g.setStroke(new BasicStroke((float) Math.max(2, H * 0.006), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{(float) (W * 0.03), (float) (W * 0.02)}, 0));
        g.draw(new Line2D.Double(0, road + (H - road) * 0.52, W, road + (H - road) * 0.52));
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 40));
        for (int i = 0; i < 60; i++) g.fill(new Ellipse2D.Double(r.nextDouble() * W, road + (H - road) * (0.1 + 0.85 * r.nextDouble()), 2 + r.nextInt(4), 1.5));

        // sari-sari store beside the road
        if (full) Art.sariSari(g, W * 0.735, road + H * 0.012, W * 0.245, H * 0.40, true);
        else Art.sariSari(g, W * 0.60, road + H * 0.03, H * 1.05, H * 0.62, false);

        g.dispose();

        // signboard / brand go in their own transparent layer, painted after the moving clouds
        overlay = null;
        if ((full && bigTitle) || !full) {
            overlay = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D og = overlay.createGraphics();
            Palette.aa(og);
            if (full) titleBoard(og, W, H); else brand(og, W, H);
            og.dispose();
        }
    }

    private void titleBoard(Graphics2D g, int W, int H) {
        double bw = Math.min(W * 0.66, 820), bh = H * 0.20, bx = (W - bw) / 2, by = H * 0.035;
        // hanging ropes
        g.setColor(new Color(0x8A6A3B));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(bx + bw * 0.12, 0, bx + bw * 0.12, by + 4));
        g.draw(new Line2D.Double(bx + bw * 0.88, 0, bx + bw * 0.88, by + 4));
        // shadow + planks
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 70));
        g.fill(new RoundRectangle2D.Double(bx + 4, by + 7, bw, bh, 18, 18));
        RoundRectangle2D board = new RoundRectangle2D.Double(bx, by, bw, bh, 18, 18);
        Shape oc = g.getClip();
        g.clip(board);
        Color[] planks = {new Color(0xB67F47), new Color(0xA9733E), new Color(0xBC864D)};
        for (int i = 0; i < 3; i++) {
            g.setPaint(new GradientPaint(0, (float) (by + bh * i / 3), planks[i], 0, (float) (by + bh * (i + 1) / 3), Palette.mix(planks[i], Palette.WOOD, 0.4f)));
            g.fill(new Rectangle2D.Double(bx, by + bh * i / 3.0, bw, bh / 3.0 + 1));
            g.setColor(Palette.alpha(Palette.WOOD_DARK, 130));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new Line2D.Double(bx, by + bh * (i + 1) / 3.0, bx + bw, by + bh * (i + 1) / 3.0));
        }
        Random gr = new Random(8);
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 40));
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 40; i++) {
            double gy = by + gr.nextDouble() * bh, gx = bx + gr.nextDouble() * bw;
            g.draw(new Line2D.Double(gx, gy, gx + 30 + gr.nextInt(60), gy + gr.nextDouble() * 2 - 1));
        }
        g.setClip(oc);
        g.setColor(Palette.WOOD_DARK);
        g.setStroke(new BasicStroke(5f));
        g.draw(new RoundRectangle2D.Double(bx + 2, by + 2, bw - 4, bh - 4, 16, 16));
        g.setColor(new Color(0xEBC888));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(bx + 8, by + 8, bw - 16, bh - 16, 12, 12));
        // nails
        g.setColor(new Color(0x3E2A17));
        for (double nx : new double[]{bx + 16, bx + bw - 16}) for (double ny : new double[]{by + 16, by + bh - 16}) g.fill(new Ellipse2D.Double(nx - 3, ny - 3, 6, 6));

        // lettering
        float fs = (float) (bh * 0.56);
        Font f = Fonts.display(fs);
        float wS = Fonts.width("STUDY ", f), wT = Fonts.width("TA ", f), wB = Fonts.width("BAI", f);
        float total = wS + wT + wB, maxW = (float) (bw * 0.80);
        if (total > maxW) { f = f.deriveFont(fs * maxW / total); wS = Fonts.width("STUDY ", f); wT = Fonts.width("TA ", f); wB = Fonts.width("BAI", f); total = wS + wT + wB; }
        float x0 = (float) (bx + (bw - total) / 2), base = (float) (by + bh * 0.60);
        Color ink = new Color(0x3A2210);
        Palette.outlineText(g, "STUDY ", f, x0, base, new Color(0xFFF3D2), ink, 2.4f);
        Palette.outlineText(g, "TA ", f, x0 + wS, base, new Color(0xFFD24A), ink, 2.4f);
        Palette.outlineText(g, "BAI", f, x0 + wS + wT, base, new Color(0xCFF08A), ink, 2.4f);
        Font sf = Fonts.sign((float) (bh * 0.15));
        String tag = "MAG-ARAL TAYO!   •   LEARN • PLAY • GROW";
        g.setColor(new Color(0xFFE9B0));
        Fonts.draw(g, tag, sf, (float) (bx + bw / 2 - Fonts.width(tag, sf) / 2), (float) (by + bh * 0.88));
        // salakot at both ends of the board
        Art.salakot(g, bx + bw * 0.045, by + bh * 0.52, bh * 0.34);
        Art.salakot(g, bx + bw * 0.955, by + bh * 0.52, bh * 0.34);
    }

    private void brand(Graphics2D g, int W, int H) {
        double bh = H * 0.62, by = H * 0.06;
        Font f = Fonts.display((float) (H * 0.42));
        float tw = Fonts.width("STUDY TA BAI", f);
        double bw = tw + H * 1.0, bx = H * 0.12;
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 70));
        g.fill(new RoundRectangle2D.Double(bx + 3, by + 5, bw, bh, 14, 14));
        g.setPaint(new GradientPaint(0, (float) by, new Color(0xB67F47), 0, (float) (by + bh), new Color(0x93602F)));
        g.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 14, 14));
        g.setColor(Palette.WOOD_DARK);
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new RoundRectangle2D.Double(bx + 1.5, by + 1.5, bw - 3, bh - 3, 12, 12));
        g.setColor(Palette.alpha(Palette.WOOD_DARK, 70));
        g.setStroke(new BasicStroke(1f));
        for (int i = 1; i < 3; i++) g.draw(new Line2D.Double(bx + 6, by + bh * i / 3, bx + bw - 6, by + bh * i / 3));
        Art.salakot(g, bx + H * 0.30, by + bh * 0.62, H * 0.27);
        float x = (float) (bx + H * 0.64), base = (float) (by + bh * 0.62);
        Color ink = new Color(0x3A2210);
        Palette.outlineText(g, "STUDY ", f, x, base, new Color(0xFFF3D2), ink, Math.max(1.4f, H * 0.014f));
        float w1 = Fonts.width("STUDY ", f), w2 = Fonts.width("TA ", f);
        Palette.outlineText(g, "TA ", f, x + w1, base, new Color(0xFFD24A), ink, Math.max(1.4f, H * 0.014f));
        Palette.outlineText(g, "BAI", f, x + w1 + w2, base, new Color(0xCFF08A), ink, Math.max(1.4f, H * 0.014f));
        Font sf = Fonts.sign((float) (H * 0.115));
        g.setColor(new Color(0xFFE9B0));
        Fonts.draw(g, "MAG-ARAL TAYO!", sf, x + 2, (float) (by + bh * 0.92));
    }

    // ------------------------------------------------------------------ moving layer

    private void dynamic(Graphics2D g, int W, int H, double t) {
        boolean full = full();
        double sea = seaY(H), tTop = terraceTop(H), road = roadTop(H);

        // sun glow and rays
        double sunR = full ? H * 0.05 : H * 0.15;
        Art.sun(g, full ? W * 0.88 : W * 0.93, full ? H * 0.14 : H * 0.26, sunR, t);

        // drifting clouds
        int nc = full ? 5 : 4;
        for (int i = 0; i < nc; i++) {
            double cwid = (full ? W * 0.20 : W * 0.12) * (0.75 + 0.1 * i);
            double v = 7 + i * 3.3, span = W + cwid * 2;
            double x = ((t * v + i * span * 0.37) % span) - cwid;
            double y = (full ? H * (0.10 + 0.075 * ((i * 3) % 5)) : H * (0.18 + 0.1 * (i % 3)));
            Art.cloud(g, x, y, cwid, 225);
        }
        // birds
        for (int i = 0; i < (full ? 4 : 2); i++) {
            double v = 26 + i * 6, span = W + 200;
            double x = ((t * v + i * 0.31 * span) % span) - 100;
            double y = (full ? H * (0.22 + 0.04 * i) : H * (0.3 + 0.1 * i)) + Math.sin(t * 0.8 + i) * 8;
            Art.bird(g, x, y, full ? H * 0.012 : H * 0.05, t + i);
        }

        // bangka on the sea
        double bl = full ? W * 0.075 : W * 0.04;
        double bx = ((t * 6 + W * 0.05) % (W * 0.34 + bl * 2)) - bl;
        Art.bangka(g, bx, sea + (tTop - sea) * 0.55, bl, t);
        // sparkles on the water
        g.setColor(Palette.alpha(Color.WHITE, 190));
        for (int i = 0; i < 26; i++) {
            double a = (Math.sin(t * 1.7 + i * 2.3) + 1) / 2;
            if (a < 0.55) continue;
            double sx = (i * 137.0) % (W * 0.36), sy = sea + 3 + ((i * 53.0) % Math.max(4, tTop - sea - 8));
            g.fill(new Ellipse2D.Double(sx, sy, 2 + a * 3, 1.4));
        }

        // palms (they sway)
        if (full) {
            Art.palm(g, W * 0.155, H * 0.745, H * 0.30, t, 1, true);
            Art.palm(g, W * 0.215, H * 0.73, H * 0.34, t, 2, false);
            Art.palm(g, W * 0.695, H * 0.70, H * 0.30, t, 3, true);
        } else {
            Art.palm(g, W * 0.42, H * 0.95, H * 0.85, t, 1, true);
            Art.palm(g, W * 0.70, H * 0.95, H * 0.80, t, 2, false);
        }

        // farmers planting seedlings
        if (full) {
            double s = H * 0.125;
            Color[] shirts = {new Color(0x8FA8B8), new Color(0xC9784A), new Color(0xE6D8B0), new Color(0x6F9E6A), new Color(0xB86C7A)};
            double[][] pos = {{0.045, 0.835}, {0.105, 0.795}, {0.175, 0.855}, {0.245, 0.80}, {0.575, 0.80}, {0.645, 0.855}};
            for (int i = 0; i < pos.length; i++)
                Art.farmerPlanting(g, W * pos[i][0], H * pos[i][1], s * (0.9 + 0.1 * (i % 3)), t, i * 1.3, i % 2 == 0 ? 1 : -1, shirts[i % shirts.length]);
        } else {
            double s = H * 0.40;
            Art.farmerPlanting(g, W * 0.12, H * 0.97, s, t, 0.4, 1, new Color(0xC9784A));
            Art.farmerPlanting(g, W * 0.32, H * 0.93, s * 0.9, t, 2.1, -1, new Color(0x8FA8B8));
            Art.farmerPlanting(g, W * 0.62, H * 0.96, s * 0.9, t, 3.3, 1, new Color(0xE6D8B0));
        }

        // butterflies / drifting sampaguita petals
        if (full) {
            for (int i = 0; i < 3; i++) {
                double bx2 = W * (0.10 + 0.08 * i) + Math.sin(t * 0.7 + i * 2) * 40, by2 = H * (0.66 + 0.05 * i) + Math.sin(t * 1.3 + i) * 16;
                Art.butterfly(g, bx2, by2, H * 0.014, t + i, i == 1 ? new Color(0xFFD24A) : new Color(0xFFFFFF));
            }
            for (int i = 0; i < 16; i++) {
                double v = 14 + (i * 7) % 12, off = (i * 331.0) % (H + 20);
                double y = ((t * v + off) % (H + 20)) - 10;
                double x = ((i * 449.0) % W) + Math.sin(t * 0.8 + i) * 22;
                g.setColor(new Color(255, 255, 250, 200));
                AffineTransform k = g.getTransform();
                g.rotate(t * 0.8 + i, x, y);
                g.fill(new Ellipse2D.Double(x - 3, y - 1.6, 6, 3.2));
                g.setTransform(k);
            }
        }

        // fiesta banderitas
        if (full) {
            Art.bunting(g, -10, H * 0.26, W * 0.5, H * 0.27, H * 0.03, t, 16, H * 0.034);
            Art.bunting(g, W * 0.5, H * 0.27, W + 10, H * 0.25, H * 0.03, t + 1, 16, H * 0.034);
        } else {
            Art.bunting(g, W * 0.30, H * 0.07, W * 0.62, H * 0.10, H * 0.04, t, 12, H * 0.10);
        }

        // vehicles on the road
        double ground = full ? H * 0.972 : H * 0.985;
        double carLen = full ? W * 0.115 : H * 0.42;
        travel(g, W, t, carLen * 1.75, 38, 0, (x, right) -> Art.carabaoCart(g, x, ground, carLen, right, t));
        double jLen = full ? W * 0.19 : H * 0.62;
        travel(g, W, t, jLen, 24, 11, (x, right) -> Art.jeepney(g, x, ground - (full ? 0 : 0), jLen, right, t));
    }

    private interface Draw { void at(double x, boolean right); }

    /** Runs {@code d} across the screen, alternating direction each pass, with a pause between passes. */
    private void travel(Graphics2D g, int W, double t, double objLen, double period, double offset, Draw d) {
        double tt = t + offset;
        long n = (long) Math.floor(tt / period);
        double within = tt - n * period, travelTime = period * 0.72;
        if (within > travelTime) return;
        double f = within / travelTime;
        boolean right = n % 2 == 0;
        double span = W + objLen * 2;
        double x = right ? -objLen + f * span : W + objLen - f * span;
        d.at(x, right);
    }
}
