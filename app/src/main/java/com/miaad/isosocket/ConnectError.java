/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket;

/**
 * Categories for connection failures to drive retry policy.
 */
public enum ConnectError {
    TIMEOUT,
    DNS,
    NETWORK_UNREACHABLE,
    CONNECTION_REFUSED,
    HANDSHAKE_TIMEOUT,
    TLS_UNVERIFIED,
    TLS_PINNING_MISMATCH,
    UNKNOWN
}


