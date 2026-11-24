package ph.studytabai.service;

import javax.sound.sampled.*;

/** Optional background music: a short synthesized pentatonic loop (no audio files needed). */
public final class MusicPlayer {
    private Clip clip;

    public synchronized void start() {
        if (clip != null && clip.isRunning()) return;
        try {
            float rate = 22050f;
            double[] notes = {261.63, 293.66, 329.63, 392.00, 440.00, 392.00, 329.63, 293.66};
            int perNote = (int) (rate * 0.28);
            byte[] data = new byte[perNote * notes.length * 2];
            for (int n = 0; n < notes.length; n++) {
                for (int i = 0; i < perNote; i++) {
                    double env = Math.min(1.0, Math.min(i / 400.0, (perNote - i) / 800.0));
                    short v = (short) (Math.sin(2 * Math.PI * notes[n] * i / rate) * 3500 * env);
                    int idx = (n * perNote + i) * 2;
                    data[idx] = (byte) (v & 0xff);
                    data[idx + 1] = (byte) ((v >> 8) & 0xff);
                }
            }
            AudioFormat fmt = new AudioFormat(rate, 16, 1, true, false);
            clip = AudioSystem.getClip();
            clip.open(fmt, data, 0, data.length);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            clip = null;   // no audio device: silently skip
        }
    }

    public synchronized void stop() {
        if (clip != null) { clip.stop(); clip.close(); clip = null; }
    }
}
