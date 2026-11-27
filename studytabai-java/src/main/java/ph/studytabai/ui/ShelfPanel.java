package ph.studytabai.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A sari-sari store front used as a list: items hang from the striped awning like sachets and snacks, each one a
 * brown wooden button (big white title, small centred subtitle) laid out in two or three columns, while the action
 * buttons sit on the wooden counter at the bottom.
 */
public final class ShelfPanel<T> extends JPanel {
    private final Function<T, String> titleOf, subtitleOf, tagOf;
    private final int maxCols;
    private final String emptyText;
    private final Grid grid = new Grid();
    private final CounterPanel counter = new CounterPanel();
    private final List<T> items = new ArrayList<>();
    private T selected;
    private Function<T, Object> keyOf = t -> t;
    private Consumer<T> onActivate = t -> { };
    private Consumer<T> onSelect = t -> { };

    public ShelfPanel(Function<T, String> title, Function<T, String> subtitle, Function<T, String> tag, int maxCols, String emptyText) {
        super(new BorderLayout());
        this.titleOf = title;
        this.subtitleOf = subtitle;
        this.tagOf = tag;
        this.maxCols = maxCols;
        this.emptyText = emptyText;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        add(new Awning(), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(grid, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(24);
        add(sp, BorderLayout.CENTER);
        add(counter, BorderLayout.SOUTH);
        Ui.name(grid, "Shelf of items");
    }

    // ------------------------------------------------------------------ API

    public void onActivate(Consumer<T> c) { this.onActivate = c; }
    public void onSelect(Consumer<T> c) { this.onSelect = c; }
    /** How to recognise the same item after the list is reloaded (e.g. by id). */
    public void keyBy(Function<T, Object> k) { this.keyOf = k; }
    private boolean same(T a, T b) { return a != null && b != null && Objects.equals(keyOf.apply(a), keyOf.apply(b)); }
    public T getSelected() { return selected; }

    /** Put the action buttons on the counter. */
    public void counter(Component... buttons) {
        counter.removeAll();
        for (Component b : buttons) counter.add(b);
        counter.revalidate();
    }

    public void setItems(List<T> newItems) {
        T keep = selected;
        items.clear();
        items.addAll(newItems);
        grid.removeAll();
        selected = null;
        for (T it : items) {
            Item b = new Item(it);
            grid.add(b);
            if (same(it, keep)) selected = it;
        }
        refreshSelection();
        grid.revalidate();
        grid.repaint();
    }

    public void setSelected(T t) {
        selected = t;
        refreshSelection();
        onSelect.accept(t);
    }

    private void refreshSelection() {
        for (Component c : grid.getComponents()) if (c instanceof ShelfPanel<?>.Item it) it.repaint();
    }

    private static String plain(String s) { return s == null ? "" : s; }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth(), h = getHeight();
        if (Theme.isHighContrast()) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.dispose();
            return;
        }
        // back wall: vertical planks
        for (int x = 0, i = 0; x < w; x += 44, i++) {
            g.setColor(i % 2 == 0 ? new Color(0xEBD8AE) : new Color(0xE2CC9C));
            g.fillRect(x, 0, 45, h);
            g.setColor(Palette.alpha(Palette.WOOD_DARK, 40));
            g.drawLine(x, 0, x, h);
        }
        // side posts
        for (int px : new int[]{0, w - 14}) {
            g.setPaint(new GradientPaint(px, 0, Palette.WOOD_LIGHT, px + 14, 0, Palette.WOOD_DARK));
            g.fillRect(px, 0, 14, h);
        }
        g.dispose();
    }

    // ------------------------------------------------------------------ awning

