package com.miaad.iso8583TCPSocket;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for retry mechanism in IsoClient
 */
public class RetryConfig {
    private final int maxRetries;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final double backoffMultiplier;
    private final Set<Class<? extends Exception>> retryableExceptions;
    private final boolean retryOnTimeout;
    private final boolean retryOnConnectionFailure;
    private final boolean retryOnIOException;

    private RetryConfig(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.baseDelayMs = builder.baseDelayMs;
        this.maxDelayMs = builder.maxDelayMs;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.retryableExceptions = new HashSet<>(builder.retryableExceptions);
        this.retryOnTimeout = builder.retryOnTimeout;
        this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
        this.retryOnIOException = builder.retryOnIOException;
    }

    public int getMaxRetries() { return maxRetries; }
    public long getBaseDelayMs() { return baseDelayMs; }
    public long getMaxDelayMs() { return maxDelayMs; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public boolean isRetryOnTimeout() { return retryOnTimeout; }
    public boolean isRetryOnConnectionFailure() { return retryOnConnectionFailure; }
    public boolean isRetryOnIOException() { return retryOnIOException; }

    /**
     * Check if an exception should trigger a retry
     */
    public boolean shouldRetry(Exception exception) {
        // Check specific exception types
        if (retryOnTimeout && exception instanceof SocketTimeoutException) {
            return true;
        }
        if (retryOnConnectionFailure && exception instanceof ConnectException) {
            return true;
        }
        if (retryOnIOException && exception instanceof IOException) {
            return true;
        }

        // Check custom retryable exceptions
        for (Class<? extends Exception> exceptionClass : retryableExceptions) {
            if (exceptionClass.isInstance(exception)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate delay for retry attempt using exponential backoff
     */
    public long calculateDelay(int attemptNumber) {
        if (attemptNumber <= 0) return 0;
        
        double delay = baseDelayMs * Math.pow(backoffMultiplier, attemptNumber - 1);
        return Math.min((long) delay, maxDelayMs);
    }

    public static class Builder {
        private int maxRetries = 3;
        private long baseDelayMs = 1000; // 1 second
        private long maxDelayMs = 30000; // 30 seconds
        private double backoffMultiplier = 2.0;
        private Set<Class<? extends Exception>> retryableExceptions = new HashSet<>();
        private boolean retryOnTimeout = true;
        private boolean retryOnConnectionFailure = true;
        private boolean retryOnIOException = false; // Only specific IO exceptions

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder baseDelay(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
            return this;
        }

        public Builder maxDelay(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder retryOnTimeout(boolean retryOnTimeout) {
            this.retryOnTimeout = retryOnTimeout;
            return this;
        }

        public Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
            this.retryOnConnectionFailure = retryOnConnectionFailure;
            return this;
        }

        public Builder retryOnIOException(boolean retryOnIOException) {
            this.retryOnIOException = retryOnIOException;
            return this;
        }

        public Builder addRetryableException(Class<? extends Exception> exceptionClass) {
            this.retryableExceptions.add(exceptionClass);
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }

    /**
     * Default retry config for typical ISO-8583 scenarios
     */
    public static RetryConfig defaultConfig() {
        return new Builder()
            .maxRetries(3)
            .baseDelay(1000)
            .maxDelay(10000)
            .backoffMultiplier(2.0)
            .retryOnTimeout(true)
            .retryOnConnectionFailure(true)
            .retryOnIOException(false)
            .build();
    }

    /**
     * Aggressive retry config for unreliable networks
     */
    public static RetryConfig aggressiveConfig() {
        return new Builder()
            .maxRetries(5)
            .baseDelay(500)
            .maxDelay(15000)
            .backoffMultiplier(1.5)
            .retryOnTimeout(true)
            .retryOnConnectionFailure(true)
            .retryOnIOException(true)
            .build();
    }

    /**
     * No retry config
     */
    public static RetryConfig noRetry() {
        return new Builder()
            .maxRetries(0)
            .build();
    }
}
