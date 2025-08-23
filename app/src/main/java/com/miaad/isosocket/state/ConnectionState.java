/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.state;

public enum ConnectionState {
    IDLE,
    RESOLVING,
    CONNECTING,
    HANDSHAKING,
    CONNECTED,
    READY,
    SENDING,
    RECEIVING,
    HEARTBEAT,
    BACKING_OFF,
    RECONNECTING,
    DISCONNECTED,
    CLOSED,
    ERROR
}


