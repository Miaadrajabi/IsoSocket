/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.tls;

import android.os.Build;


import com.miaad.isosocket.util.Logger;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/** Utility helpers for TLS sockets and engines. */
public final class TlsUtil {
    private TlsUtil() {}

    public static X509TrustManager systemDefaultTrustManager() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((java.security.KeyStore) null);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) return (X509TrustManager) tm;
            }
            throw new IllegalStateException("No system X509TrustManager available");
        } catch (Exception e) {
            throw new RuntimeException("Unable to load system trust manager", e);
        }
    }

    public static SSLContext buildSslContext(X509TrustManager tm, List<String> protocols) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            TrustManager[] tms = new TrustManager[] { tm == null ? systemDefaultTrustManager() : tm };
            ctx.init(null, tms, null);
            return ctx;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLSocket wrapSocket(Socket plain, String host, List<String> protocols, boolean enableSni, Logger logger) throws IOException {
        SSLContext ctx = buildSslContext(null, protocols);
        SSLSocketFactory factory = ctx.getSocketFactory();
        SSLSocket ssl = (SSLSocket) factory.createSocket(plain, host, plain.getPort(), true);
        if (protocols != null && !protocols.isEmpty()) ssl.setEnabledProtocols(protocols.toArray(new String[0]));
        if (enableSni && Build.VERSION.SDK_INT >= 24) {
            SSLParameters p = ssl.getSSLParameters();
            p.setServerNames(java.util.Collections.singletonList(new javax.net.ssl.SNIHostName(host)));
            ssl.setSSLParameters(p);
        }
        return ssl;
    }

    public static void verifyHostnameOrThrow(SSLSession session, String host) throws IOException {
        HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
        if (!verifier.verify(host, session)) {
            throw new javax.net.ssl.SSLPeerUnverifiedException("Hostname verification failed for " + host);
        }
    }

    public static List<String> spkiPinsSha256(X509Certificate[] chain) {
        List<String> pins = new ArrayList<>();
        for (X509Certificate cert : chain) {
            byte[] spki = cert.getPublicKey().getEncoded();
            byte[] sha256 = sha256(spki);
            pins.add(base64(sha256));
        }
        return pins;
    }

    private static byte[] sha256(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String base64(byte[] data) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Base64.getEncoder().encodeToString(data);
        } else {
            return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
        }
    }
}


