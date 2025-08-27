package com.miaad.iso8583TCPSocket;

/**
 * Callback interface for retry events
 */
public interface RetryCallback {
    /**
     * Called when a retry attempt is about to start
     * @param operation Operation type (connect/send)
     * @param attempt Attempt number (1-based)
     * @param maxAttempts Maximum number of attempts
     * @param delayMs Delay before this attempt in milliseconds
     */
    void onRetryAttempt(String operation, int attempt, int maxAttempts, long delayMs);
    
    /**
     * Called when an attempt fails
     * @param operation Operation type (connect/send)
     * @param attempt Attempt number (1-based)
     * @param maxAttempts Maximum number of attempts
     * @param exception Exception that caused the failure
     * @param willRetry Whether another retry will be attempted
     */
    void onAttemptFailed(String operation, int attempt, int maxAttempts, Exception exception, boolean willRetry);
    
    /**
     * Called when operation succeeds after retries
     * @param operation Operation type (connect/send)
     * @param attempt Attempt number that succeeded (1-based)
     * @param totalTimeMs Total time taken including delays
     */
    void onSuccess(String operation, int attempt, long totalTimeMs);
    
    /**
     * Called when all attempts fail
     * @param operation Operation type (connect/send)
     * @param totalAttempts Total number of attempts made
     * @param lastException Last exception that occurred
     */
    void onAllAttemptsFailed(String operation, int totalAttempts, Exception lastException);
}
