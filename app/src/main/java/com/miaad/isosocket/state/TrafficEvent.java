/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.state;

public final class TrafficEvent {
    public enum Kind { SENT_BYTES, RECEIVED_BYTES, FRAME_SENT, FRAME_RECEIVED }

    public final Kind kind;
    public final int byteCount;

    public TrafficEvent(Kind kind, int byteCount) {
        this.kind = kind;
        this.byteCount = byteCount;
    }
}


