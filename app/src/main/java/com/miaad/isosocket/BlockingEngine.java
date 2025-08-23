/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket;

import com.miaad.isosocket.framing.Framer;
import com.miaad.isosocket.state.ConnectionState;
import com.miaad.isosocket.state.StateInfo;
import com.miaad.isosocket.state.StateListener;
import com.miaad.isosocket.state.StateManager;
import com.miaad.isosocket.state.TrafficEvent;
import com.miaad.isosocket.tls.PinningTrustManager;
import com.miaad.isosocket.tls.TlsOptions;
import com.miaad.isosocket.tls.TlsUtil;
import com.miaad.isosocket.util.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Blocking I/O engine based on Socket/SSLSocket.
 */
final class BlockingEngine implements TcpEngine {
    private final TcpConfig config;
    private final Framer framer;
    private final Logger logger;
    private final StateManager stateManager;

    private final Object connectLock = new Object();
    private volatile boolean closed = false;
    private volatile Socket socket;
    private volatile SocketAddress remoteAddress;

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    private volatile ScheduledFuture<?> handshakeTimeoutFuture;

    BlockingEngine(TcpConfig config, StateManager stateManager) {
        this.config = config;
        this.framer = config.framer;
        this.logger = config.logger;
        this.stateManager = stateManager;
    }

    @Override
    public void connect() throws IOException, TimeoutException {
        synchronized (connectLock) {
            if (closed) throw new IOException("Closed");
            if (isConnected()) return;

            stateManager.stateChanged(ConnectionState.CONNECTING, new StateInfo(0, 0, null));
            Socket s = new Socket();
            s.setTcpNoDelay(config.tcpNoDelay);
            s.setKeepAlive(config.keepAlive);
            if (config.soLingerSec != null) s.setSoLinger(true, config.soLingerSec);
            if (config.receiveBufferSize != null) s.setReceiveBufferSize(config.receiveBufferSize);
            if (config.sendBufferSize != null) s.setSendBufferSize(config.sendBufferSize);

            try {
                InetSocketAddress addr = new InetSocketAddress(config.host, config.port);
                remoteAddress = addr;
                s.connect(addr, config.connectTimeoutMs);
                stateManager.stateChanged(ConnectionState.CONNECTED, new StateInfo(0, 0, remoteAddress));
                if (config.tlsOptions.enableTls) {
                    stateManager.stateChanged(ConnectionState.HANDSHAKING, new StateInfo(0, 0, remoteAddress));
                    SSLSocket ssl = startTls(s, config.tlsOptions);
                    scheduleHandshakeTimeout(ssl);
                    ssl.startHandshake();
                    cancelHandshakeTimeout();
                    if (config.tlsOptions.verifyHostname) {
                        TlsUtil.verifyHostnameOrThrow(ssl.getSession(), config.host);
                    }
                    s = ssl;
                }
                socket = s;
                stateManager.stateChanged(ConnectionState.READY, new StateInfo(0, 0, remoteAddress));
            } catch (IOException e) {
                safeClose(s);
                stateManager.stateChanged(ConnectionState.ERROR, new StateInfo(0, 0, remoteAddress));
                stateManager.error(e, StateListener.ErrorStage.CONNECT);
                throw e;
            }
        }
    }

    private void scheduleHandshakeTimeout(SSLSocket ssl) {
        cancelHandshakeTimeout();
        handshakeTimeoutFuture = scheduler.schedule(() -> {
            try { ssl.close(); } catch (IOException ignored) {}
        }, config.handshakeTimeoutMs, TimeUnit.MILLISECONDS);
    }

    private void cancelHandshakeTimeout() {
        ScheduledFuture<?> f = handshakeTimeoutFuture;
        if (f != null) f.cancel(true);
    }

    private SSLSocket startTls(Socket plain, TlsOptions opts) throws IOException {
        X509TrustManager delegate = opts.trustManager != null ? opts.trustManager : TlsUtil.systemDefaultTrustManager();
        X509TrustManager tm = new PinningTrustManager(delegate, opts.pinnedSpkiSha256);
        SSLContext ctx = TlsUtil.buildSslContext(tm, opts.tlsProtocolVersions);
        SSLSocketFactory factory = ctx.getSocketFactory();
        SSLSocket ssl = (SSLSocket) factory.createSocket(plain, config.host, config.port, true);
        if (opts.tlsProtocolVersions != null && !opts.tlsProtocolVersions.isEmpty()) {
            ssl.setEnabledProtocols(opts.tlsProtocolVersions.toArray(new String[0]));
        }
        return ssl;
    }

    @Override
    public boolean isConnected() {
        Socket s = socket;
        return s != null && s.isConnected() && !s.isClosed();
    }

    @Override
    public boolean isReady() { return isConnected(); }

    @Override
    public TcpResponse sendAndReceive(byte[] request, long perRequestTimeoutMs) throws IOException, TimeoutException {
        Socket s = socket;
        if (s == null || !isReady()) throw new IOException("Not connected");
        s.setSoTimeout((int) Math.max(1, Math.min(Integer.MAX_VALUE, perRequestTimeoutMs)));
        stateManager.stateChanged(ConnectionState.SENDING, new StateInfo(0, 0, remoteAddress));
        ByteBuffer[] out = framer.frameForSend(request);
        int totalOut = 0;
        for (ByteBuffer b : out) {
            byte[] arr = new byte[b.remaining()];
            b.get(arr);
            s.getOutputStream().write(arr);
            totalOut += arr.length;
        }
        s.getOutputStream().flush();
        stateManager.traffic(new TrafficEvent(TrafficEvent.Kind.SENT_BYTES, totalOut));

        stateManager.stateChanged(ConnectionState.RECEIVING, new StateInfo(0, 0, remoteAddress));
        long startTime = System.currentTimeMillis();
        ByteBuffer inbound = ByteBuffer.allocate(64 * 1024);
        while (true) {
            int r = s.getInputStream().read(inbound.array(), inbound.position(), inbound.remaining());
            if (r == -1) throw new EOFException("Connection closed");
            inbound.position(inbound.position() + r);
            stateManager.traffic(new TrafficEvent(TrafficEvent.Kind.RECEIVED_BYTES, r));
            inbound.flip();
            Framer.Result res = framer.tryDecode(inbound);
            if (res.bytesConsumed > 0) {
                // compact buffer
                int remaining = inbound.remaining();
                if (remaining > 0) {
                    byte[] tmp = new byte[remaining];
                    inbound.get(tmp);
                    inbound.clear();
                    inbound.put(tmp);
                } else {
                    inbound.clear();
                }
            } else {
                inbound.position(inbound.limit());
                inbound.limit(inbound.capacity());
            }
            if (res.frames.length > 0) {
                byte[] payload = res.frames[0];
                long endTime = System.currentTimeMillis();
                long rtt = endTime - startTime;
                return new TcpResponse(payload, rtt, remoteAddress, startTime, endTime);
            }
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        cancelHandshakeTimeout();
        Socket s = socket;
        socket = null;
        if (s != null) safeClose(s);
        stateManager.stateChanged(ConnectionState.CLOSED, new StateInfo(0, 0, remoteAddress));
    }

    private static void safeClose(Socket s) {
        try { s.close(); } catch (IOException ignored) {}
    }
}


