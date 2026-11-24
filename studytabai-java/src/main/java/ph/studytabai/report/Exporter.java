package ph.studytabai.report;

import java.io.IOException;
import java.nio.file.Path;

/** ABSTRACTION: turn any report into a file. */
public interface Exporter {
    String extension();
    void export(Reportable report, Path file) throws IOException;
}
