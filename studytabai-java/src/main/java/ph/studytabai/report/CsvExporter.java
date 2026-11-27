package ph.studytabai.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** RFC 4180 CSV. Cells that look like spreadsheet formulas are neutralised (CSV injection). */
public final class CsvExporter implements Exporter {
    @Override public String extension() { return "csv"; }

    @Override
    public void export(Reportable report, Path file) throws IOException {
        StringBuilder sb = new StringBuilder("\uFEFF");   // BOM so Excel reads UTF-8
        sb.append(cell(report.getTitle())).append("\r\n");
        for (Table t : report.getTables()) {
            sb.append("\r\n").append(cell(t.getTitle())).append("\r\n");
            line(sb, t.getHeader());
            for (List<String> r : t.getRows()) line(sb, r);
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(cells.get(i)));
        }
        sb.append("\r\n");
    }

    static String cell(String v) {
        if (v == null) v = "";
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 && !v.matches("-?\\d+(\\.\\d+)?%?")) v = "'" + v;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) v = "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}
