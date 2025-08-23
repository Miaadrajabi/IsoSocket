/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential backoff with jitter.
 */
public final class Backoff {
    private long current;
    private final long max;
    private final float jitterFactor;

    public Backoff(long initial, long max, float jitterFactor) {
        if (initial <= 0 || max <= 0) throw new IllegalArgumentException("initial/max > 0");
        this.current = initial;
        this.max = max;
        this.jitterFactor = Math.max(0f, Math.min(1f, jitterFactor));
    }

    public long nextDelayMs() {
        float jitter = (float) (ThreadLocalRandom.current().nextDouble(-jitterFactor, jitterFactor));
        long delay = (long) (current * (1.0 + jitter));
        current = Math.min(max, Math.max(1, current * 2));
        return Math.max(0, delay);
    }

    public void reset(long initial) {
        this.current = initial;
    }
}


