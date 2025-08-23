/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.framing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Length-prefixed framing (2 or 4 bytes) commonly used for ISO-8583.
 */
public final class LengthPrefixedFramer implements Framer {
    private final int lengthBytes;
    private final ByteOrder byteOrder;
    private final boolean includeLengthInFrame;

    public LengthPrefixedFramer(int lengthBytes, ByteOrder order, boolean includeLength) {
        if (lengthBytes != 2 && lengthBytes != 4) throw new IllegalArgumentException("lengthBytes must be 2 or 4");
        this.lengthBytes = lengthBytes;
        this.byteOrder = order == null ? ByteOrder.BIG_ENDIAN : order;
        this.includeLengthInFrame = includeLength;
    }

    @Override
    public ByteBuffer[] frameForSend(byte[] payload) {
        int len = payload == null ? 0 : payload.length;
        int header = len;
        if (includeLengthInFrame) header = len + lengthBytes;
        ByteBuffer headerBuf = ByteBuffer.allocate(lengthBytes).order(byteOrder);
        if (lengthBytes == 2) headerBuf.putShort((short) (header & 0xFFFF)); else headerBuf.putInt(header);
        headerBuf.flip();
        ByteBuffer body = ByteBuffer.wrap(payload == null ? new byte[0] : payload);
        return new ByteBuffer[] { headerBuf, body };
    }

    @Override
    public Result tryDecode(ByteBuffer inbound) {
        inbound.mark();
        if (inbound.remaining() < lengthBytes) {
            inbound.reset();
            return new Result(new byte[0][], 0);
        }
        int startPos = inbound.position();
        ByteBuffer lenBuf = inbound.slice();
        lenBuf.limit(lengthBytes);
        lenBuf.order(byteOrder);
        int frameLen = (lengthBytes == 2) ? (lenBuf.getShort(0) & 0xFFFF) : lenBuf.getInt(0);
        int totalLen = frameLen;
        if (!includeLengthInFrame) totalLen = frameLen + lengthBytes;
        if (inbound.remaining() < totalLen) {
            inbound.reset();
            return new Result(new byte[0][], 0);
        }
        int bodyLen = includeLengthInFrame ? (frameLen - lengthBytes) : frameLen;
        inbound.position(startPos + lengthBytes);
        byte[] body = new byte[bodyLen];
        inbound.get(body);
        int consumed = includeLengthInFrame ? frameLen : (lengthBytes + bodyLen);
        return new Result(new byte[][] { body }, consumed);
    }

    @Override
    public int expectedBytesForNextFrame() {
        return -1;
    }
}


