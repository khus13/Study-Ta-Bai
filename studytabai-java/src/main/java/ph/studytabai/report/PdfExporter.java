package ph.studytabai.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Renders each table as aligned monospaced text in a simple PDF. */
public final class PdfExporter implements Exporter {
    private static final int MAX_COL = 28;

    @Override public String extension() { return "pdf"; }

    @Override
    public void export(Reportable report, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(report.getTitle());
        lines.add("=".repeat(Math.min(90, report.getTitle().length())));
        for (Table t : report.getTables()) {
            lines.add("");
            lines.add(t.getTitle());
            int cols = t.getHeader().size();
            int[] w = new int[cols];
            for (int c = 0; c < cols; c++) {
                w[c] = Math.min(MAX_COL, t.getHeader().get(c).length());
                for (List<String> r : t.getRows()) w[c] = Math.max(w[c], Math.min(MAX_COL, r.get(c).length()));
            }
            lines.add(format(t.getHeader(), w));
            lines.add("-".repeat(Math.min(90, java.util.Arrays.stream(w).sum() + 2 * cols)));
            if (t.getRows().isEmpty()) lines.add("(no data)");
            for (List<String> r : t.getRows()) lines.add(format(r, w));
        }
        Files.write(file, SimplePdf.fromLines(lines));
    }

    private static String format(List<String> cells, int[] w) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < w.length; c++) {
            String v = cells.get(c);
            if (v.length() > w[c]) v = v.substring(0, w[c] - 1) + "~";
            sb.append(v).append(" ".repeat(w[c] - v.length() + 2));
        }
        return sb.toString().stripTrailing();
    }
}
