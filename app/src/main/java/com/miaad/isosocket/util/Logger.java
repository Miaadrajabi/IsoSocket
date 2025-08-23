/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.util;

/**
 * Minimal pluggable logger abstraction with log-level control.
 */
public interface Logger {
    enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE
    }

    void log(Level level, String message, Throwable t);

    default void error(String message, Throwable t) { log(Level.ERROR, message, t); }

    default void warn(String message) { log(Level.WARN, message, null); }

    default void info(String message) { log(Level.INFO, message, null); }

    default void debug(String message) { log(Level.DEBUG, message, null); }

    default void trace(String message) { log(Level.TRACE, message, null); }
}


