package com.miaad.iso8583TCPSocket;

/**
 * Simple configuration for ISO-8583 client
 */
public class IsoConfig {
    private final String host;
    private final int port;
        private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final boolean useTls;
    private final RetryConfig retryConfig;
    private final ConnectionMode connectionMode;

    private IsoConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;
        this.useTls = builder.useTls;
        this.retryConfig = builder.retryConfig;
        this.connectionMode = builder.connectionMode;
    }
    
    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public boolean isUseTls() { return useTls; }
    public RetryConfig getRetryConfig() { return retryConfig; }
    public ConnectionMode getConnectionMode() { return connectionMode; }
    
    public static class Builder {
        private String host;
        private int port;
        private int connectTimeoutMs = 30000; // Default 30 seconds
        private int readTimeoutMs = 30000;    // Default 30 seconds
        private boolean useTls = false;
        private RetryConfig retryConfig = RetryConfig.defaultConfig();
        private ConnectionMode connectionMode = ConnectionMode.BLOCKING; // Default to blocking
        
        public Builder(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        public Builder connectTimeout(int ms) {
            this.connectTimeoutMs = ms;
            return this;
        }
        
        public Builder readTimeout(int ms) {
            this.readTimeoutMs = ms;
            return this;
        }
        
                public Builder useTls(boolean useTls) {
            this.useTls = useTls;
            return this;
        }

        public Builder retryConfig(RetryConfig retryConfig) {
            this.retryConfig = retryConfig;
            return this;
        }

        public Builder connectionMode(ConnectionMode connectionMode) {
            this.connectionMode = connectionMode;
            return this;
        }

        public IsoConfig build() {
            return new IsoConfig(this);
        }
    }
}
