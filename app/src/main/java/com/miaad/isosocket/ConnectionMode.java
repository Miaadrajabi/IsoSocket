/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket;

/**
 * Connection mode for the TCP client.
 */
public enum ConnectionMode {
    /** Classic blocking I/O using java.net.Socket or javax.net.ssl.SSLSocket. */
    BLOCKING,
    /** Non-blocking I/O using java.nio channels, Selector, and (optionally) SSLEngine. */
    NON_BLOCKING
}


