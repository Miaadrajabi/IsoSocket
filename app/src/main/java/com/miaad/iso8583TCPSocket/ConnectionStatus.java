package com.miaad.iso8583TCPSocket;

/**
 * Comprehensive connection status information
 */
public class ConnectionStatus {
    private final boolean isConnected;
    private final boolean isClosed;
    private final boolean isOpen;
    private final boolean isConnecting;
    private final boolean isDisconnecting;
    private final boolean isTransactionInProgress;
    private final boolean isOperationInProgress;
    private final boolean isCancelled;
    private final boolean isRetrying;
    private final boolean hasError;
    private final boolean isTimeout;
    private final boolean isTlsEnabled;
    private final boolean isTlsConnected;
    private final boolean isReadable;
    private final boolean isWritable;
    private final boolean isSocketBound;
    private final boolean isSocketClosed;
    private final ConnectionState currentState;
    private final ConnectionMode connectionMode;
    private final String engineType;
    private final String localAddress;
    private final String remoteAddress;
    private final Exception lastError;
    private final long connectionStartTime;
    private final long lastActivityTime;
    private final long connectionDuration;
    private final int reconnectAttempts;
    private final String statusDescription;

    public ConnectionStatus(Builder builder) {
        this.isConnected = builder.isConnected;
        this.isClosed = builder.isClosed;
        this.isOpen = builder.isOpen;
        this.isConnecting = builder.isConnecting;
        this.isDisconnecting = builder.isDisconnecting;
        this.isTransactionInProgress = builder.isTransactionInProgress;
        this.isOperationInProgress = builder.isOperationInProgress;
        this.isCancelled = builder.isCancelled;
        this.isRetrying = builder.isRetrying;
        this.hasError = builder.hasError;
        this.isTimeout = builder.isTimeout;
        this.isTlsEnabled = builder.isTlsEnabled;
        this.isTlsConnected = builder.isTlsConnected;
        this.isReadable = builder.isReadable;
        this.isWritable = builder.isWritable;
        this.isSocketBound = builder.isSocketBound;
        this.isSocketClosed = builder.isSocketClosed;
        this.currentState = builder.currentState;
        this.connectionMode = builder.connectionMode;
        this.engineType = builder.engineType;
        this.localAddress = builder.localAddress;
        this.remoteAddress = builder.remoteAddress;
        this.lastError = builder.lastError;
        this.connectionStartTime = builder.connectionStartTime;
        this.lastActivityTime = builder.lastActivityTime;
        this.connectionDuration = builder.connectionDuration;
        this.reconnectAttempts = builder.reconnectAttempts;
        this.statusDescription = builder.statusDescription;
    }

    // Connection state checks
    public boolean isConnected() { return isConnected; }
    public boolean isClosed() { return isClosed; }
    public boolean isOpen() { return isOpen; }
    public boolean isConnecting() { return isConnecting; }
    public boolean isDisconnecting() { return isDisconnecting; }
    
    // Operation state checks
    public boolean isTransactionInProgress() { return isTransactionInProgress; }
    public boolean isOperationInProgress() { return isOperationInProgress; }
    public boolean isCancelled() { return isCancelled; }
    public boolean isRetrying() { return isRetrying; }
    
    // Error state checks
    public boolean hasError() { return hasError; }
    public boolean isTimeout() { return isTimeout; }
    
    // Security checks
    public boolean isTlsEnabled() { return isTlsEnabled; }
    public boolean isTlsConnected() { return isTlsConnected; }
    
    // Socket level checks
    public boolean isReadable() { return isReadable; }
    public boolean isWritable() { return isWritable; }
    public boolean isSocketBound() { return isSocketBound; }
    public boolean isSocketClosed() { return isSocketClosed; }
    
    // State information
    public ConnectionState getCurrentState() { return currentState; }
    public ConnectionMode getConnectionMode() { return connectionMode; }
    public String getEngineType() { return engineType; }
    
    // Address information
    public String getLocalAddress() { return localAddress; }
    public String getRemoteAddress() { return remoteAddress; }
    
    // Error information
    public Exception getLastError() { return lastError; }
    
    // Timing information
    public long getConnectionStartTime() { return connectionStartTime; }
    public long getLastActivityTime() { return lastActivityTime; }
    public long getConnectionDuration() { return connectionDuration; }
    public int getReconnectAttempts() { return reconnectAttempts; }
    
    // Overall status
    public String getStatusDescription() { return statusDescription; }
    
    // Utility methods
    public boolean canConnect() {
        return !isConnected && !isConnecting && !isOperationInProgress;
    }
    
    public boolean canSend() {
        return isConnected && !isTransactionInProgress && !isClosed;
    }
    
    public boolean canDisconnect() {
        return isConnected || isConnecting;
    }
    
    public boolean isHealthy() {
        return isConnected && !hasError && !isTimeout && !isClosed;
    }
    
    public boolean needsReconnection() {
        return !isConnected && (hasError || isTimeout || isClosed);
    }
    
