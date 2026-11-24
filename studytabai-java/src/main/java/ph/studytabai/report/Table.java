package ph.studytabai.report;

import java.util.List;

/** One table of a report: title, header row and data rows (all text, ready to display or export). */
public final class Table {
    private final String title;
    private final List<String> header;
    private final List<List<String>> rows;

    public Table(String title, List<String> header, List<List<String>> rows) {
        this.title = title;
        this.header = List.copyOf(header);
        this.rows = List.copyOf(rows);
    }

    public String getTitle() { return title; }
    public List<String> getHeader() { return header; }
    public List<List<String>> getRows() { return rows; }
}
