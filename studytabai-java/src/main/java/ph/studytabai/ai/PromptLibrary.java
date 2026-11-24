package ph.studytabai.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Loads prompt templates from /prompts/*.txt on the classpath and fills {{placeholders}}. */
public final class PromptLibrary {
    private PromptLibrary() {}

    public static String render(String name, Map<String, String> vars) {
        String t = load(name);
        for (Map.Entry<String, String> e : vars.entrySet())
            t = t.replace("{{" + e.getKey() + "}}", e.getValue());
        return t;
    }

    private static String load(String name) {
        try (InputStream in = PromptLibrary.class.getResourceAsStream("/prompts/" + name + ".txt")) {
            if (in == null) throw new IllegalStateException("Missing prompt template: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read prompt template: " + name, e);
        }
    }
}
