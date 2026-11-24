package ph.studytabai.session;

public enum ScoringMode {
    SPEED("Classic (speed bonus)", new SpeedScoring()),
    FLAT("Practice (flat points)", new FlatScoring());

    private final String label;
    private final ScoringStrategy strategy;

    ScoringMode(String label, ScoringStrategy strategy) { this.label = label; this.strategy = strategy; }

    public ScoringStrategy strategy() { return strategy; }
    @Override public String toString() { return label; }
}
