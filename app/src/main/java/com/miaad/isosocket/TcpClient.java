/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket;

import android.os.Handler;
import androidx.annotation.Nullable;

import com.miaad.isosocket.framing.Framer;
import com.miaad.isosocket.state.ConnectionState;
import com.miaad.isosocket.state.StateInfo;
import com.miaad.isosocket.state.StateListener;
import com.miaad.isosocket.state.StateManager;
import com.miaad.isosocket.state.TrafficEvent;
import com.miaad.isosocket.tls.TlsOptions;
import com.miaad.isosocket.util.Backoff;
import com.miaad.isosocket.util.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Facade TCP client with Builder configuration, blocking and NIO engines.
 */
public final class TcpClient implements AutoCloseable {
    private final TcpConfig config;
    private final StateManager stateManager;
    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduler;

    private final ReentrantLock connectLock = new ReentrantLock();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile TcpEngine engine;
    private volatile Future<?> inFlightConnect;
    private volatile int retryAttempt;
    private volatile Long lastConnectEpochMs;
    private volatile Long lastDisconnectEpochMs;
    private volatile ConnectionState lastState = ConnectionState.IDLE;

    private final Semaphore inflightSemaphore;
    private final Semaphore queueSemaphore;
    private final Object pacingLock = new Object();
    private volatile long lastSendAtNanos = 0L;

    private TcpClient(TcpConfig config, StateListener listener, Handler mainHandler) {
        this.config = config;
        StateListener wrapped = new MetricsAndStateListener(config, listener);
        this.stateManager = new StateManager(wrapped, config.callbackExecutor, mainHandler);
        this.ioExecutor = Executors.newSingleThreadExecutor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.engine = createEngine();
        this.inflightSemaphore = new Semaphore(Math.max(1, config.maxInFlightRequests));
        this.queueSemaphore = new Semaphore(Math.max(0, config.requestQueueCapacity));
    }

    private TcpEngine createEngine() {
        switch (config.mode) {
            case BLOCKING:
                return new BlockingEngine(config, stateManager);
            case NON_BLOCKING:
            default:
                return new NioEngine(config, stateManager);
        }
    }

    public void connect() throws IOException, TimeoutException {
        ensureOpen();
        singleFlightConnect(false);
    }

    private void singleFlightConnect(boolean isReconnect) throws IOException, TimeoutException {
        connectLock.lock();
        try {
            if (engine.isReady()) return;
            if (inFlightConnect != null && !inFlightConnect.isDone()) {
                try { inFlightConnect.get(); } catch (Exception e) { throw unwrap(e); }
                return;
            }
            inFlightConnect = ioExecutor.submit(() -> attemptConnectLoop(isReconnect));
            try { inFlightConnect.get(); } catch (Exception e) { throw unwrap(e); }
        } finally {
            connectLock.unlock();
        }
    }

