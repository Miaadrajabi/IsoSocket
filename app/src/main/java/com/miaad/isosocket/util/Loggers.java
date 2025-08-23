/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.util;

import android.util.Log;

/**
 * Factory for default Logger implementations.
 */
public final class Loggers {
    private Loggers() {}

    public static Logger androidTag(String tag, Logger.Level minLevel) {
        return (level, message, t) -> {
            if (level.ordinal() > minLevel.ordinal()) return;
            switch (level) {
                case ERROR:
                    if (t != null) Log.e(tag, message, t); else Log.e(tag, message);
                    break;
                case WARN:
                    Log.w(tag, message);
                    break;
                case INFO:
                    Log.i(tag, message);
                    break;
                case DEBUG:
                    Log.d(tag, message);
                    break;
                default:
                    Log.v(tag, message);
            }
        };
    }
}


