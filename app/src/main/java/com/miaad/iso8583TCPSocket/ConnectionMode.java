package com.miaad.iso8583TCPSocket;

/**
 * Connection mode for IsoClient
 */
public enum ConnectionMode {
    /**
     * Blocking mode - uses traditional blocking I/O
     * Simple and reliable for most use cases
     */
    BLOCKING("Blocking I/O"),
    
    /**
     * Non-blocking mode - uses NIO (New I/O) 
     * Better performance for high-concurrency scenarios
     */
    NON_BLOCKING("Non-blocking NIO");
    
    private final String description;
    
    ConnectionMode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
