/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket;


import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Common interface for mode-specific engines (blocking vs NIO).
 */
interface TcpEngine extends Closeable {
    void connect() throws IOException, TimeoutException;
    boolean isConnected();
    boolean isReady();
    TcpResponse sendAndReceive(byte[] request, long perRequestTimeoutMs) throws IOException, TimeoutException;
}


