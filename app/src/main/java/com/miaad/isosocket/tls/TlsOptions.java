/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.tls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.X509TrustManager;

/**
 * TLS configuration options.
 */
public final class TlsOptions {
    public final boolean enableTls;
    public final X509TrustManager trustManager;
    public final boolean verifyHostname;
    public final List<String> pinnedSpkiSha256;
    public final List<String> tlsProtocolVersions;

    private TlsOptions(Builder b) {
        this.enableTls = b.enableTls;
        this.trustManager = b.trustManager;
        this.verifyHostname = b.verifyHostname;
        this.pinnedSpkiSha256 = Collections.unmodifiableList(new ArrayList<>(b.pinnedSpkiSha256));
        this.tlsProtocolVersions = Collections.unmodifiableList(new ArrayList<>(b.tlsProtocolVersions));
    }

    public static final class Builder {
        private boolean enableTls = false;
        private X509TrustManager trustManager;
        private boolean verifyHostname = false;
        private final List<String> pinnedSpkiSha256 = new ArrayList<>();
        private final List<String> tlsProtocolVersions = new ArrayList<>();

        public Builder enableTls(boolean enable) { this.enableTls = enable; return this; }
        public Builder trustManager(X509TrustManager tm) { this.trustManager = tm; return this; }
        public Builder verifyHostname(boolean verify) { this.verifyHostname = verify; return this; }
        public Builder pinnedSpkiSha256(List<String> pins) { this.pinnedSpkiSha256.clear(); if (pins != null) this.pinnedSpkiSha256.addAll(pins); return this; }
        public Builder tlsProtocolVersions(List<String> versions) { this.tlsProtocolVersions.clear(); if (versions != null) this.tlsProtocolVersions.addAll(versions); return this; }

        public TlsOptions build() { return new TlsOptions(this); }
    }
}


