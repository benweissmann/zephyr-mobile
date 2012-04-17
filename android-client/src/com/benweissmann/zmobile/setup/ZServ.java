package com.benweissmann.zmobile.setup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ZServ {
    private final String hostName;
    private final int port;
    private final String encodedKeyStore;
    
    public ZServ(String hostName, int port, String encodedKeyStore) {
        this.hostName = hostName;
        this.port = port;
        this.encodedKeyStore = encodedKeyStore;
    }
    
    /**
     * Return a KeyStore. Throws NoStoredZServException if a keystore can't
     * be constructed.
     */
    public KeyStore getKeyStore() throws NoStoredZServException {
        try {
            KeyStore localTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            localTrustStore.load(null);
            
            InputStream in = new ByteArrayInputStream(this.encodedKeyStore.getBytes());
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
            localTrustStore.setCertificateEntry("client", cert);
            
            return localTrustStore;
        }
        catch (KeyStoreException e) {
            throw new NoStoredZServException("Got KeyStoreException in getKeyStore", e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new NoStoredZServException("Got NoSuchAlgorithmException in getKeyStore", e);
        }
        catch (CertificateException e) {
            throw new NoStoredZServException("Got CertificateException in getKeyStore", e);
        }
        catch (IOException e) {
            throw new NoStoredZServException("Got IOException in getKeyStore", e);
        }
    }
    
    /**
     * Returns the XML-RPC server's URL. throws NoStoredZServException if the
     * hostName is malformed.
     */
    public URL getURL() throws NoStoredZServException {
        try {
            return new URL(String.format("https://" + this.hostName + ":" + this.port));
        }
        catch (MalformedURLException e) {
            throw new NoStoredZServException(e);
        }
    }
}
