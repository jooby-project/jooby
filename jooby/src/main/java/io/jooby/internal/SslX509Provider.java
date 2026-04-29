/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

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
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import io.jooby.SneakyThrows;
import io.jooby.SslOptions;

public class SslX509Provider implements SslContextProvider {

  @Override
  public boolean supports(String type) {
    return SslOptions.X509.equalsIgnoreCase(type);
  }

  @Override
  public SSLContext create(ClassLoader loader, String provider, SslOptions options) {
    try (options) {
      char[] password = toCharArray(options.getPassword());

      var store = buildKeyStore(options.getCert(), options.getPrivateKey(), password);
      var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(store, password);

      TrustManager[] tms = null;
      if (options.getTrustCert() != null) {
        TrustManagerFactory tmf = buildTrustManagerFactory(options.getTrustCert());
        tms = tmf.getTrustManagers();
      }

      SSLContext context =
          provider == null
              ? SSLContext.getInstance("TLS")
              : SSLContext.getInstance("TLS", provider);

      context.init(kmf.getKeyManagers(), tms, null);

      return context;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private KeyStore buildKeyStore(
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

  private TrustManagerFactory buildTrustManagerFactory(final InputStream certChainFile)
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

    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(ks);

    return trustManagerFactory;
  }

  private PKCS8EncodedKeySpec generateKeySpec(final char[] password, final byte[] key)
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

  private char[] toCharArray(String password) {
    return password == null ? null : password.toCharArray();
  }
}
