package ph.studytabai.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/** Sliding-window limiter: at most 'max' AI calls per 'windowMs'. */
public final class AiRateLimiter {
    private final int max;
    private final long windowMs;
    private final LongSupplier clock;
    private final Deque<Long> hits = new ArrayDeque<>();

    public AiRateLimiter(int max, long windowMs) { this(max, windowMs, System::currentTimeMillis); }

    public AiRateLimiter(int max, long windowMs, LongSupplier clock) {
        this.max = max;
        this.windowMs = windowMs;
        this.clock = clock;
    }

    public synchronized void check() throws AiException {
        long now = clock.getAsLong();
        while (!hits.isEmpty() && now - hits.peekFirst() >= windowMs) hits.pollFirst();
        if (hits.size() >= max) throw new AiException("You are sending AI requests too quickly. Please wait a moment.");
        hits.addLast(now);
    }
}
