/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

/**
 * A secure socket protocol implementation which acts as a factory for {@link SSLEngine} and {@link
 * SslHandler}. Internally, it is implemented via JDK's {@link SSLContext} or OpenSSL's {@code
 * SSL_CTX}.
 *
 * <p>Borrowed from <a href="http://netty.io">Netty</a>
 */
public abstract class SslContext {
  static final CertificateFactory X509_CERT_FACTORY;

  static {
    try {
      X509_CERT_FACTORY = CertificateFactory.getInstance("X.509");
    } catch (CertificateException e) {
      throw new IllegalStateException("unable to instance X.509 CertificateFactory", e);
    }
  }

  public static SslContext newServerContextInternal(
      final String provider,
      final InputStream trustCertChainFile,
      final InputStream keyCertChainFile,
      final InputStream keyFile,
      final String keyPassword,
      final long sessionCacheSize,
      final long sessionTimeout)
      throws SSLException {
    return new JdkSslServerContext(
        provider,
        trustCertChainFile,
        keyCertChainFile,
        keyFile,
        keyPassword,
        sessionCacheSize,
        sessionTimeout);
  }

  public abstract long sessionCacheSize();

  public abstract long sessionTimeout();

  public abstract SSLContext context();

  public abstract SSLSessionContext sessionContext();

  protected static PKCS8EncodedKeySpec generateKeySpec(final char[] password, final byte[] key)
      throws IOException,
          NoSuchAlgorithmException,
          NoSuchPaddingException,
          InvalidKeySpecException,
          InvalidKeyException,
          InvalidAlgorithmParameterException {

    if (password == null || password.length == 0) {
      return new PKCS8EncodedKeySpec(key);
    }

    EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
    SecretKeyFactory keyFactory =
        SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
    SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);

    Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
    cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());

    return encryptedPrivateKeyInfo.getKeySpec(cipher);
  }

  static KeyStore buildKeyStore(
      final InputStream certChainFile, final InputStream keyFile, final char[] keyPasswordChars)
      throws KeyStoreException,
          NoSuchAlgorithmException,
          NoSuchPaddingException,
          InvalidKeySpecException,
          InvalidAlgorithmParameterException,
          CertificateException,
          KeyException,
          IOException {
    ByteBuffer encodedKeyBuf = PemReader.readPrivateKey(keyFile);
    byte[] encodedKey = encodedKeyBuf.array();

    PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPasswordChars, encodedKey);

    PrivateKey key;
    try {
      key = KeyFactory.getInstance("RSA").generatePrivate(encodedKeySpec);
    } catch (InvalidKeySpecException ignore) {
      try {
        key = KeyFactory.getInstance("DSA").generatePrivate(encodedKeySpec);
      } catch (InvalidKeySpecException ignore2) {
        try {
          key = KeyFactory.getInstance("EC").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException e) {
          throw new InvalidKeySpecException("Neither RSA, DSA nor EC worked", e);
        }
      }
    }

    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    List<ByteBuffer> certs = PemReader.readCertificates(certChainFile);
    List<Certificate> certChain = new ArrayList<>(certs.size());

    for (ByteBuffer buf : certs) {
      certChain.add(cf.generateCertificate(new ByteArrayInputStream(buf.array())));
    }

    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);
    ks.setKeyEntry("key", key, keyPasswordChars, certChain.toArray(new Certificate[0]));
    return ks;
  }

  protected static TrustManagerFactory buildTrustManagerFactory(
      final InputStream certChainFile, TrustManagerFactory trustManagerFactory)
      throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");

    List<ByteBuffer> certs = PemReader.readCertificates(certChainFile);

    for (ByteBuffer buf : certs) {
      X509Certificate cert =
          (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(buf.array()));
      X500Principal principal = cert.getSubjectX500Principal();
      ks.setCertificateEntry(principal.getName("RFC2253"), cert);
    }

    if (trustManagerFactory == null) {
      trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    }
    trustManagerFactory.init(ks);

    return trustManagerFactory;
  }
}
