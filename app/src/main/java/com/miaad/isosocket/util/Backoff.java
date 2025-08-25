package com.miaad.isosocket.util;

import java.util.concurrent.ThreadLocalRandom;

public final class Backoff {
    private long current;
    private final long max;
    private final float jitterFactor;

    public Backoff(long var1, long var3, float var5) {
        if (var1 > 0L && var3 > 0L) {
            this.current = var1;
            this.max = var3;
            this.jitterFactor = Math.max(0.0F, Math.min(1.0F, var5));
        } else {
            throw new IllegalArgumentException("initial/max > 0");
        }
    }

    public long nextDelayMs() {
        float var1 = (float) ThreadLocalRandom.current().nextDouble((double)(-this.jitterFactor), (double)this.jitterFactor);
        long var2 = (long)((double)this.current * (1.0 + (double)var1));
        this.current = Math.min(this.max, Math.max(1L, this.current * 2L));
        return Math.max(0L, var2);
    }

    public void reset(long var1) {
        this.current = var1;
    }
}
