package ph.studytabai.report;

import java.util.List;

/** Immutable report = title + tables. Produced by ReportService. */
public final class SimpleReport implements Reportable {
    private final String title;
    private final List<Table> tables;

    public SimpleReport(String title, List<Table> tables) {
        this.title = title;
        this.tables = List.copyOf(tables);
    }

    @Override public String getTitle() { return title; }
    @Override public List<Table> getTables() { return tables; }
}
