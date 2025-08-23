/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket;

import android.os.Handler;

import com.miaad.isosocket.framing.Framer;
import com.miaad.isosocket.framing.LengthPrefixedFramer;
import com.miaad.isosocket.tls.TlsOptions;
import com.miaad.isosocket.util.Logger;
import com.miaad.isosocket.util.Loggers;
import com.miaad.isosocket.util.Metrics;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Immutable configuration for TcpClient.
 */
public final class TcpConfig {
    public final String host;
    public final int port;
    public final ConnectionMode mode;
    public final int connectTimeoutMs;
    public final int readTimeoutMs;
    public final int writeTimeoutMs;
    public final int handshakeTimeoutMs;
    public final int requestTimeoutMs;

    public final boolean tcpNoDelay;
    public final boolean keepAlive;
    public final Integer soLingerSec;
    public final Integer receiveBufferSize;
    public final Integer sendBufferSize;

    public final boolean autoReconnect;
    public final long initialBackoffMs;
    public final long maxBackoffMs;
    public final float jitterFactor;
    public final int maxRetries;

    public final boolean connectRetryEnabled;
    public final int connectMaxRetries;
    public final long connectInitialBackoffMs;
    public final long connectMaxBackoffMs;
    public final float connectJitterFactor;
    public final Set<ConnectError> connectRetryOn;

    public final int maxInFlightRequests;
    public final int requestQueueCapacity;
    public final Long minInterRequestDelayMs;

    public final Framer framer;
    public final TlsOptions tlsOptions;
    public final Logger logger;
    public final Metrics metrics;
    public final Executor callbackExecutor;
    public final Handler mainThreadHandler;

    private TcpConfig(Builder b) {
        this.host = b.host;
        this.port = b.port;
        this.mode = b.mode;
        this.connectTimeoutMs = b.connectTimeoutMs;
        this.readTimeoutMs = b.readTimeoutMs;
        this.writeTimeoutMs = b.writeTimeoutMs;
        this.handshakeTimeoutMs = b.handshakeTimeoutMs;
        this.requestTimeoutMs = b.requestTimeoutMs;
        this.tcpNoDelay = b.tcpNoDelay;
        this.keepAlive = b.keepAlive;
        this.soLingerSec = b.soLingerSec;
        this.receiveBufferSize = b.receiveBufferSize;
        this.sendBufferSize = b.sendBufferSize;
        this.autoReconnect = b.autoReconnect;
        this.initialBackoffMs = b.initialBackoffMs;
        this.maxBackoffMs = b.maxBackoffMs;
        this.jitterFactor = b.jitterFactor;
        this.maxRetries = b.maxRetries;
        this.connectRetryEnabled = b.connectRetryEnabled;
        this.connectMaxRetries = b.connectMaxRetries;
        this.connectInitialBackoffMs = b.connectInitialBackoffMs;
        this.connectMaxBackoffMs = b.connectMaxBackoffMs;
        this.connectJitterFactor = b.connectJitterFactor;
        this.connectRetryOn = Collections.unmodifiableSet(EnumSet.copyOf(b.connectRetryOn));
        this.maxInFlightRequests = b.maxInFlightRequests;
        this.requestQueueCapacity = b.requestQueueCapacity;
        this.minInterRequestDelayMs = b.minInterRequestDelayMs;
        this.framer = b.framer;
        this.tlsOptions = b.tlsOptions;
        this.logger = b.logger;
        this.metrics = b.metrics;
        this.callbackExecutor = b.callbackExecutor;
        this.mainThreadHandler = b.mainThreadHandler;
    }

    public static final class Builder {
        private String host;
        private int port;
        private ConnectionMode mode = ConnectionMode.BLOCKING;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 15000;
        private int writeTimeoutMs = 15000;
        private int handshakeTimeoutMs = 5000;
        private int requestTimeoutMs = 10000;

        private boolean tcpNoDelay = true;
        private boolean keepAlive = true;
        private Integer soLingerSec = null;
        private Integer receiveBufferSize = null;
        private Integer sendBufferSize = null;

        private boolean autoReconnect = false;
        private long initialBackoffMs = 250;
        private long maxBackoffMs = 8000;
        private float jitterFactor = 0.3f;
        private int maxRetries = 3;

