/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.x509;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link SslContext} which uses JDK's SSL/TLS implementation.
 *
 * <p>Borrowed from <a href="http://netty.io">Netty</a>
 */
public abstract class JdkSslContext extends SslContext {

  /** The logging system. */
  private static final Logger logger = LoggerFactory.getLogger(JdkSslContext.class);

  static final String PROTOCOL = "TLS";
  static final String[] PROTOCOLS;
  static final List<String> DEFAULT_CIPHERS;
  static final Set<String> SUPPORTED_CIPHERS;

  private static final char[] EMPTY_CHARS = new char[0];

  static {
    SSLContext context;
    try {
      context = SSLContext.getInstance(PROTOCOL);
      context.init(null, null, null);
    } catch (Exception e) {
      throw new Error("failed to initialize the default SSL context", e);
    }

    SSLEngine engine = context.createSSLEngine();

    // Choose the sensible default list of protocols.
    final String[] supportedProtocols = engine.getSupportedProtocols();
    Set<String> supportedProtocolsSet = new HashSet<>(Arrays.asList(supportedProtocols));
    List<String> protocols = new ArrayList<>();

    // Modernized for Java 21: prioritize TLS 1.3 and TLS 1.2
    addIfSupported(supportedProtocolsSet, protocols, "TLSv1.3", "TLSv1.2");

    if (!protocols.isEmpty()) {
      PROTOCOLS = protocols.toArray(new String[0]);
    } else {
      PROTOCOLS = engine.getEnabledProtocols();
    }

    // Choose the sensible default list of cipher suites.
    final String[] supportedCiphers = engine.getSupportedCipherSuites();
    SUPPORTED_CIPHERS = new HashSet<>(Arrays.asList(supportedCiphers));
    List<String> ciphers = new ArrayList<>();

    addIfSupported(
        SUPPORTED_CIPHERS,
        ciphers,
        // TLS 1.3 Ciphers
        "TLS_AES_256_GCM_SHA384",
        "TLS_AES_128_GCM_SHA256",
        "TLS_CHACHA20_POLY1305_SHA256",
        // Modern TLS 1.2 Ciphers
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_256_CBC_SHA");

    if (ciphers.isEmpty()) {
      // Use the default from JDK as fallback.
      for (String cipher : engine.getEnabledCipherSuites()) {
        if (cipher.contains("_RC4_")) {
          continue;
        }
        ciphers.add(cipher);
      }
    }
    DEFAULT_CIPHERS = Collections.unmodifiableList(ciphers);

    if (logger.isDebugEnabled()) {
      logger.debug("Default protocols (JDK): {} ", Arrays.asList(PROTOCOLS));
      logger.debug("Default cipher suites (JDK): {}", DEFAULT_CIPHERS);
    }
  }

  private static void addIfSupported(
      final Set<String> supported, final List<String> enabled, final String... names) {
    for (String n : names) {
      if (supported.contains(n)) {
        enabled.add(n);
      }
    }
  }

  @Override
  public final SSLSessionContext sessionContext() {
    return context().getServerSessionContext();
  }

  @Override
  public final long sessionCacheSize() {
    return sessionContext().getSessionCacheSize();
  }

  @Override
  public final long sessionTimeout() {
    return sessionContext().getSessionTimeout();
  }

  protected static KeyManagerFactory buildKeyManagerFactory(
      final InputStream certChainFile, final InputStream keyFile, final String keyPassword)
      throws UnrecoverableKeyException,
          KeyStoreException,
          NoSuchAlgorithmException,
          NoSuchPaddingException,
          InvalidKeySpecException,
          InvalidAlgorithmParameterException,
          CertificateException,
          KeyException,
          IOException {
    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }
    return buildKeyManagerFactory(certChainFile, algorithm, keyFile, keyPassword);
  }

  protected static KeyManagerFactory buildKeyManagerFactory(
      final InputStream certChainFile,
      final String keyAlgorithm,
      final InputStream keyFile,
      final String keyPassword)
      throws KeyStoreException,
          NoSuchAlgorithmException,
          NoSuchPaddingException,
          InvalidKeySpecException,
          InvalidAlgorithmParameterException,
          IOException,
          CertificateException,
          KeyException,
          UnrecoverableKeyException {
    char[] keyPasswordChars = keyPassword == null ? EMPTY_CHARS : keyPassword.toCharArray();
    KeyStore ks = buildKeyStore(certChainFile, keyFile, keyPasswordChars);
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyAlgorithm);
    kmf.init(ks, keyPasswordChars);

    return kmf;
  }
}
