package ph.studytabai.util;

import java.util.*;

/** Small dependency-free JSON reader/writer (objects, arrays, strings, numbers, booleans, null). */
public final class Json {
    private static final int MAX_DEPTH = 64;
    private final String s;
    private int i;
    private int depth;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.err("Unexpected trailing data");
        return v;
    }

    /** Pulls the first JSON object/array out of an LLM reply (ignores code fences and chatter). */
    public static Object extract(String text) {
        int a = -1;
        for (int k = 0; k < text.length(); k++) {
            char c = text.charAt(k);
            if (c == '{' || c == '[') { a = k; break; }
        }
        int b = Math.max(text.lastIndexOf('}'), text.lastIndexOf(']'));
        if (a < 0 || b < a) throw new IllegalArgumentException("No JSON found in reply");
        return parse(text.substring(a, b + 1));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : List.of();
    }

    public static String str(Object o) { return o instanceof String s ? s : null; }

    // ---------- writer ----------
    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        write(o, sb);
        return sb.toString();
    }

    private static void write(Object o, StringBuilder sb) {
        if (o == null) sb.append("null");
        else if (o instanceof String str) quote(str, sb);
        else if (o instanceof Number || o instanceof Boolean) sb.append(o);
        else if (o instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (o instanceof Collection<?> c) {
            sb.append('[');
            boolean first = true;
            for (Object x : c) {
                if (!first) sb.append(',');
                first = false;
                write(x, sb);
            }
            sb.append(']');
        } else quote(String.valueOf(o), sb);
    }

    private static void quote(String str, StringBuilder sb) {
        sb.append('"');
        for (int k = 0; k < str.length(); k++) {
            char c = str.charAt(k);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------- parser ----------
    private IllegalArgumentException err(String m) { return new IllegalArgumentException(m + " at position " + i); }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private Object value() {
        if (i >= s.length()) throw err("Unexpected end");
        char c = s.charAt(i);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> { lit("true"); yield Boolean.TRUE; }
            case 'f' -> { lit("false"); yield Boolean.FALSE; }
            case 'n' -> { lit("null"); yield null; }
            default -> number();
        };
    }

    private void lit(String w) {
        if (!s.startsWith(w, i)) throw err("Invalid literal");
        i += w.length();
    }

    private Map<String, Object> object() {
        if (++depth > MAX_DEPTH) throw err("Too deeply nested");
        Map<String, Object> m = new LinkedHashMap<>();
        i++; ws();
        if (i < s.length() && s.charAt(i) == '}') { i++; depth--; return m; }
        while (true) {
            ws();
            if (i >= s.length() || s.charAt(i) != '"') throw err("Expected string key");
            String k = string();
            ws();
            if (i >= s.length() || s.charAt(i) != ':') throw err("Expected ':'");
            i++; ws();
            m.put(k, value());
            ws();
            if (i >= s.length()) throw err("Unterminated object");
            char c = s.charAt(i++);
            if (c == '}') break;
            if (c != ',') throw err("Expected ',' or '}'");
        }
        depth--;
        return m;
    }

    private List<Object> array() {
        if (++depth > MAX_DEPTH) throw err("Too deeply nested");
        List<Object> l = new ArrayList<>();
        i++; ws();
        if (i < s.length() && s.charAt(i) == ']') { i++; depth--; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            if (i >= s.length()) throw err("Unterminated array");
            char c = s.charAt(i++);
            if (c == ']') break;
            if (c != ',') throw err("Expected ',' or ']'");
        }
        depth--;
        return l;
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        i++;
        while (true) {
            if (i >= s.length()) throw err("Unterminated string");
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\') {
                if (i >= s.length()) throw err("Bad escape");
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw err("Bad unicode escape");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> sb.append(e);   // \" \\ \/
                }
            } else sb.append(c);
        }
        return sb.toString();
    }

    private Number number() {
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (st == i) throw err("Unexpected character '" + s.charAt(i) + "'");
        String t = s.substring(st, i);
        try {
            return t.contains(".") || t.contains("e") || t.contains("E") ? (Number) Double.valueOf(t) : (Number) Long.valueOf(t);
        } catch (NumberFormatException e) {
            throw err("Bad number");
        }
    }
}
