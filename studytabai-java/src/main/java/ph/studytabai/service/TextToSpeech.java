package ph.studytabai.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Read-aloud using the operating system's speech engine (no libraries needed).
 * Text is sent through stdin or as a single argument, never through a shell, so it cannot inject commands.
 */
public final class TextToSpeech {
    private Process current;

    public synchronized boolean speak(String text) {
        stop();
        if (text == null || text.isBlank()) return false;
        String clipped = text.length() > 2000 ? text.substring(0, 2000) : text;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                current = start(List.of("powershell", "-NoProfile", "-Command",
                        "Add-Type -AssemblyName System.Speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak([Console]::In.ReadToEnd())"), clipped);
            } else if (os.contains("mac")) {
                current = start(List.of("say", "-f", "-"), clipped);
            } else {
                try {
                    current = start(List.of("espeak-ng", "--stdin"), clipped);
                } catch (IOException e) {
                    try {
                        current = start(List.of("espeak", "--stdin"), clipped);
                    } catch (IOException e2) {
                        current = new ProcessBuilder("spd-say", "--", clipped).redirectErrorStream(true).start();
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Process start(List<String> cmd, String stdin) throws IOException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try (OutputStream out = p.getOutputStream()) {
            out.write(stdin.getBytes(StandardCharsets.UTF_8));
        }
        return p;
    }

    public synchronized void stop() {
        if (current != null && current.isAlive()) current.destroy();
        current = null;
    }
}
