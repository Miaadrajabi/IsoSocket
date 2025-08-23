/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.framing;

import java.nio.ByteBuffer;

/**
 * Delimiter-based framer (e.g., newline-terminated protocols).
 */
public final class DelimiterFramer implements Framer {
    private final byte delimiter;

    public DelimiterFramer(byte delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public ByteBuffer[] frameForSend(byte[] payload) {
        byte[] body = payload == null ? new byte[0] : payload;
        byte[] framed = new byte[body.length + 1];
        System.arraycopy(body, 0, framed, 0, body.length);
        framed[framed.length - 1] = delimiter;
        return new ByteBuffer[] { ByteBuffer.wrap(framed) };
    }

    @Override
    public Result tryDecode(ByteBuffer inbound) {
        int start = inbound.position();
        for (int i = start; i < inbound.limit(); i++) {
            if (inbound.get(i) == delimiter) {
                int len = i - start;
                byte[] frame = new byte[len];
                inbound.position(start);
                inbound.get(frame);
                inbound.get(); // consume delimiter
                int consumed = len + 1;
                return new Result(new byte[][] { frame }, consumed);
            }
        }
        inbound.position(start);
        return new Result(new byte[0][], 0);
    }

    @Override
    public int expectedBytesForNextFrame() { return -1; }
}