        private boolean connectRetryEnabled = true;
        private int connectMaxRetries = 3;
        private long connectInitialBackoffMs = 250;
        private long connectMaxBackoffMs = 8000;
        private float connectJitterFactor = 0.3f;
        private Set<ConnectError> connectRetryOn = EnumSet.of(
                ConnectError.TIMEOUT,
                ConnectError.DNS,
                ConnectError.NETWORK_UNREACHABLE,
                ConnectError.CONNECTION_REFUSED,
                ConnectError.HANDSHAKE_TIMEOUT
        );

        private int maxInFlightRequests = 1;
        private int requestQueueCapacity = 1;
        private Long minInterRequestDelayMs = null;

        private Framer framer = new LengthPrefixedFramer(2, ByteOrder.BIG_ENDIAN, false);
        private TlsOptions tlsOptions = new TlsOptions.Builder().build();
        private Logger logger = Loggers.androidTag("TcpClient", Logger.Level.INFO);
        private Metrics metrics = new Metrics();
        private Executor callbackExecutor = Executors.newSingleThreadExecutor();
        private Handler mainThreadHandler = null;

        public Builder host(String host) { this.host = Objects.requireNonNull(host, "host"); return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder mode(ConnectionMode mode) { this.mode = mode; return this; }
        public Builder connectTimeoutMs(int ms) { this.connectTimeoutMs = ms; return this; }
        public Builder readTimeoutMs(int ms) { this.readTimeoutMs = ms; return this; }
        public Builder writeTimeoutMs(int ms) { this.writeTimeoutMs = ms; return this; }
        public Builder handshakeTimeoutMs(int ms) { this.handshakeTimeoutMs = ms; return this; }
        public Builder requestTimeoutMs(int ms) { this.requestTimeoutMs = ms; return this; }
        public Builder tcpNoDelay(boolean v) { this.tcpNoDelay = v; return this; }
        public Builder keepAlive(boolean v) { this.keepAlive = v; return this; }
        public Builder soLingerSec(Integer v) { this.soLingerSec = v; return this; }
        public Builder receiveBufferSize(Integer v) { this.receiveBufferSize = v; return this; }
        public Builder sendBufferSize(Integer v) { this.sendBufferSize = v; return this; }
        public Builder autoReconnect(boolean v) { this.autoReconnect = v; return this; }
        public Builder initialBackoffMs(long v) { this.initialBackoffMs = v; return this; }
        public Builder maxBackoffMs(long v) { this.maxBackoffMs = v; return this; }
        public Builder jitterFactor(float v) { this.jitterFactor = v; return this; }
        public Builder maxRetries(int v) { this.maxRetries = v; return this; }
        public Builder connectRetryEnabled(boolean v) { this.connectRetryEnabled = v; return this; }
        public Builder connectMaxRetries(int v) { this.connectMaxRetries = v; return this; }
        public Builder connectInitialBackoffMs(long v) { this.connectInitialBackoffMs = v; return this; }
        public Builder connectMaxBackoffMs(long v) { this.connectMaxBackoffMs = v; return this; }
        public Builder connectJitterFactor(float v) { this.connectJitterFactor = v; return this; }
        public Builder connectRetryOn(Set<ConnectError> v) { if (v != null && !v.isEmpty()) this.connectRetryOn = EnumSet.copyOf(v); return this; }
        public Builder maxInFlightRequests(int v) { this.maxInFlightRequests = v; return this; }
        public Builder requestQueueCapacity(int v) { this.requestQueueCapacity = v; return this; }
        public Builder minInterRequestDelayMs(Long v) { this.minInterRequestDelayMs = v; return this; }
        public Builder framer(Framer framer) { this.framer = Objects.requireNonNull(framer); return this; }
        public Builder tlsOptions(TlsOptions tlsOptions) { this.tlsOptions = Objects.requireNonNull(tlsOptions); return this; }
        public Builder logger(Logger logger) { this.logger = Objects.requireNonNull(logger); return this; }
        public Builder metrics(Metrics metrics) { this.metrics = Objects.requireNonNull(metrics); return this; }
        public Builder callbackExecutor(Executor executor) { this.callbackExecutor = Objects.requireNonNull(executor); return this; }
        public Builder mainThreadHandler(Handler handler) { this.mainThreadHandler = handler; return this; }

        public TcpConfig build() {
            if (host == null) throw new IllegalStateException("host required");
            if (port <= 0 || port > 65535) throw new IllegalStateException("valid port required");
            return new TcpConfig(this);
        }
    }
}


