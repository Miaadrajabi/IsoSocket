package com.miaad.iso8583TCPSocket;

/**
 * Simple ISO-8583 response wrapper
 */
public class IsoResponse {
    private final byte[] data;
    private final long responseTimeMs;
    
    public IsoResponse(byte[] data, long responseTimeMs) {
        this.data = data;
        this.responseTimeMs = responseTimeMs;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
}
