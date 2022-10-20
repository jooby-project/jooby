/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.x509;

import java.io.InputStream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * A server-side {@link SslContext} which uses JDK's SSL/TLS implementation.
 *
 * <p>Borrowed from <a href="http://netty.io">Netty</a>
 */
public final class JdkSslServerContext extends JdkSslContext {

  private final SSLContext ctx;

  /**
   * Creates a new instance.
   *
   * @param trustCertChainFile an X.509 certificate chain file in PEM format. This provides the
   *     certificate chains used for mutual authentication. {@code null} to use the system default
   * @param trustManagerFactory the {@link TrustManagerFactory} that provides the {@link
   *     TrustManager}s that verifies the certificates sent from clients. {@code null} to use the
   *     default or the results of parsing {@code trustCertChainFile}
   * @param keyCertChainFile an X.509 certificate chain file in PEM format
   * @param keyFile a PKCS#8 private key file in PEM format
   * @param keyPassword the password of the {@code keyFile}. {@code null} if it's not
   *     password-protected.
   * @param keyManagerFactory the {@link KeyManagerFactory} that provides the {@link KeyManager}s
   *     that is used to encrypt data being sent to clients. {@code null} to use the default or the
   *     results of parsing {@code keyCertChainFile} and {@code keyFile}.
   * @param ciphers the cipher suites to enable, in the order of preference. {@code null} to use the
   *     default cipher suites.
   * @param cipherFilter a filter to apply over the supplied list of ciphers Only required if {@code
   *     provider} is {@link SslProvider#JDK}
   * @param apn Application Protocol Negotiator object.
   * @param sessionCacheSize the size of the cache used for storing SSL session objects. {@code 0}
   *     to use the default value.
   * @param sessionTimeout the timeout for the cached SSL session objects, in seconds. {@code 0} to
   *     use the default value.
   */
  public JdkSslServerContext(
      final String provider,
      final InputStream trustCertChainFile,
      final InputStream keyCertChainFile,
      final InputStream keyFile,
      final String keyPassword,
      final long sessionCacheSize,
      final long sessionTimeout)
      throws SSLException {

    try {
      TrustManagerFactory trustManagerFactory = null;
      if (trustCertChainFile != null) {
        trustManagerFactory = buildTrustManagerFactory(trustCertChainFile, trustManagerFactory);
      }
      KeyManagerFactory keyManagerFactory =
          buildKeyManagerFactory(keyCertChainFile, keyFile, keyPassword);

      // Initialize the SSLContext to work with our key managers.
      ctx =
          provider == null
              ? SSLContext.getInstance(PROTOCOL)
              : SSLContext.getInstance(PROTOCOL, provider);

      ctx.init(
          keyManagerFactory.getKeyManagers(),
          trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(),
          null);

      SSLSessionContext sessCtx = ctx.getServerSessionContext();
      if (sessionCacheSize > 0) {
        sessCtx.setSessionCacheSize((int) Math.min(sessionCacheSize, Integer.MAX_VALUE));
      }
      if (sessionTimeout > 0) {
        sessCtx.setSessionTimeout((int) Math.min(sessionTimeout, Integer.MAX_VALUE));
      }
    } catch (Exception e) {
      throw new SSLException("failed to initialize the server-side SSL context", e);
    }
  }

  @Override
  public SSLContext context() {
    return ctx;
  }
}
