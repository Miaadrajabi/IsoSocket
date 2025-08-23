/**
 * Author: miaad.rajabi
 * Email: miaad.rajabi@gmail.com
 */
package com.miaad.isosocket.tls;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

/**
 * Decorates a delegate trust manager and enforces SPKI SHA-256 pinning.
 */
public final class PinningTrustManager implements X509TrustManager {
    private final X509TrustManager delegate;
    private final Set<String> pins;

    public PinningTrustManager(X509TrustManager delegate, List<String> spkiPinsSha256) {
        this.delegate = delegate == null ? TlsUtil.systemDefaultTrustManager() : delegate;
        this.pins = new HashSet<>();
        if (spkiPinsSha256 != null) this.pins.addAll(spkiPinsSha256);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkClientTrusted(chain, authType);
        enforcePinning(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkServerTrusted(chain, authType);
        enforcePinning(chain);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    private void enforcePinning(X509Certificate[] chain) throws CertificateException {
        if (pins.isEmpty()) return;
        for (String pin : TlsUtil.spkiPinsSha256(chain)) {
            if (pins.contains(pin)) return;
        }
        throw new CertificateException("SPKI pinning failure: no matching pin found");
    }
}


