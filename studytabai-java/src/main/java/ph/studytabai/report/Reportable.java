package ph.studytabai.report;

import java.util.List;

/** POLYMORPHISM: exporters and the UI work with any report through this interface. */
public interface Reportable {
    String getTitle();
    List<Table> getTables();
}