    private static final class Awning extends JComponent {
        Awning() { setOpaque(false); }
        @Override public Dimension getPreferredSize() { return new Dimension(100, 58); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Palette.aa(g);
            int w = getWidth();
            boolean hc = Theme.isHighContrast();
            int stripe = 36, top = -16, bodyH = 36;
            // roof stripes with scalloped edge
            for (int x = -16, i = 0; x < w + 16; x += stripe, i++) {
                Color c = hc ? (i % 2 == 0 ? Color.YELLOW : Color.WHITE) : (i % 2 == 0 ? Palette.LEAF : new Color(0xFFF6DC));
                g.setColor(Palette.alpha(Color.BLACK, 40));
                g.fill(new Ellipse2D.Double(x + 1, top + bodyH - stripe / 2.0 + 5, stripe, stripe));
                g.setColor(c);
                g.fill(new Rectangle2D.Double(x, top, stripe, bodyH));
                g.fill(new Ellipse2D.Double(x, top + bodyH - stripe / 2.0, stripe, stripe));
                g.setColor(Palette.alpha(hc ? Color.BLACK : Palette.WOOD_DARK, 90));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Arc2D.Double(x, top + bodyH - stripe / 2.0, stripe, stripe, 180, 180, Arc2D.OPEN));
            }
            g.setColor(hc ? Color.WHITE : Palette.WOOD_DARK);
            g.fillRect(0, 0, w, 5);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ counter

