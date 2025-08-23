/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.framing;

import java.nio.ByteBuffer;

/**
 * Protocol-agnostic framing interface used by both engines.
 */
public interface Framer {
    ByteBuffer[] frameForSend(byte[] payload);

    Result tryDecode(ByteBuffer inbound);

    int expectedBytesForNextFrame();

    final class Result {
        public final byte[][] frames;
        public final int bytesConsumed;

        public Result(byte[][] frames, int bytesConsumed) {
            this.frames = frames;
            this.bytesConsumed = bytesConsumed;
        }
    }
}


