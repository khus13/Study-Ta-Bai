package ph.studytabai.service;

import java.util.List;
import java.util.Random;

/** Fun feedback captions (text and emoji only, so there are no image rights or network needs). */
public final class MemeBank {
    private static final List<String> WIN = List.of(
            "Big brain energy! 🧠", "Absolutely cooking! 🔥", "Nailed it! 🎯", "Genius mode: ON 😎", "Too easy for you! 🚀", "Quiz champion vibes! 🏆");
    private static final List<String> LOSE = List.of(
            "Oof! Shake it off 💪", "Almost! Learn it, then crush it 📚", "Plot twist: that one was sneaky 🕵️",
            "Mistakes help brains grow 🌱", "Next one is yours! ✨", "Brain buffering... 🔄");
    private static final Random RANDOM = new Random();

    private MemeBank() {}

    public static String caption(boolean correct) {
        List<String> l = correct ? WIN : LOSE;
        return l.get(RANDOM.nextInt(l.size()));
    }
}