    private final class CounterPanel extends JPanel {
        CounterPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 10, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 0, 10, 0));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int w = getWidth(), h = getHeight();
            if (Theme.isHighContrast()) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, 3);
            } else {
                g.setPaint(new GradientPaint(0, 0, Palette.WOOD_LIGHT, 0, 14, new Color(0xA8713F)));
                g.fillRect(-20, 0, w + 40, 14);                 // table top
                g.setColor(new Color(0xE4B57A));
                g.fillRect(-20, 0, w + 40, 2);
                g.setPaint(new GradientPaint(0, 14, Palette.WOOD, 0, h, Palette.WOOD_DARK));
                g.fillRect(-20, 14, w + 40, h - 14);            // front apron
                g.setColor(Palette.alpha(Color.BLACK, 45));
                for (int x = 0; x < w; x += 70) g.drawLine(x, 14, x, h);
                g.setColor(Palette.alpha(Color.BLACK, 40));
                g.fillRect(-20, 14, w + 40, 4);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // ------------------------------------------------------------------ grid

    private final class Grid extends JPanel implements Scrollable {
        private static final int GAP = 14, PAD = 8;

        Grid() { super(null); setOpaque(false); }

        private int cols(int width) {
            int fit = Math.max(1, (width - 2 * PAD + GAP) / (210 + GAP));
            return Math.min(maxCols, fit);
        }

        private int rowH() {
            return Item.hangH() + Item.btnH() + 14;
        }

        @Override public Dimension getPreferredSize() {
            int w = getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 640;
            int n = Math.max(1, getComponentCount());
            int rows = (n + cols(w) - 1) / cols(w);
            return new Dimension(w, PAD * 2 + rows * rowH());
        }

        @Override public void doLayout() {
            int w = getWidth(), c = cols(w);
            int cw = (w - 2 * PAD - (c - 1) * GAP) / c, rh = rowH();
            for (int i = 0; i < getComponentCount(); i++)
                getComponent(i).setBounds(PAD + (i % c) * (cw + GAP), PAD + (i / c) * rh, cw, rh - 4);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (getComponentCount() > 0) return;
            Graphics2D g = (Graphics2D) g0.create();
            Palette.aa(g);
            Font f = Fonts.display((float) (12 * 1.15 * 1.5 * Theme.getScale()));
            String[] lines = emptyText.split("\n");
            int y = Math.max(50, getHeight() / 3);
            for (String s : lines) {
                float x = (getWidth() - Fonts.width(s, f)) / 2f;
                g.setColor(Theme.isHighContrast() ? Color.WHITE : Palette.WOOD_DARK);
                Fonts.draw(g, s, f, x, y);
                y += g.getFontMetrics(f).getHeight() + 4;
            }
            g.dispose();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(640, 300); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(60, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ------------------------------------------------------------------ one hanging item

    private final class Item extends JComponent {
        private final T value;
        private boolean hover;

        Item(T value) {
            this.value = value;
            setFocusable(true);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String t = plain(titleOf.apply(value)), st = plain(subtitleOf.apply(value));
            getAccessibleContext().setAccessibleName(t + " " + st);
            setToolTipText(t + " " + st);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                    ShelfPanel.this.setSelected(Item.this.value);
                    if (e.getClickCount() == 2) onActivate.accept(Item.this.value);
                }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pick");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
            getActionMap().put("pick", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { ShelfPanel.this.setSelected(Item.this.value); }
            });
            getActionMap().put("open", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { ShelfPanel.this.setSelected(Item.this.value); onActivate.accept(Item.this.value); }
            });
        }

        @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() {
                    @Override public javax.accessibility.AccessibleRole getAccessibleRole() { return javax.accessibility.AccessibleRole.PUSH_BUTTON; }
                    @Override public javax.accessibility.AccessibleStateSet getAccessibleStateSet() {
                        javax.accessibility.AccessibleStateSet st = super.getAccessibleStateSet();
                        if (same(selected, value)) st.add(javax.accessibility.AccessibleState.SELECTED);
                        return st;
                    }
                };
            }
            return accessibleContext;
        }

        static int hangH() { return 30; }

        static Font titleFont(double mult) { return Fonts.sign((float) (12 * 1.15 * 1.4 * mult * Theme.getScale())); }
        static Font subFont() { return Fonts.body(Font.PLAIN, (float) (12 * 1.15 * 0.95 * Theme.getScale())); }
        static Font tagFont() { return Fonts.sign((float) (12 * 0.78 * Theme.getScale())); }
        static final double MAX_MULT = 1.55;

        /** Button height: the largest title plus the subtitle, with the same padding above and below. */
        static int btnH() {
            JLabel c = new JLabel();
            int th = c.getFontMetrics(titleFont(MAX_MULT)).getHeight(), sh = c.getFontMetrics(subFont()).getHeight();
            return (int) ((th + sh) * 1.28) + 6;
        }

        private String fit(String s, Font f, float max) {
            if (Fonts.width(s, f) <= max) return s;
            String t = s;
            while (t.length() > 1 && Fonts.width(t + "…", f) > max) t = t.substring(0, t.length() - 1);
            return t.trim() + "…";
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Palette.aa(g);
            boolean sel = same(selected, value), hc = Theme.isHighContrast();
            int w = getWidth(), bh = btnH(), by = hangH() - (sel ? 3 : 0), bx = 6, bw = w - 12;
            // rail + two ropes
            g.setPaint(hc ? Color.WHITE : new GradientPaint(0, 2, new Color(0xD9B873), 0, 10, new Color(0x9B7B3F)));
            g.fill(new RoundRectangle2D.Double(-8, 2, w + 16, 8, 8, 8));
            g.setColor(hc ? Color.WHITE : new Color(0x7A5A2A));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int mid = w / 2;
            g.draw(new Line2D.Double(mid, 8, bx + bw * 0.20, by + 3));
            g.draw(new Line2D.Double(mid, 8, bx + bw * 0.80, by + 3));
            g.setColor(hc ? Color.WHITE : new Color(0x5C3C1B));
            g.fill(new Ellipse2D.Double(mid - 4, 5, 8, 8));
            // shadow
            g.setColor(Palette.alpha(Color.BLACK, hc ? 0 : 55));
            g.fill(new RoundRectangle2D.Double(bx + 3, by + 6, bw, bh, 16, 16));
            // wooden tag body
            RoundRectangle2D body = new RoundRectangle2D.Double(bx, by, bw, bh, 16, 16);
            Color top, bot, ink, subInk;
            if (hc) { top = bot = sel ? Color.WHITE : Color.YELLOW; ink = subInk = Color.BLACK; }
            else if (sel) { top = new Color(0x3E9A5E); bot = Palette.LEAF_DARK; ink = Color.WHITE; subInk = new Color(0xF2F7D8); }
            else if (hover) { top = new Color(0xA9703B); bot = new Color(0x7B4C27); ink = Color.WHITE; subInk = new Color(0xFBE9C2); }
            else { top = new Color(0x9A6535); bot = new Color(0x6E4322); ink = Color.WHITE; subInk = new Color(0xF6DFB4); }
            g.setPaint(new GradientPaint(0, by, top, 0, by + bh, bot));
            g.fill(body);
            if (!hc) {
                Shape oc = g.getClip();
                g.clip(body);
                g.setColor(Palette.alpha(Color.BLACK, 38));
                g.setStroke(new BasicStroke(1f));
                for (int i = 1; i < 4; i++) g.draw(new Line2D.Double(bx, by + bh * i / 4.0, bx + bw, by + bh * i / 4.0));
                g.setColor(Palette.alpha(Color.WHITE, 40));
                g.fill(new Rectangle2D.Double(bx, by, bw, 3));
                g.setClip(oc);
            }
            g.setStroke(new BasicStroke(sel ? 3f : 2f));
            g.setColor(hc ? (sel ? Color.WHITE : Color.BLACK) : (sel ? new Color(0xFFD24A) : Palette.WOOD_DARK));
            g.draw(new RoundRectangle2D.Double(bx + 1, by + 1, bw - 2, bh - 2, 15, 15));
            if (hc && sel) { g.setColor(Color.BLACK); g.setStroke(new BasicStroke(1.5f)); g.draw(new RoundRectangle2D.Double(bx + 5, by + 5, bw - 10, bh - 10, 10, 10)); }
            if (isFocusOwner()) {
                g.setColor(hc ? Color.BLACK : new Color(0xFFF1B8));
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
                g.draw(new RoundRectangle2D.Double(bx + 5, by + 5, bw - 10, bh - 10, 11, 11));
            }
            // kind tag (QUIZ / CARDS) hangs over the top edge so it never takes padding from the text
            String tag = plain(tagOf.apply(value)).toUpperCase(java.util.Locale.ROOT);
            Font tf = tagFont(), f2 = subFont();
            FontMetrics tm = g.getFontMetrics(tf), m2 = g.getFontMetrics(f2);
            if (!tag.isEmpty()) {
                float tw = Fonts.width(tag, tf) + 14;
                double tx = bx + bw - tw - 8, th = tm.getHeight() + 2, ty = by - th / 2.0;
                g.setColor(hc ? Color.BLACK : new Color(0xFFF6DC));
                g.fill(new RoundRectangle2D.Double(tx, ty, tw, th, 8, 8));
                g.setColor(hc ? (sel ? Color.WHITE : Color.YELLOW) : new Color(0x5B3A1F));
                Fonts.draw(g, tag, tf, (float) (tx + 7), (float) (ty + 1 + tm.getAscent()));
            }
            // title: as big as fits across the sign (up to a cap), subtitle small and centred; block centred vertically
            float maxText = bw - 28;
            String rawTitle = plain(titleOf.apply(value));
            double mult = MAX_MULT;
            Font f1 = titleFont(mult);
            while (mult > 0.95 && Fonts.width(rawTitle, f1) > maxText) { mult -= 0.05; f1 = titleFont(mult); }
            FontMetrics m1 = g.getFontMetrics(f1);
            String title = fit(rawTitle, f1, maxText), sub = fit(plain(subtitleOf.apply(value)), f2, maxText);
            int blockH = m1.getHeight() + m2.getHeight();
            float y1 = by + (bh - blockH) / 2f + m1.getAscent();
            g.setColor(ink);
            Fonts.draw(g, title, f1, (w - Fonts.width(title, f1)) / 2f, y1);
            g.setColor(subInk);
            Fonts.draw(g, sub, f2, (w - Fonts.width(sub, f2)) / 2f, y1 - m1.getAscent() + m1.getHeight() + m2.getAscent());
            g.dispose();
        }
    }
}
