/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket;

import java.net.SocketAddress;
import java.util.Arrays;

/**
 * Represents a synchronous TCP response along with basic metadata.
 */
public final class TcpResponse {
    private final byte[] payload;
    private final long roundTripTimeMillis;
    private final SocketAddress remoteAddress;
    private final long requestStartTimeMillis;
    private final long responseReceiveTimeMillis;

    public TcpResponse(
            byte[] payload,
            long roundTripTimeMillis,
            SocketAddress remoteAddress,
            long requestStartTimeMillis,
            long responseReceiveTimeMillis) {
        this.payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        this.roundTripTimeMillis = roundTripTimeMillis;
        this.remoteAddress = remoteAddress;
        this.requestStartTimeMillis = requestStartTimeMillis;
        this.responseReceiveTimeMillis = responseReceiveTimeMillis;
    }

    public byte[] getPayload() {
        return Arrays.copyOf(payload, payload.length);
    }

    public long getRoundTripTimeMillis() {
        return roundTripTimeMillis;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public long getRequestStartTimeMillis() {
        return requestStartTimeMillis;
    }

    public long getResponseReceiveTimeMillis() {
        return responseReceiveTimeMillis;
    }
}


