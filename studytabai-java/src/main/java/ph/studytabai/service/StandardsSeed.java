package ph.studytabai.service;

import ph.studytabai.model.Standard;

import java.util.List;

/** Placeholder standards so the tagging UI works out of the box. Teachers should add their real framework codes. */
public final class StandardsSeed {
    private StandardsSeed() {}

    public static List<Standard> samples() {
        return List.of(
                new Standard("Sample", "SAMPLE-1", "Sample standard: identify main ideas (replace with your real codes)"),
                new Standard("Sample", "SAMPLE-2", "Sample standard: use evidence to support answers"),
                new Standard("Sample", "SAMPLE-3", "Sample standard: solve multi-step problems"));
    }
}
