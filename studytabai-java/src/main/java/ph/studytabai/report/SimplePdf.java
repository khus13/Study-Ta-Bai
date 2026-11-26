package ph.studytabai.report;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Minimal text-only PDF writer (Courier, A4). No external library needed. */
public final class SimplePdf {
    private static final int LINES_PER_PAGE = 62;
    private static final int MAX_CHARS = 92;

    private SimplePdf() {}

    public static byte[] fromLines(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += LINES_PER_PAGE)
            pages.add(lines.subList(i, Math.min(lines.size(), i + LINES_PER_PAGE)));
        if (pages.isEmpty()) pages.add(List.of(""));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");

        int fontObj = 3;
        int firstPageObj = 4;
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(firstPageObj + 2 * i).append(" 0 R ");

        obj(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>");

        for (int i = 0; i < pages.size(); i++) {
            int contentObj = firstPageObj + 2 * i + 1;
            obj(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 " + fontObj
                    + " 0 R >> >> /Contents " + contentObj + " 0 R >>");
            StringBuilder c = new StringBuilder("BT\n/F1 9 Tf\n11 TL\n40 800 Td\n");
            for (String l : pages.get(i)) c.append('(').append(escape(l)).append(") Tj T*\n");
            c.append("ET\n");
            byte[] data = c.toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, (offsets.size()) + " 0 obj\n<< /Length " + data.length + " >>\nstream\n");
            out.writeBytes(data);
            write(out, "endstream\nendobj\n");
        }

        int xref = out.size();
        write(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) write(out, String.format("%010d 00000 n \n", off));
        write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void obj(ByteArrayOutputStream out, List<Integer> offsets, String body) {
        offsets.add(out.size());
        write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String s) { out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1)); }

    /** Keeps Latin-1 characters, replaces others, and escapes PDF string syntax. */
    static String escape(String s) {
        if (s.length() > MAX_CHARS) s = s.substring(0, MAX_CHARS - 1) + "~";
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '(' || ch == ')' || ch == '\\') sb.append('\\').append(ch);
            else if (ch >= 32 && ch <= 126 || ch >= 160 && ch <= 255) sb.append(ch);
            else sb.append('?');
        }
        return sb.toString();
    }
}
