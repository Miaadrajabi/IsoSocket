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
import com.miaad.isosocket.tls.TlsUtil;
import com.miaad.isosocket.util.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * NIO-based engine with optional TLS via SSLEngine.
 */
final class NioEngine implements TcpEngine {
    private final TcpConfig config;
    private final Framer framer;
    private final Logger logger;
    private final StateManager stateManager;

    private volatile SocketChannel channel;
    private volatile Selector selector;
    private volatile SocketAddress remoteAddress;

    private volatile boolean tls;
    private volatile SSLEngine sslEngine;
    private ByteBuffer netRead;
    private ByteBuffer netWrite;
    private ByteBuffer appRead;
    private ByteBuffer appWrite;
    private volatile boolean ready;

    NioEngine(TcpConfig config, StateManager stateManager) {
        this.config = config;
        this.framer = config.framer;
        this.logger = config.logger;
        this.stateManager = stateManager;
    }

    @Override
    public void connect() throws IOException, TimeoutException {
        if (isConnected()) return;
        stateManager.stateChanged(ConnectionState.CONNECTING, new StateInfo(0, 0, null));
        channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            channel.socket().setTcpNoDelay(config.tcpNoDelay);
            channel.socket().setKeepAlive(config.keepAlive);
            if (config.soLingerSec != null) channel.socket().setSoLinger(true, config.soLingerSec);
            if (config.receiveBufferSize != null) channel.socket().setReceiveBufferSize(config.receiveBufferSize);
            if (config.sendBufferSize != null) channel.socket().setSendBufferSize(config.sendBufferSize);

            selector = Selector.open();
            InetSocketAddress addr = new InetSocketAddress(config.host, config.port);
            remoteAddress = addr;
            channel.register(selector, SelectionKey.OP_CONNECT);
            channel.connect(addr);
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.connectTimeoutMs);
            while (true) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new TimeoutException("connect timeout");
                int n = selector.select(TimeUnit.NANOSECONDS.toMillis(remaining));
                if (n == 0) continue;
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); it.remove();
                    if (key.isConnectable()) {
                        if (channel.finishConnect()) {
                            stateManager.stateChanged(ConnectionState.CONNECTED, new StateInfo(0, 0, remoteAddress));
                            key.interestOps(SelectionKey.OP_READ);
                            if (config.tlsOptions.enableTls) {
                                doTlsHandshake();
                            } else {
                                ready = true;
                            }
                            stateManager.stateChanged(ConnectionState.READY, new StateInfo(0, 0, remoteAddress));
                            return;
                        }
                    }
                }
            }
        } catch (IOException | TimeoutException e) {
            closeQuietly();
            stateManager.stateChanged(ConnectionState.ERROR, new StateInfo(0, 0, remoteAddress));
            stateManager.error(e, StateListener.ErrorStage.CONNECT);
            if (e instanceof TimeoutException) throw (TimeoutException) e;
            throw (e instanceof IOException) ? (IOException) e : new IOException(e);
        }
    }

    private void doTlsHandshake() throws IOException, TimeoutException {
        this.tls = true;
        X509TrustManager delegate = config.tlsOptions.trustManager != null ? config.tlsOptions.trustManager : TlsUtil.systemDefaultTrustManager();
        X509TrustManager tm = new PinningTrustManager(delegate, config.tlsOptions.pinnedSpkiSha256);
        SSLContext ctx = TlsUtil.buildSslContext(tm, config.tlsOptions.tlsProtocolVersions);
        sslEngine = ctx.createSSLEngine(config.host, config.port);
        sslEngine.setUseClientMode(true);
        if (config.tlsOptions.tlsProtocolVersions != null && !config.tlsOptions.tlsProtocolVersions.isEmpty()) {
            sslEngine.setEnabledProtocols(config.tlsOptions.tlsProtocolVersions.toArray(new String[0]));
        }
        SSLSession sess = sslEngine.getSession();
        netRead = ByteBuffer.allocate(sess.getPacketBufferSize());
        netWrite = ByteBuffer.allocate(sess.getPacketBufferSize());
        appRead = ByteBuffer.allocate(sess.getApplicationBufferSize() * 2);
        appWrite = ByteBuffer.allocate(sess.getApplicationBufferSize());

        stateManager.stateChanged(ConnectionState.HANDSHAKING, new StateInfo(0, 0, remoteAddress));
        sslEngine.beginHandshake();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.handshakeTimeoutMs);
        while (true) {
            SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();
            switch (hs) {
                case NEED_WRAP:
                    netWrite.clear();
                    SSLEngineResult wrap = sslEngine.wrap(ByteBuffer.allocate(0), netWrite);
                    if (wrap.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                        netWrite = enlarge(netWrite, sslEngine.getSession().getPacketBufferSize());
                        continue;
                    }
                    netWrite.flip();
                    while (netWrite.hasRemaining()) {
                        channel.write(netWrite);
                    }
                    channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
                    break;
                case NEED_UNWRAP:
                    if (selector.select(TimeUnit.NANOSECONDS.toMillis(Math.max(1, deadline - System.nanoTime()))) == 0) {
                        if (System.nanoTime() >= deadline) throw new TimeoutException("handshake timeout");
                        continue;
                    }
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> it = keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey k = it.next(); it.remove();
                        if (k.isReadable()) {
                            if (channel.read(netRead) == -1) throw new EOFException("closed during handshake");
                            netRead.flip();
                            SSLEngineResult res = sslEngine.unwrap(netRead, appRead);
                            netRead.compact();
                            if (res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                                appRead = enlarge(appRead, sslEngine.getSession().getApplicationBufferSize());
                            }
                        }
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) task.run();
                    break;
                case FINISHED:
                case NOT_HANDSHAKING:
                    if (config.tlsOptions.verifyHostname) {
                        TlsUtil.verifyHostnameOrThrow(sslEngine.getSession(), config.host);
                    }
                    ready = true;
                    return;
            }
            if (System.nanoTime() >= deadline) throw new TimeoutException("handshake timeout");
        }
    }

    private static ByteBuffer enlarge(ByteBuffer buf, int sessionSize) {
        int newCap = Math.max(buf.capacity() * 2, sessionSize);
        ByteBuffer bigger = ByteBuffer.allocate(newCap);
        buf.flip();
        bigger.put(buf);
        return bigger;
    }

    @Override
    public boolean isConnected() {
        SocketChannel ch = channel;
        return ch != null && ch.isOpen() && ch.isConnected();
    }

    @Override
    public boolean isReady() { return ready && isConnected(); }

    @Override
    public TcpResponse sendAndReceive(byte[] request, long perRequestTimeoutMs) throws IOException, TimeoutException {
        if (!isReady()) throw new IOException("Not connected");
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(perRequestTimeoutMs);
        stateManager.stateChanged(ConnectionState.SENDING, new StateInfo(0, 0, remoteAddress));
        ByteBuffer[] frames = framer.frameForSend(request);
        int totalOut = 0;
        if (tls) {
            // write via SSLEngine
            for (ByteBuffer frame : frames) {
                while (frame.hasRemaining()) {
                    netWrite.clear();
                    SSLEngineResult res = sslEngine.wrap(frame, netWrite);
                    if (res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                        netWrite = enlarge(netWrite, sslEngine.getSession().getPacketBufferSize());
                        continue;
                    }
                    netWrite.flip();
                    while (netWrite.hasRemaining()) {
                        channel.write(netWrite);
                    }
                }
                totalOut += frame.limit();
            }
        } else {
            for (ByteBuffer frame : frames) {
                while (frame.hasRemaining()) channel.write(frame);
                totalOut += frame.limit();
            }
        }
        stateManager.traffic(new TrafficEvent(TrafficEvent.Kind.SENT_BYTES, totalOut));

        stateManager.stateChanged(ConnectionState.RECEIVING, new StateInfo(0, 0, remoteAddress));
        long start = System.currentTimeMillis();
        if (tls) {
            // read, unwrap, and feed to framer
            while (true) {
                long remainMs = TimeUnit.NANOSECONDS.toMillis(Math.max(1, deadline - System.nanoTime()));
                if (remainMs <= 0) throw new TimeoutException("read timeout");
                int n = selector.select(remainMs);
                if (n == 0) {
                    if (System.nanoTime() >= deadline) throw new TimeoutException("read timeout");
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); it.remove();
                    if (key.isReadable()) {
                        if (channel.read(netRead) == -1) throw new EOFException("closed");
                        netRead.flip();
                        SSLEngineResult res = sslEngine.unwrap(netRead, appRead);
                        netRead.compact();
                        if (res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                            appRead = enlarge(appRead, sslEngine.getSession().getApplicationBufferSize());
                        }
                        appRead.flip();
                        Framer.Result fr = framer.tryDecode(appRead);
                        if (fr.bytesConsumed > 0) {
                            int remaining = appRead.remaining();
                            if (remaining > 0) {
                                byte[] tmp = new byte[remaining];
                                appRead.get(tmp);
                                appRead.clear();
                                appRead.put(tmp);
                            } else {
                                appRead.clear();
                            }
                        } else {
                            appRead.position(appRead.limit());
                            appRead.limit(appRead.capacity());
                        }
                        if (fr.frames.length > 0) {
                            stateManager.traffic(new TrafficEvent(TrafficEvent.Kind.RECEIVED_BYTES, res.bytesProduced()));
                            long rtt = System.currentTimeMillis() - start;
                            return new TcpResponse(fr.frames[0], rtt, remoteAddress, start, System.currentTimeMillis());
                        }
                    }
                }
            }
        } else {
            ByteBuffer inbound = ByteBuffer.allocate(64 * 1024);
            while (true) {
                long remainMs = TimeUnit.NANOSECONDS.toMillis(Math.max(1, deadline - System.nanoTime()));
                if (remainMs <= 0) throw new TimeoutException("read timeout");
                selector.select(remainMs);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); it.remove();
                    if (key.isReadable()) {
                        int r = channel.read(inbound);
                        if (r == -1) throw new EOFException("closed");
                        inbound.flip();
                        Framer.Result res = framer.tryDecode(inbound);
                        if (res.bytesConsumed > 0) {
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
                            stateManager.traffic(new TrafficEvent(TrafficEvent.Kind.RECEIVED_BYTES, r));
                            long rtt = System.currentTimeMillis() - start;
                            return new TcpResponse(res.frames[0], rtt, remoteAddress, start, System.currentTimeMillis());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        ready = false;
        closeQuietly();
        stateManager.stateChanged(ConnectionState.CLOSED, new StateInfo(0, 0, remoteAddress));
    }

    private void closeQuietly() {
        try { if (selector != null) selector.close(); } catch (IOException ignored) {}
        try { if (channel != null) channel.close(); } catch (IOException ignored) {}
    }
}


