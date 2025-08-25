package com.miaad.isosocket;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

interface TcpEngine extends Closeable {
    void connect() throws IOException, TimeoutException;

    boolean isConnected();

    boolean isReady();

    TcpResponse sendAndReceive(byte[] var1, long var2) throws IOException, TimeoutException;
}
