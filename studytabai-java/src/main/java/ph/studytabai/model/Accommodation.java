package ph.studytabai.model;

import java.io.Serializable;

/** Per-student or per-session accessibility settings. */
public final class Accommodation implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean textToSpeech;
    private double timeMultiplier = 1.0;
    private boolean highContrast;
    private double fontScale = 1.0;

    public boolean isTextToSpeech() { return textToSpeech; }
    public void setTextToSpeech(boolean v) { this.textToSpeech = v; }

    public double getTimeMultiplier() { return timeMultiplier; }
    public void setTimeMultiplier(double m) {
        if (m < 1.0 || m > 3.0) throw new IllegalArgumentException("Time multiplier must be 1.0-3.0");
        this.timeMultiplier = m;
    }

    public boolean isHighContrast() { return highContrast; }
    public void setHighContrast(boolean v) { this.highContrast = v; }

    public double getFontScale() { return fontScale; }
    public void setFontScale(double s) {
        if (s < 0.8 || s > 2.0) throw new IllegalArgumentException("Font scale must be 0.8-2.0");
        this.fontScale = s;
    }

    public int adjustedTimeSec(int baseSec) { return (int) Math.ceil(baseSec * timeMultiplier); }
}