    @Override
    public String toString() {
        return String.format("ConnectionStatus{state=%s, connected=%s, mode=%s, engine=%s}", 
            currentState, isConnected, connectionMode, engineType);
    }
    
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Connection Status ===\n");
        sb.append("State: ").append(currentState).append("\n");
        sb.append("Connected: ").append(isConnected).append("\n");
        sb.append("Open: ").append(isOpen).append("\n");
        sb.append("Closed: ").append(isClosed).append("\n");
        sb.append("Mode: ").append(connectionMode).append("\n");
        sb.append("Engine: ").append(engineType).append("\n");
        sb.append("Local: ").append(localAddress != null ? localAddress : "N/A").append("\n");
        sb.append("Remote: ").append(remoteAddress != null ? remoteAddress : "N/A").append("\n");
        sb.append("TLS: ").append(isTlsEnabled ? (isTlsConnected ? "Connected" : "Enabled") : "Disabled").append("\n");
        sb.append("Transaction: ").append(isTransactionInProgress).append("\n");
        sb.append("Operation: ").append(isOperationInProgress).append("\n");
        sb.append("Error: ").append(hasError).append("\n");
        if (lastError != null) {
            sb.append("Last Error: ").append(lastError.getMessage()).append("\n");
        }
        sb.append("Duration: ").append(connectionDuration).append("ms\n");
        sb.append("Attempts: ").append(reconnectAttempts).append("\n");
        sb.append("Description: ").append(statusDescription).append("\n");
        return sb.toString();
    }

    public static class Builder {
        private boolean isConnected = false;
        private boolean isClosed = false;
        private boolean isOpen = false;
        private boolean isConnecting = false;
        private boolean isDisconnecting = false;
        private boolean isTransactionInProgress = false;
        private boolean isOperationInProgress = false;
        private boolean isCancelled = false;
        private boolean isRetrying = false;
        private boolean hasError = false;
        private boolean isTimeout = false;
        private boolean isTlsEnabled = false;
        private boolean isTlsConnected = false;
        private boolean isReadable = false;
        private boolean isWritable = false;
        private boolean isSocketBound = false;
        private boolean isSocketClosed = false;
        private ConnectionState currentState = ConnectionState.DISCONNECTED;
        private ConnectionMode connectionMode = ConnectionMode.BLOCKING;
        private String engineType = "";
        private String localAddress = null;
        private String remoteAddress = null;
        private Exception lastError = null;
        private long connectionStartTime = 0;
        private long lastActivityTime = 0;
        private long connectionDuration = 0;
        private int reconnectAttempts = 0;
        private String statusDescription = "";

        public Builder connected(boolean connected) { this.isConnected = connected; return this; }
        public Builder closed(boolean closed) { this.isClosed = closed; return this; }
        public Builder open(boolean open) { this.isOpen = open; return this; }
        public Builder connecting(boolean connecting) { this.isConnecting = connecting; return this; }
        public Builder disconnecting(boolean disconnecting) { this.isDisconnecting = disconnecting; return this; }
        public Builder transactionInProgress(boolean inProgress) { this.isTransactionInProgress = inProgress; return this; }
        public Builder operationInProgress(boolean inProgress) { this.isOperationInProgress = inProgress; return this; }
        public Builder cancelled(boolean cancelled) { this.isCancelled = cancelled; return this; }
        public Builder retrying(boolean retrying) { this.isRetrying = retrying; return this; }
        public Builder hasError(boolean hasError) { this.hasError = hasError; return this; }
        public Builder timeout(boolean timeout) { this.isTimeout = timeout; return this; }
        public Builder tlsEnabled(boolean enabled) { this.isTlsEnabled = enabled; return this; }
        public Builder tlsConnected(boolean connected) { this.isTlsConnected = connected; return this; }
        public Builder readable(boolean readable) { this.isReadable = readable; return this; }
        public Builder writable(boolean writable) { this.isWritable = writable; return this; }
        public Builder socketBound(boolean bound) { this.isSocketBound = bound; return this; }
        public Builder socketClosed(boolean closed) { this.isSocketClosed = closed; return this; }
        public Builder currentState(ConnectionState state) { this.currentState = state; return this; }
        public Builder connectionMode(ConnectionMode mode) { this.connectionMode = mode; return this; }
        public Builder engineType(String type) { this.engineType = type; return this; }
        public Builder localAddress(String address) { this.localAddress = address; return this; }
        public Builder remoteAddress(String address) { this.remoteAddress = address; return this; }
        public Builder lastError(Exception error) { this.lastError = error; return this; }
        public Builder connectionStartTime(long time) { this.connectionStartTime = time; return this; }
        public Builder lastActivityTime(long time) { this.lastActivityTime = time; return this; }
        public Builder connectionDuration(long duration) { this.connectionDuration = duration; return this; }
        public Builder reconnectAttempts(int attempts) { this.reconnectAttempts = attempts; return this; }
        public Builder statusDescription(String description) { this.statusDescription = description; return this; }

        public ConnectionStatus build() {
            return new ConnectionStatus(this);
        }
    }
}
