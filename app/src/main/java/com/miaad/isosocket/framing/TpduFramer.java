/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.framing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * TPDU + length-prefixed framer commonly used with ISO-8583 over TCP.
 * Prepends a TPDU and length header (2 or 4 bytes, big/little endian).
 */
public final class TpduFramer implements Framer {
    private final byte[] tpdu;
    private final int lengthBytes;
    private final ByteOrder order;

    public TpduFramer(byte[] tpdu, int lengthBytes, ByteOrder order) {
        if (tpdu == null || tpdu.length == 0) throw new IllegalArgumentException("tpdu required");
        if (lengthBytes != 2 && lengthBytes != 4) throw new IllegalArgumentException("lengthBytes must be 2 or 4");
        this.tpdu = Arrays.copyOf(tpdu, tpdu.length);
        this.lengthBytes = lengthBytes;
        this.order = order == null ? ByteOrder.BIG_ENDIAN : order;
    }

    @Override
    public ByteBuffer[] frameForSend(byte[] payload) {
        int len = payload == null ? 0 : payload.length;
        int headerLen = tpdu.length + lengthBytes;
        ByteBuffer headerBuf = ByteBuffer.allocate(headerLen).order(order);
        if (lengthBytes == 2) headerBuf.putShort((short) (len & 0xFFFF)); else headerBuf.putInt(len);
        headerBuf.put(tpdu);
        headerBuf.flip();
        return new ByteBuffer[] { headerBuf, ByteBuffer.wrap(payload == null ? new byte[0] : payload) };
    }

    @Override
    public Result tryDecode(ByteBuffer inbound) {
        inbound.mark();
        if (inbound.remaining() < lengthBytes + tpdu.length) {
            inbound.reset();
            return new Result(new byte[0][], 0);
        }
        int start = inbound.position();
        ByteBuffer lenBuf = inbound.slice();
        lenBuf.limit(lengthBytes);
        lenBuf.order(order);
        int bodyLen = (lengthBytes == 2) ? (lenBuf.getShort(0) & 0xFFFF) : lenBuf.getInt(0);
        int total = lengthBytes + tpdu.length + bodyLen;
        if (inbound.remaining() < total) {
            inbound.reset();
            return new Result(new byte[0][], 0);
        }
        inbound.position(start + lengthBytes);
        byte[] actualTpdu = new byte[tpdu.length];
        inbound.get(actualTpdu);
        if (!Arrays.equals(actualTpdu, tpdu)) {
            inbound.reset();
            return new Result(new byte[0][], 0);
        }
        byte[] frame = new byte[bodyLen];
        inbound.get(frame);
        return new Result(new byte[][] { frame }, total);
    }

    @Override
    public int expectedBytesForNextFrame() { return -1; }
}