    private void attemptConnectLoop(boolean isReconnect) {
        Backoff backoff = new Backoff(config.connectInitialBackoffMs, config.connectMaxBackoffMs, config.connectJitterFactor);
        int maxAttempts = Math.max(1, config.connectMaxRetries);
        retryAttempt = 0;
        stateManager.stateChanged(ConnectionState.CONNECTING, new StateInfo(0, 0, null));
        while (!closed.get() && retryAttempt < maxAttempts) {
            try {
                engine.connect();
                lastConnectEpochMs = System.currentTimeMillis();
                stateManager.stateChanged(ConnectionState.READY, new StateInfo(retryAttempt, 0, null));
                return;
            } catch (IOException | TimeoutException e) {
                ConnectError ce = mapConnectError(e);
                if (!config.connectRetryEnabled || !config.connectRetryOn.contains(ce)) {
                    stateManager.error(e, StateListener.ErrorStage.CONNECT);
                    stateManager.stateChanged(ConnectionState.ERROR, new StateInfo(retryAttempt, 0, null));
                    throw propagate(e);
                }
                retryAttempt++;
                long delay = backoff.nextDelayMs();
                stateManager.stateChanged(ConnectionState.BACKING_OFF, new StateInfo(retryAttempt, delay, null));
                stateManager.retryScheduled(retryAttempt, delay, e);
                sleep(delay);
            }
        }
        stateManager.retryExhausted(new IOException("connect retries exhausted"));
        throw propagate(new IOException("connect retries exhausted"));
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private static RuntimeException propagate(Exception e) { return (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e); }

    private static IOException unwrap(Exception e) throws IOException, TimeoutException {
        Throwable c = e.getCause();
        if (c instanceof IOException) throw (IOException) c;
        if (c instanceof TimeoutException) throw (TimeoutException) c;
        if (c instanceof RuntimeException) throw (RuntimeException) c;
        throw new RuntimeException(c);
    }

    private ConnectError mapConnectError(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (e instanceof TimeoutException) return ConnectError.TIMEOUT;
        if (m.contains("refused")) return ConnectError.CONNECTION_REFUSED;
        if (m.contains("unreachable")) return ConnectError.NETWORK_UNREACHABLE;
        if (m.contains("handshake")) return ConnectError.HANDSHAKE_TIMEOUT;
        if (e instanceof javax.net.ssl.SSLPeerUnverifiedException) return ConnectError.TLS_UNVERIFIED;
        if (m.contains("pinning")) return ConnectError.TLS_PINNING_MISMATCH;
        return ConnectError.UNKNOWN;
    }

    public TcpResponse sendAndReceive(byte[] request, long perRequestTimeoutMs) throws IOException, TimeoutException {
        ensureOpen();
        if (!engine.isReady()) throw new IOException("Not READY");
        long start = System.nanoTime();
        long initialRemainingMs = perRequestTimeoutMs;
        try {
            // Bound the queue; fail fast if full
            if (!queueSemaphore.tryAcquire(Math.max(1, 1), Math.max(1, initialRemainingMs), TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("request queue timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        }
        final long remainingMs = perRequestTimeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (remainingMs <= 0) { queueSemaphore.release(); throw new TimeoutException("request timeout before send"); }

        try {
            Future<TcpResponse> f = ioExecutor.submit(() -> {
                // ensure only maxInFlightRequests concurrently send
                inflightSemaphore.acquireUninterruptibly();
                try {
                    // pacing
                    if (config.minInterRequestDelayMs != null) {
                        synchronized (pacingLock) {
                            long now = System.nanoTime();
                            long minGapNanos = TimeUnit.MILLISECONDS.toNanos(config.minInterRequestDelayMs);
                            long wait = minGapNanos - (now - lastSendAtNanos);
                            if (wait > 0) {
                                try { TimeUnit.NANOSECONDS.sleep(wait); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                            }
                            lastSendAtNanos = System.nanoTime();
                        }
                    }
                    try {
                        return engine.sendAndReceive(request, remainingMs);
                    } catch (IOException | TimeoutException e) {
                        onIoFailure(e);
                        throw e;
                    }
                } finally {
                    inflightSemaphore.release();
                }
            });
            return f.get(Math.max(1, remainingMs), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("request timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof IOException) throw (IOException) c;
            if (c instanceof TimeoutException) throw (TimeoutException) c;
            throw new IOException(c);
        } finally {
            queueSemaphore.release();
        }
    }

    /** Convenience: send, receive, then close the connection regardless of outcome. */
    public TcpResponse sendAndReceiveThenClose(byte[] request, long perRequestTimeoutMs) throws IOException, TimeoutException {
        try {
            return sendAndReceive(request, perRequestTimeoutMs);
        } finally {
            close();
        }
    }

    /** Alias for close() for readability in app code. */
    public void disconnect() { close(); }

    public boolean isConnecting() { return !isReady() && !isClosed() && !isDisconnected(); }
    public boolean isHandshaking() { return false; }
    public boolean isConnected() { return engine != null && engine.isConnected(); }
    public boolean isReady() { return engine != null && engine.isReady(); }
    public boolean isOpen() { return !closed.get(); }
    public boolean isClosed() { return closed.get(); }
    public boolean isDisconnected() { return !isReady() && !isClosed(); }
    public ConnectionState getState() { return isReady() ? ConnectionState.READY : (isClosed() ? ConnectionState.CLOSED : ConnectionState.DISCONNECTED); }
    @Nullable public Long getLastConnectTimeMs() { return lastConnectEpochMs; }
    @Nullable public Long getLastDisconnectTimeMs() { return lastDisconnectEpochMs; }

    private void ensureOpen() {
        if (closed.get()) throw new IllegalStateException("Client closed");
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { if (engine != null) engine.close(); } catch (IOException ignored) {}
        ioExecutor.shutdownNow();
        scheduler.shutdownNow();
    }

    private void onIoFailure(Exception e) {
        lastDisconnectEpochMs = System.currentTimeMillis();
        if (config.autoReconnect && !closed.get()) {
            ioExecutor.submit(() -> {
                try { singleFlightConnect(true); } catch (Exception ignored) {}
            });
        }
    }

    public static final class Builder {
        private final TcpConfig.Builder cfg = new TcpConfig.Builder();
        private StateListener listener = new StateListener() {
            @Override public void onStateChanged(ConnectionState state, StateInfo info) {}
            @Override public void onTraffic(TrafficEvent event) {}
            @Override public void onError(Throwable t, ErrorStage stage) {}
        };
        private Handler mainHandler;

        public Builder host(String h) { cfg.host(h); return this; }
        public Builder port(int p) { cfg.port(p); return this; }
        public Builder mode(ConnectionMode m) { cfg.mode(m); return this; }
        public Builder enableTls(boolean v) { cfg.tlsOptions(new TlsOptions.Builder().enableTls(v).build()); return this; }
        public Builder verifyHostname(boolean v) { cfg.tlsOptions(new TlsOptions.Builder().enableTls(true).verifyHostname(v).build()); return this; }
        public Builder trustManager(javax.net.ssl.X509TrustManager tm) { cfg.tlsOptions(new TlsOptions.Builder().enableTls(true).trustManager(tm).build()); return this; }
        public Builder pinnedSpkiSha256(java.util.List<String> pins) { cfg.tlsOptions(new TlsOptions.Builder().enableTls(true).pinnedSpkiSha256(pins).build()); return this; }
        public Builder tlsProtocolVersions(java.util.List<String> versions) { cfg.tlsOptions(new TlsOptions.Builder().enableTls(true).tlsProtocolVersions(versions).build()); return this; }
        public Builder tlsOptions(TlsOptions opts) { cfg.tlsOptions(opts); return this; }
        public Builder tcpNoDelay(boolean v) { cfg.tcpNoDelay(v); return this; }
        public Builder connectTimeoutMs(int v) { cfg.connectTimeoutMs(v); return this; }
        public Builder readTimeoutMs(int v) { cfg.readTimeoutMs(v); return this; }
        public Builder writeTimeoutMs(int v) { cfg.writeTimeoutMs(v); return this; }
        public Builder handshakeTimeoutMs(int v) { cfg.handshakeTimeoutMs(v); return this; }
        public Builder requestTimeoutMs(int v) { cfg.requestTimeoutMs(v); return this; }
        public Builder framer(Framer f) { cfg.framer(f); return this; }
        public Builder autoReconnect(boolean v) { cfg.autoReconnect(v); return this; }
        public Builder initialBackoffMs(long v) { cfg.initialBackoffMs(v); return this; }
        public Builder maxBackoffMs(long v) { cfg.maxBackoffMs(v); return this; }
        public Builder jitterFactor(float v) { cfg.jitterFactor(v); return this; }
        public Builder connectRetryEnabled(boolean v) { cfg.connectRetryEnabled(v); return this; }
        public Builder connectMaxRetries(int v) { cfg.connectMaxRetries(v); return this; }
        public Builder connectInitialBackoffMs(long v) { cfg.connectInitialBackoffMs(v); return this; }
        public Builder connectMaxBackoffMs(long v) { cfg.connectMaxBackoffMs(v); return this; }
        public Builder connectJitterFactor(float v) { cfg.connectJitterFactor(v); return this; }
        public Builder connectRetryOn(Set<ConnectError> set) { cfg.connectRetryOn(set); return this; }
        public Builder keepAlive(boolean v) { cfg.keepAlive(v); return this; }
        public Builder soLingerSec(Integer v) { cfg.soLingerSec(v); return this; }
        public Builder receiveBufferSize(Integer v) { cfg.receiveBufferSize(v); return this; }
        public Builder sendBufferSize(Integer v) { cfg.sendBufferSize(v); return this; }
        public Builder maxInFlightRequests(int v) { cfg.maxInFlightRequests(v); return this; }
        public Builder requestQueueCapacity(int v) { cfg.requestQueueCapacity(v); return this; }
        public Builder minInterRequestDelayMs(Long v) { cfg.minInterRequestDelayMs(v); return this; }
        public Builder logger(Logger l) { cfg.logger(l); return this; }
        public Builder stateListener(StateListener l) { this.listener = Objects.requireNonNull(l); return this; }
        public Builder mainThreadHandler(Handler h) { this.mainHandler = h; return this; }

        public TcpClient build() { return new TcpClient(cfg.build(), listener, mainHandler); }
    }

    private final class MetricsAndStateListener implements StateListener {
        private final TcpConfig cfg;
        private final StateListener delegate;
        MetricsAndStateListener(TcpConfig cfg, StateListener d) { this.cfg = cfg; this.delegate = d; }
        @Override public void onStateChanged(ConnectionState state, StateInfo info) {
            lastState = state;
            if (state == ConnectionState.READY) lastConnectEpochMs = System.currentTimeMillis();
            if (state == ConnectionState.DISCONNECTED) lastDisconnectEpochMs = System.currentTimeMillis();
            delegate.onStateChanged(state, info);
        }
        @Override public void onTraffic(TrafficEvent event) {
            switch (event.kind) {
                case SENT_BYTES: cfg.metrics.addBytesOut(event.byteCount); break;
                case RECEIVED_BYTES: cfg.metrics.addBytesIn(event.byteCount); break;
                default: break;
            }
            delegate.onTraffic(event);
        }
        @Override public void onError(Throwable t, ErrorStage stage) { delegate.onError(t, stage); }
        @Override public void onRetryScheduled(int attempt, long backoffMs, Throwable cause) { delegate.onRetryScheduled(attempt, backoffMs, cause); }
        @Override public void onRetryExhausted(Throwable lastError) { delegate.onRetryExhausted(lastError); }
    }
}


