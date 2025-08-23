/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight metrics for bytes in/out and last RTT.
 */
public final class Metrics {
    private final AtomicLong bytesIn = new AtomicLong();
    private final AtomicLong bytesOut = new AtomicLong();
    private final AtomicLong lastRttMs = new AtomicLong(-1);

    public void addBytesIn(long n) { if (n > 0) bytesIn.addAndGet(n); }
    public void addBytesOut(long n) { if (n > 0) bytesOut.addAndGet(n); }
    public void setLastRttMs(long rtt) { lastRttMs.set(rtt); }

    public long getBytesIn() { return bytesIn.get(); }
    public long getBytesOut() { return bytesOut.get(); }
    public long getLastRttMs() { return lastRttMs.get(); }
}


