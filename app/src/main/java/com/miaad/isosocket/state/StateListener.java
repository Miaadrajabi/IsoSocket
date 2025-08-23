/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.state;

public interface StateListener {
    enum ErrorStage { DNS, CONNECT, HANDSHAKE, IO, CLOSE }

    void onStateChanged(ConnectionState state, StateInfo info);

    void onTraffic(TrafficEvent event);

    void onError(Throwable t, ErrorStage stage);

    default void onRetryScheduled(int attempt, long backoffMs, Throwable cause) {}

    default void onRetryExhausted(Throwable lastError) {}
}


