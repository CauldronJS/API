package com.cauldronjs.bindings;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXParameters;

public class Crypto {
    public String[] getRootCertificates() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException {
        String filename = System.getProperty("java.home") + "/lib/security/cacerts";
        FileInputStream is = new FileInputStream(filename);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String password = "changeit";
        keystore.load(is, password.toCharArray());
        PKIXParameters params = new PKIXParameters(keystore);

        return (String[]) params.getTrustAnchors().stream().map(anchor -> anchor.getTrustedCert().toString()).toArray();
    }

    public String[] getSSLCiphers() {
        return new String[0];
    }
}
