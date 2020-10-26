/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static io.jooby.SneakyThrows.throwingFunction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.typesafe.config.Config;

/**
 * SSL options for enabling HTTPs in Jooby. Jooby supports two certificate formats:
 *
 * - PKCS12
 * - X.509
 *
 * Jooby doesn't support JKS format due it is a proprietary format, it favors the use of PKCS12
 * format.
 *
 * @author edgar
 * @since 2.3.0
 */
public final class SslOptions {
  /**
   * The desired SSL client authentication mode for SSL channels in server mode.
   */
  public enum ClientAuth {
    /**
     * SSL client authentication is NOT requested.
     */
    NONE,

    /**
     * SSL client authentication is requested but not required.
     */
    REQUESTED,

    /**
     * SSL client authentication is required.
     */
    REQUIRED
  }

  /** TLSv1.2. Default TLS protocol. */
  public static final String TLS_V1_2 = "TLSv1.2";

  /**
   * TLSv1.3 protocol. Available in:
   * - 8u261-b12 from Oracle JDK
   * - TLS 1.3 support in OpenJDK is (beside Azul's OpenJSSE) expected to come into 8u272.
   * - Java 11.0.3 or higher.
   */
  public static final String TLS_V1_3 = "TLSv1.3";

  /** X509 constant. */
  public static final String X509 = "X509";

  /** PKCS12 constant. */
  public static final String PKCS12 = "PKCS12";

  private String password;

  private String type = PKCS12;

  private String cert;

  private String trustCert;

  private String trustPassword;

  private String privateKey;

  private ClientAuth clientAuth = ClientAuth.NONE;

  private List<String> protocol = Arrays.asList(TLS_V1_3, TLS_V1_2);

  /**
   * Certificate type. Default is {@link #PKCS12}.
   *
   * @return Certificate type. Default is {@link #PKCS12}.
   */
  public String getType() {
    return type;
  }

  /**
   * Set certificate type.
   *
   * @param type Certificate type.
   * @return Ssl options.
   */
  public @Nonnull SslOptions setType(@Nonnull String type) {
    this.type = type;
    return this;
  }

  /**
   * A PKCS12 or X.509 certificate chain file in PEM format. It can be an absolute path or a
   * classpath resource. Required.
   *
   * @return A PKCS12 or X.509 certificate chain file in PEM format. It can be an absolute path or
   *     a classpath resource. Required.
   */
  public @Nonnull String getCert() {
    return cert;
  }

  /**
   * Set certificate path. A PKCS12 or X.509 certificate chain file in PEM format.
   * It can be an absolute path or a classpath resource. Required.
   *
   * @param cert Certificate path or location.
   * @return Ssl options.
   */
  public @Nonnull SslOptions setCert(@Nonnull String cert) {
    this.cert = cert;
    return this;
  }

  /**
   * A PKCS12 or X.509 certificate chain file in PEM format. It can be an absolute path or a
   * classpath resource. Required for {@link ClientAuth#REQUIRED} or {@link ClientAuth#REQUESTED}.
   *
   * @return A PKCS12 or X.509 certificate chain file in PEM format. It can be an absolute path or
   *     a classpath resource. Required for {@link ClientAuth#REQUIRED} or
   *     {@link ClientAuth#REQUESTED}.
   */
  public @Nullable String getTrustCert() {
    return trustCert;
  }

  /**
   * Set certificate path. A PKCS12 or X.509 certificate chain file in PEM format.
   * It can be an absolute path or a classpath resource. Required.
   *
   * @param trustCert Certificate path or location.
   * @return Ssl options.
   */
  public @Nonnull SslOptions setTrustCert(@Nullable String trustCert) {
    this.trustCert = trustCert;
    return this;
  }

  /**
   * Trust certificate password. Optional.
   *
   * @return Trust certificate password. Optional.
   */
  public @Nullable String getTrustPassword() {
    return trustPassword;
  }

  /**
   * Set trust certificate password.
   *
   * @param password Certificate password.
   * @return SSL options.
   */
  public @Nonnull SslOptions setTrustPassword(@Nullable String password) {
    this.trustPassword = password;
    return this;
  }

  /**
   * Private key file location. A PKCS#8 private key file in PEM format. It can be an absolute path
   * or a classpath resource. Required when using X.509 certificates.
   *
   * @return A PKCS#8 private key file in PEM format. It can be an absolute path or a classpath
   *     resource. Required when using X.509 certificates.
   */
  public @Nullable String getPrivateKey() {
    return privateKey;
  }

