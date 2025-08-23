/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.state;

import android.os.Handler;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Dispatches state events to the listener on a background executor or provided Handler.
 */
public final class StateManager {
    private final StateListener listener;
    private final Executor callbackExecutor;
    private final Handler handler;

    public StateManager(StateListener listener, Executor callbackExecutor, Handler mainThreadHandler) {
        this.listener = Objects.requireNonNull(listener, "listener");
        this.callbackExecutor = callbackExecutor != null ? callbackExecutor : Executors.newSingleThreadExecutor();
        this.handler = mainThreadHandler;
    }

    public void stateChanged(ConnectionState state, StateInfo info) {
        dispatch(() -> listener.onStateChanged(state, info));
    }

    public void traffic(TrafficEvent event) {
        dispatch(() -> listener.onTraffic(event));
    }

    public void error(Throwable t, StateListener.ErrorStage stage) {
        dispatch(() -> listener.onError(t, stage));
    }

    public void retryScheduled(int attempt, long backoffMs, Throwable cause) {
        dispatch(() -> listener.onRetryScheduled(attempt, backoffMs, cause));
    }

    public void retryExhausted(Throwable lastError) {
        dispatch(() -> listener.onRetryExhausted(lastError));
    }

    private void dispatch(Runnable r) {
        if (handler != null) {
            handler.post(r);
        } else {
            callbackExecutor.execute(r);
        }
    }
}


