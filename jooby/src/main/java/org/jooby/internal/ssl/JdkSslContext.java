/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.ssl;

import java.io.File;
import java.io.IOException;
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

/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link SslContext} which uses JDK's SSL/TLS implementation.
 * Kindly Borrowed from <a href="http://netty.io">Netty</a>
 */
public abstract class JdkSslContext extends SslContext {

  /** The logging system. */
  private final static Logger logger = LoggerFactory.getLogger(JdkSslContext.class);

  static final String PROTOCOL = "TLS";
  static final String[] PROTOCOLS;
  static final List<String> DEFAULT_CIPHERS;
  static final Set<String> SUPPORTED_CIPHERS;

  private static final char[] EMPTY_CHARS = new char[0];

  static {
    SSLContext context;
    int i;
    try {
      context = SSLContext.getInstance(PROTOCOL);
      context.init(null, null, null);
    } catch (Exception e) {
      throw new Error("failed to initialize the default SSL context", e);
    }

    SSLEngine engine = context.createSSLEngine();

    // Choose the sensible default list of protocols.
    final String[] supportedProtocols = engine.getSupportedProtocols();
    Set<String> supportedProtocolsSet = new HashSet<String>(supportedProtocols.length);
    for (i = 0; i < supportedProtocols.length; ++i) {
      supportedProtocolsSet.add(supportedProtocols[i]);
    }
    List<String> protocols = new ArrayList<String>();
    addIfSupported(
        supportedProtocolsSet, protocols,
        "TLSv1.2", "TLSv1.1", "TLSv1");

    if (!protocols.isEmpty()) {
      PROTOCOLS = protocols.toArray(new String[protocols.size()]);
    } else {
      PROTOCOLS = engine.getEnabledProtocols();
    }

    // Choose the sensible default list of cipher suites.
    final String[] supportedCiphers = engine.getSupportedCipherSuites();
    SUPPORTED_CIPHERS = new HashSet<String>(supportedCiphers.length);
    for (i = 0; i < supportedCiphers.length; ++i) {
      SUPPORTED_CIPHERS.add(supportedCiphers[i]);
    }
    List<String> ciphers = new ArrayList<String>();
    addIfSupported(
        SUPPORTED_CIPHERS, ciphers,
        // XXX: Make sure to sync this list with OpenSslEngineFactory.
        // GCM (Galois/Counter Mode) requires JDK 8.
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        // AES256 requires JCE unlimited strength jurisdiction policy files.
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        // GCM (Galois/Counter Mode) requires JDK 8.
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        // AES256 requires JCE unlimited strength jurisdiction policy files.
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA");

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

  private static void addIfSupported(final Set<String> supported, final List<String> enabled,
      final String... names) {
    for (String n : names) {
      if (supported.contains(n)) {
        enabled.add(n);
      }
    }
  }

  /**
   * Returns the JDK {@link SSLSessionContext} object held by this context.
   */
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

  /**
   * Build a {@link KeyManagerFactory} based upon a key file, key file password, and a certificate
   * chain.
   *
   * @param certChainFile a X.509 certificate chain file in PEM format
   * @param keyFile a PKCS#8 private key file in PEM format
   * @param keyPassword the password of the {@code keyFile}.
   *        {@code null} if it's not password-protected.
   * @param kmf The existing {@link KeyManagerFactory} that will be used if not {@code null}
   * @return A {@link KeyManagerFactory} based upon a key file, key file password, and a certificate
   *         chain.
   */
  protected static KeyManagerFactory buildKeyManagerFactory(final File certChainFile,
      final File keyFile, final String keyPassword)
          throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
          NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException,
          CertificateException, KeyException, IOException {
    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }
    return buildKeyManagerFactory(certChainFile, algorithm, keyFile, keyPassword);
  }

  /**
   * Build a {@link KeyManagerFactory} based upon a key algorithm, key file, key file password,
   * and a certificate chain.
   *
   * @param certChainFile a X.509 certificate chain file in PEM format
   * @param keyAlgorithm the standard name of the requested algorithm. See the Java Secure Socket
   *        Extension
   *        Reference Guide for information about standard algorithm names.
   * @param keyFile a PKCS#8 private key file in PEM format
   * @param keyPassword the password of the {@code keyFile}.
   *        {@code null} if it's not password-protected.
   * @return A {@link KeyManagerFactory} based upon a key algorithm, key file, key file password,
   *         and a certificate chain.
   */
  protected static KeyManagerFactory buildKeyManagerFactory(final File certChainFile,
      final String keyAlgorithm, final File keyFile, final String keyPassword)
          throws KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException,
          InvalidKeySpecException, InvalidAlgorithmParameterException, IOException,
          CertificateException, KeyException, UnrecoverableKeyException {
    char[] keyPasswordChars = keyPassword == null ? EMPTY_CHARS : keyPassword.toCharArray();
    KeyStore ks = buildKeyStore(certChainFile, keyFile, keyPasswordChars);
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyAlgorithm);
    kmf.init(ks, keyPasswordChars);

    return kmf;
  }
}
