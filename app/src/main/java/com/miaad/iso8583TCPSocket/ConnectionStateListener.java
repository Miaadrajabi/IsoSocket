package com.miaad.iso8583TCPSocket;

/**
 * Comprehensive listener for all connection state changes and events
 */
public interface ConnectionStateListener {
    
    /**
     * Called when connection state changes
     * @param oldState Previous state
     * @param newState New state
     * @param details Additional details about the state change
     */
    void onStateChanged(ConnectionState oldState, ConnectionState newState, String details);
    
    /**
     * Called when starting a connection attempt
     * @param host Target host
     * @param port Target port
     * @param attempt Attempt number (1-based)
     * @param maxAttempts Maximum number of attempts
     */
    void onConnectionAttemptStarted(String host, int port, int attempt, int maxAttempts);
    
    /**
     * Called when host resolution starts
     * @param hostname Hostname being resolved
     */
    void onHostResolutionStarted(String hostname);
    
    /**
     * Called when host resolution completes
     * @param hostname Hostname that was resolved
     * @param resolvedIp Resolved IP address
     * @param timeMs Time taken for resolution
     */
    void onHostResolutionCompleted(String hostname, String resolvedIp, long timeMs);
    
    /**
     * Called when TCP connection starts
     * @param host Target host
     * @param port Target port
     */
    void onTcpConnectionStarted(String host, int port);
    
    /**
     * Called when TCP connection completes
     * @param localAddress Local socket address
     * @param remoteAddress Remote socket address
     * @param timeMs Time taken for TCP connection
     */
    void onTcpConnectionCompleted(String localAddress, String remoteAddress, long timeMs);
    
    /**
     * Called when TLS handshake starts
     */
    void onTlsHandshakeStarted();
    
    /**
     * Called when TLS handshake completes
     * @param protocol TLS protocol version
     * @param cipherSuite Cipher suite used
     * @param timeMs Time taken for handshake
     */
    void onTlsHandshakeCompleted(String protocol, String cipherSuite, long timeMs);
    
    /**
     * Called when starting to send data
     * @param dataLength Length of data being sent
     * @param messageType Type of message (e.g., "ISO-8583")
     */
    void onSendStarted(int dataLength, String messageType);
    
    /**
     * Called when creating message frame
     * @param frameType Type of frame (e.g., "Length-Prefixed")
     * @param headerSize Size of frame header
     * @param dataSize Size of data portion
     */
    void onFrameCreated(String frameType, int headerSize, int dataSize);
    
    /**
     * Called when data transmission starts
     * @param totalBytes Total bytes to send
     */
    void onDataTransmissionStarted(int totalBytes);
    
    /**
     * Called during data transmission progress
     * @param bytesSent Bytes sent so far
     * @param totalBytes Total bytes to send
     * @param percentComplete Completion percentage
     */
    void onDataTransmissionProgress(int bytesSent, int totalBytes, int percentComplete);
    
    /**
     * Called when data transmission completes
     * @param totalBytes Total bytes sent
     * @param timeMs Time taken for transmission
     */
    void onDataTransmissionCompleted(int totalBytes, long timeMs);
    
    /**
     * Called when starting to wait for response
     * @param timeoutMs Timeout value in milliseconds
     */
    void onResponseWaitStarted(int timeoutMs);
    
    /**
     * Called when response header reading starts
     * @param expectedHeaderSize Expected header size
     */
    void onResponseHeaderReadStarted(int expectedHeaderSize);
    
    /**
     * Called when response header is received
     * @param headerBytes Header bytes received
     * @param parsedLength Parsed data length from header
     * @param timeMs Time taken to read header
     */
    void onResponseHeaderReceived(byte[] headerBytes, int parsedLength, long timeMs);
    
    /**
     * Called when response data reading starts
     * @param expectedDataSize Expected data size
     */
    void onResponseDataReadStarted(int expectedDataSize);
    
    /**
     * Called during response data reading progress
     * @param bytesRead Bytes read so far
     * @param totalBytes Total bytes to read
     * @param percentComplete Completion percentage
     */
    void onResponseDataReadProgress(int bytesRead, int totalBytes, int percentComplete);
    
    /**
     * Called when response data is fully received
     * @param dataBytes Response data bytes
     * @param totalBytes Total bytes received
     * @param timeMs Time taken to read data
     */
    void onResponseDataReceived(byte[] dataBytes, int totalBytes, long timeMs);
    
    /**
     * Called when starting to process response
     * @param responseSize Size of response data
     */
    void onResponseProcessingStarted(int responseSize);
    
    /**
     * Called when response processing completes
     * @param processingTimeMs Time taken for processing
     * @param totalTransactionTimeMs Total transaction time
     */
    void onResponseProcessingCompleted(long processingTimeMs, long totalTransactionTimeMs);
    
    /**
     * Called when disconnection starts
     * @param reason Reason for disconnection
     */
    void onDisconnectionStarted(String reason);
    
    /**
     * Called when socket is being closed
     */
    void onSocketClosing();
    
    /**
     * Called when socket is closed
     * @param timeMs Time taken to close
     */
    void onSocketClosed(long timeMs);
    
    /**
     * Called when an error occurs
     * @param error Error that occurred
     * @param currentState State when error occurred
     * @param details Additional error details
     */
    void onError(Exception error, ConnectionState currentState, String details);
    
    /**
     * Called when operation is cancelled
     * @param currentState State when cancelled
     * @param reason Cancellation reason
     */
    void onCancelled(ConnectionState currentState, String reason);
    
    /**
     * Called when a timeout occurs
     * @param timeoutType Type of timeout (connect, read, etc.)
     * @param timeoutMs Timeout value that was exceeded
     * @param currentState State when timeout occurred
     */
    void onTimeout(String timeoutType, int timeoutMs, ConnectionState currentState);
    
    /**
     * Called when retry delay starts
     * @param attempt Attempt number
     * @param delayMs Delay duration in milliseconds
     * @param reason Reason for retry
     */
    void onRetryDelayStarted(int attempt, long delayMs, String reason);
    
    /**
     * Called when retry delay ends and new attempt begins
     * @param attempt Attempt number about to start
     */
    void onRetryDelayEnded(int attempt);
    
    /**
     * Called when all retry attempts are exhausted
     * @param totalAttempts Total number of attempts made
     * @param lastError Last error that occurred
     */
    void onRetryExhausted(int totalAttempts, Exception lastError);
    
    /**
     * Called for general operational messages/logs
     * @param level Log level (DEBUG, INFO, WARN, ERROR)
     * @param message Log message
     * @param details Additional details
     */
    void onLog(String level, String message, String details);
    
    /**
     * Called for performance metrics
     * @param metricName Name of the metric
     * @param value Metric value
     * @param unit Unit of measurement
     */
    void onMetric(String metricName, long value, String unit);
}