  /**
   * Set private key file location. A PKCS#8 private key file in PEM format. It can be an absolute
   * path or a classpath resource. Required when using X.509 certificates.
   *
   * @param privateKey Private key file location. A PKCS#8 private key file in PEM format. It can
   *     be an absolute path or a classpath resource. Required when using X.509 certificates.
   * @return Ssl options.
   */
  public @Nonnull SslOptions setPrivateKey(@Nullable String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  /**
   * Certificate password.
   *
   * @param password Certificate password.
   * @return SSL options.
   */
  public @Nonnull SslOptions setPassword(@Nullable String password) {
    this.password = password;
    return this;
  }

  /**
   * Certificate password.
   *
   * @return Certificate password.
   */
  public @Nullable String getPassword() {
    return password;
  }

  /**
   * Search for a resource at the given path. This method uses the following order:
   *
   * - Look at file system for path as it is (absolute path)
   * - Look at file system for path relative to current process dir
   * - Look at class path for path
   *
   * @param loader Class loader.
   * @param path Path (file system path or classpath).
   * @return Resource.
   * @throws IOException If file not found or can't be read it.
   */
  public @Nonnull InputStream getResource(@Nonnull ClassLoader loader, @Nonnull String path)
      throws IOException {
    InputStream resource = Stream
        .of(Paths.get(path), Paths.get(System.getProperty("user.dir"), path))
        .map(it -> it.normalize().toAbsolutePath())
        .filter(Files::exists)
        .findFirst()
        .map(throwingFunction(file -> Files.newInputStream(file)))
        .orElseGet(
            () -> loader.getResourceAsStream(path.startsWith("/") ? path.substring(1) : path));
    if (resource == null) {
      throw new FileNotFoundException(path);
    }
    return resource;
  }

  /**
   * The desired SSL client authentication mode for SSL channels in server mode.
   *
   * Default is: {@link ClientAuth#REQUESTED}.
   *
   * @return desired SSL client authentication mode for SSL channels in server mode.
   */
  public @Nonnull ClientAuth getClientAuth() {
    return clientAuth;
  }

  /**
   * Set desired SSL client authentication mode for SSL channels in server mode.
   *
   * @param clientAuth The desired SSL client authentication mode for SSL channels in server mode.
   * @return This options.
   */
  public @Nonnull SslOptions setClientAuth(@Nonnull ClientAuth clientAuth) {
    this.clientAuth = clientAuth;
    return this;
  }

  /**
   * Specify the enabled protocols for an SSL/TLS session. Default is: <code>TLSv1.2</code> and
   * <code>TLSv1.3</code>.
   *
   * If a listed protocol is not supported, it is ignored; however, if you specify a list of
   * protocols, none of which are supported, an exception will be thrown.
   *
   * Please note TLSv1.3 protocol is available in:
   *  - 8u261-b12 from Oracle JDK
   *  - TLS 1.3 support in OpenJDK is (beside Azul's OpenJSSE) expected to come into 8u272.
   *  - Java 11.0.3 or higher.
   *
   * @return TLS protocols. Default is: <code>TLSv1.2</code> and <code>TLSv1.3</code>.
   */
  public @Nonnull List<String> getProtocol() {
    return protocol;
  }

  /**
   * Specify the enabled protocols for an SSL/TLS session. If a listed protocol is not supported,
   * it is ignored; however, if you specify a list of protocols, none of which are supported,
   * an exception will be thrown.
   *
   * @param protocol TLS protocols.
   * @return This options.
   */
  public SslOptions setProtocol(@Nonnull String... protocol) {
    return setProtocol(Arrays.asList(protocol));
  }

  /**
   * Specify the enabled protocols for an SSL/TLS session. If a listed protocol is not supported,
   * it is ignored; however, if you specify a list of protocols, none of which are supported,
   * an exception will be thrown.
   *
   * @param protocol TLS protocols.
   * @return This options.
   */
  public SslOptions setProtocol(@Nonnull List<String> protocol) {
    this.protocol = protocol;
    return this;
  }

  @Override public String toString() {
    return type;
  }

  /**
   * Creates SSL options for X.509 certificate type.
   *
   * @param crt Certificate path or location.
   * @param key Private key path or location.
   * @return New SSL options.
   */
  public static @Nonnull SslOptions x509(@Nonnull String crt, @Nonnull String key) {
    return x509(crt, key, null);
  }

  /**
   * Creates SSL options for X.509 certificate type.
   *
   * @param crt Certificate path or location.
   * @param key Private key path or location.
   * @param password Password.
   * @return New SSL options.
   */
  public static @Nonnull SslOptions x509(@Nonnull String crt, @Nonnull String key,
      @Nullable String password) {
    SslOptions options = new SslOptions();
    options.setType(X509);
    options.setPrivateKey(key);
    options.setCert(crt);
    options.setPassword(password);
    return options;
  }

  /**
   * Creates SSL options for PKCS12 certificate type.
   *
   * @param crt Certificate path or location.
   * @param password Password.
   * @return New SSL options.
   */
  public static SslOptions pkcs12(@Nonnull String crt, @Nonnull String password) {
    SslOptions options = new SslOptions();
    options.setType(PKCS12);
    options.setCert(crt);
    options.setPassword(password);
    return options;
  }

  /**
   * Creates SSL options using a self-signed certificate using PKCS12. Useful for development.
   * Certificate works for <code>localhost</code>.
   *
   * @return New SSL options.
   */
  public static SslOptions selfSigned() {
    return selfSigned(PKCS12);
  }

  /**
   * Creates SSL options using a self-signed certificate. Useful for development. Certificate works
   * for <code>localhost</code>.
   *
   * @param type Certificate type: <code>PKCS12</code> or <code>X509</code>.
   * @return New SSL options.
   */
  public static SslOptions selfSigned(final String type) {
    switch (type.toUpperCase()) {
      case PKCS12:
        return pkcs12("io/jooby/ssl/localhost.p12", "changeit");
      case X509:
        return x509("io/jooby/ssl/localhost.crt", "io/jooby/ssl/localhost.key");
      default:
        throw new UnsupportedOperationException("SSL type: " + type);
    }
  }

  /**
   * Get SSL options from application configuration. Configuration must be at
   * <code>server.ssl</code> or <code>ssl</code>.
   *
   * PKCS12 example:
   * <pre>
   *   server {
   *     ssl {
   *       type: PKCS12
   *       cert: mycertificate.crt
   *       password: mypassword
   *     }
   *   }
   * </pre>
   *
   * X509 example:
   * <pre>
   *   server {
   *     ssl {
   *       type: X509
   *       cert: mycertificate.crt
   *       key: mykey.key
   *     }
   *   }
   * </pre>
   *
   * @param conf Application configuration.
   * @return SSl options or empty.
   */
  public static @Nonnull Optional<SslOptions> from(@Nonnull Config conf) {
    return from(conf, "server.ssl", "ssl");
  }

  /**
   * Get SSL options from application configuration. It looks for ssl options at the given path(s).
   *
   * PKCS12 example:
   * <pre>
   *   server {
   *     ssl {
   *       type: PKCS12
   *       cert: mycertificate.crt
   *       password: mypassword
   *     }
   *   }
   * </pre>
   *
   * X509 example:
   * <pre>
   *   server {
   *     ssl {
   *       type: X509
   *       cert: mycertificate.crt
   *       key: mykey.key
   *     }
   *   }
   * </pre>
   *
   * @param conf Application configuration.
   * @param key Path to use for loading SSL options. Required.
   * @return SSl options or empty.
   */
  public static @Nonnull Optional<SslOptions> from(@Nonnull Config conf, String... key) {
    return Stream.of(key)
        .filter(conf::hasPath)
        .findFirst()
        .map(path -> {
          String type = conf.hasPath(path + ".type")
              ? conf.getString(path + ".type").toUpperCase()
              : PKCS12;
          SslOptions options;
          if (type.equalsIgnoreCase("self-signed")) {
            options = SslOptions.selfSigned();
          } else {
            options = new SslOptions();
            options.setType(type);
            if (X509.equalsIgnoreCase(type)) {
              options.setCert(conf.getString(path + ".cert"));
              options.setPrivateKey(conf.getString(path + ".key"));
              if (conf.hasPath(path + ".password")) {
                options.setPassword(conf.getString(path + ".password"));
              }
            } else if (type.equalsIgnoreCase(PKCS12)) {
              options.setCert(conf.getString(path + ".cert"));
              options.setPassword(conf.getString(path + ".password"));
            } else {
              throw new UnsupportedOperationException("SSL type: " + type);
            }
          }
          if (conf.hasPath(path + ".clientAuth")) {
            options.setClientAuth(ClientAuth.valueOf(conf.getString(path + ".clientAuth")
                .toUpperCase()));
          }
          if (conf.hasPath(path + ".trust.cert")) {
            options.setTrustCert(conf.getString(path + ".trust.cert"));
          }
          if (conf.hasPath(path + ".trust.password")) {
            options.setTrustPassword(conf.getString(path + ".trust.password"));
          }
          if (conf.hasPath(path + ".protocol")) {
            Object value = conf.getAnyRef(path + ".protocol");
            if (value instanceof List) {
              options.setProtocol((List) value);
            } else {
              options.setProtocol(value.toString());
            }
          }
          return options;
        });
  }
}
