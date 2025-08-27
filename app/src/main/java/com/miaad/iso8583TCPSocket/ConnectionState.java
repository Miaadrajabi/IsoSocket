package com.miaad.iso8583TCPSocket;

/**
 * Enum representing all possible connection states
 */
public enum ConnectionState {
    // Initial state
    DISCONNECTED("Disconnected"),
    
    // Connection phase
    CONNECTING("Connecting..."),
    RESOLVING_HOST("Resolving host..."),
    TCP_CONNECTING("TCP connecting..."),
    TCP_CONNECTED("TCP connected"),
    TLS_HANDSHAKING("TLS handshaking..."),
    TLS_CONNECTED("TLS connected"),
    CONNECTED("Connected"),
    
    // Transaction phase
    PREPARING_SEND("Preparing to send..."),
    CREATING_FRAME("Creating message frame..."),
    SENDING_DATA("Sending data..."),
    DATA_SENT("Data sent"),
    WAITING_RESPONSE("Waiting for response..."),
    READING_HEADER("Reading response header..."),
    HEADER_RECEIVED("Header received"),
    READING_DATA("Reading response data..."),
    DATA_RECEIVED("Data received"),
    PROCESSING_RESPONSE("Processing response..."),
    TRANSACTION_COMPLETE("Transaction complete"),
    
    // Disconnection phase
    DISCONNECTING("Disconnecting..."),
    CLOSING_SOCKET("Closing socket..."),
    SOCKET_CLOSED("Socket closed"),
    
    // Error states
    CONNECTION_FAILED("Connection failed"),
    TRANSACTION_FAILED("Transaction failed"),
    TIMEOUT("Timeout"),
    CANCELLED("Cancelled"),
    
    // Retry states
    RETRY_WAITING("Waiting to retry..."),
    RETRY_CONNECTING("Retrying connection...");
    
    private final String description;
    
    ConnectionState(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
    
    public boolean isConnected() {
        return this == CONNECTED || this == TCP_CONNECTED || this == TLS_CONNECTED;
    }
    
    public boolean isConnecting() {
        return this == CONNECTING || this == RESOLVING_HOST || this == TCP_CONNECTING || 
               this == TLS_HANDSHAKING || this == RETRY_CONNECTING;
    }
    
    public boolean isTransacting() {
        return this == PREPARING_SEND || this == CREATING_FRAME || this == SENDING_DATA ||
               this == DATA_SENT || this == WAITING_RESPONSE || this == READING_HEADER ||
               this == HEADER_RECEIVED || this == READING_DATA || this == DATA_RECEIVED ||
               this == PROCESSING_RESPONSE;
    }
    
    public boolean isDisconnecting() {
        return this == DISCONNECTING || this == CLOSING_SOCKET;
    }
    
    public boolean isError() {
        return this == CONNECTION_FAILED || this == TRANSACTION_FAILED || 
               this == TIMEOUT || this == CANCELLED;
    }
    
    public boolean isRetrying() {
        return this == RETRY_WAITING || this == RETRY_CONNECTING;
    }
}
