/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.framing;


import java.nio.ByteBuffer;

/**
 * Fixed-size framer; emits exactly fixedSize bytes per frame.
 */
public final class RawFixedFramer implements Framer {
    private final int fixedSize;

    public RawFixedFramer(int fixedSize) {
        if (fixedSize <= 0) throw new IllegalArgumentException("fixedSize > 0");
        this.fixedSize = fixedSize;
    }

    @Override
    public ByteBuffer[] frameForSend(byte[] payload) {
        if (payload == null || payload.length != fixedSize) {
            throw new IllegalArgumentException("payload must be exactly fixedSize bytes");
        }
        return new ByteBuffer[] { ByteBuffer.wrap(payload) };
    }

    @Override
    public Result tryDecode(ByteBuffer inbound) {
        if (inbound.remaining() < fixedSize) return new Result(new byte[0][], 0);
        byte[] frame = new byte[fixedSize];
        inbound.get(frame);
        return new Result(new byte[][] { frame }, fixedSize);
    }

    @Override
    public int expectedBytesForNextFrame() { return fixedSize; }
}


