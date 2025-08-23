/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.state;

import java.net.SocketAddress;

public final class StateInfo {
    public final int retryCount;
    public final long nextBackoffMs;
    public final SocketAddress remoteAddress;

    public StateInfo(int retryCount, long nextBackoffMs, SocketAddress remoteAddress) {
        this.retryCount = retryCount;
        this.nextBackoffMs = nextBackoffMs;
        this.remoteAddress = remoteAddress;
    }
}


