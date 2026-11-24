package ph.studytabai.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts plain text from TXT, DOCX and PDF. PDF needs Apache PDFBox on the classpath (loaded by reflection). */
public final class DocumentParser {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final int MAX_CHARS = 400_000;

    public String parse(Path path) throws DocumentException {
        String name = path.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        long size;
        try { size = Files.size(path); } catch (IOException e) { throw new DocumentException("Could not read the file."); }
        if (size == 0) throw new DocumentException("The file is empty.");
        if (size > MAX_BYTES) throw new DocumentException("File is too large (max 10 MB).");

        String text = switch (ext) {
            case "txt" -> txt(path);
            case "docx" -> docx(path);
            case "pdf" -> pdf(path);
            default -> throw new DocumentException("Only PDF, DOCX, and TXT files are supported.");
        };
        text = normalize(text);
        if (text.isEmpty()) throw new DocumentException("No readable text found. Scanned or image-only files are not supported.");
        return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
    }

    private String txt(Path p) throws DocumentException {
        try {
            byte[] b = Files.readAllBytes(p);
            for (int i = 0; i < Math.min(b.length, 4096); i++)
                if (b[i] == 0) throw new DocumentException("This does not look like a text file.");
            try {
                return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(b)).toString();
            } catch (CharacterCodingException e) {
                return new String(b, java.nio.charset.Charset.forName("windows-1252"));
            }
        } catch (IOException e) {
            throw new DocumentException("Could not read the file.");
        }
    }

    private String docx(Path p) throws DocumentException {
        try (InputStream in = Files.newInputStream(p)) {
            byte[] magic = in.readNBytes(4);
            if (magic.length < 4 || magic[0] != 'P' || magic[1] != 'K') throw new DocumentException("This does not look like a valid DOCX file.");
        } catch (IOException e) {
            throw new DocumentException("Could not read the file.");
        }
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(p))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equals("word/document.xml")) {
                    String xml = new String(zin.readNBytes((int) MAX_BYTES), StandardCharsets.UTF_8);
                    xml = xml.replace("</w:p>", "\n").replaceAll("<w:(tab|br)[^>]*/>", " ");
                    return unescape(xml.replaceAll("<[^>]+>", ""));
                }
            }
        } catch (IOException e) {
            throw new DocumentException("Could not open the DOCX file.");
        }
        throw new DocumentException("This does not look like a valid DOCX file.");
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
    }

    private String pdf(Path p) throws DocumentException {
        try (InputStream in = Files.newInputStream(p)) {
            byte[] magic = in.readNBytes(5);
            if (!new String(magic, StandardCharsets.ISO_8859_1).equals("%PDF-")) throw new DocumentException("This does not look like a valid PDF.");
        } catch (IOException e) {
            throw new DocumentException("Could not read the file.");
        }
        try {
            Class<?> loader = Class.forName("org.apache.pdfbox.Loader");
            Class<?> docClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            Object doc = loader.getMethod("loadPDF", File.class).invoke(null, p.toFile());
            try {
                Object stripper = stripperClass.getDeclaredConstructor().newInstance();
                return (String) stripperClass.getMethod("getText", docClass).invoke(stripper, doc);
            } finally {
                ((AutoCloseable) doc).close();
            }
        } catch (ClassNotFoundException e) {
            throw new DocumentException("PDF support needs the Apache PDFBox library (see README).");
        } catch (Exception e) {
            throw new DocumentException("Could not read this PDF. It may be encrypted or corrupted.");
        }
    }

    static String normalize(String t) {
        t = t.replace("\r\n", "\n").replace('\r', '\n');
        t = t.replaceAll("[\\p{Cc}&&[^\\n\\t]]", "");
        t = t.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n");
        return t.trim();
    }
}
