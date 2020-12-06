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
package io.jooby.internal.x509;

import java.io.ByteArrayInputStream;
import java.io.File;
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
 * A secure socket protocol implementation which acts as a factory for {@link SSLEngine} and
 * {@link SslHandler}.
 * Internally, it is implemented via JDK's {@link SSLContext} or OpenSSL's {@code SSL_CTX}.
 *
 * <h3>Making your server support SSL/TLS</h3>
 * <pre>
 * // In your {@link ChannelInitializer}:
 * {@link ChannelPipeline} p = channel.pipeline();
 * {@link SslContext} sslCtx = {@link SslContextBuilder#forServer(File, File) SslContextBuilder.forServer(...)}.build();
 * p.addLast("ssl", {@link #newEngine(ByteBufAllocator) sslCtx.newEngine(channel.alloc())});
 * ...
 * </pre>
 *
 * <h3>Making your client support SSL/TLS</h3>
 * <pre>
 * // In your {@link ChannelInitializer}:
 * {@link ChannelPipeline} p = channel.pipeline();
 * {@link SslContext} sslCtx = {@link SslContextBuilder#forClient() SslContextBuilder.forClient()}.build();
 * p.addLast("ssl", {@link #newEngine(ByteBufAllocator, String, int) sslCtx.newEngine(channel.alloc(), host, port)});
 * ...
 * </pre>
 *
 * Borrowed from <a href="http://netty.io">Netty</a>
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

  public static SslContext newServerContextInternal(final String provider,
      final InputStream trustCertChainFile,
      final InputStream keyCertChainFile, final InputStream keyFile, final String keyPassword,
      final long sessionCacheSize, final long sessionTimeout) throws SSLException {
    return new JdkSslServerContext(provider, trustCertChainFile, keyCertChainFile,
        keyFile, keyPassword, sessionCacheSize, sessionTimeout);
  }

  /**
   * Returns the size of the cache used for storing SSL session objects.
   */
  public abstract long sessionCacheSize();

  public abstract long sessionTimeout();

  public abstract SSLContext context();

  /**
   * Returns the {@link SSLSessionContext} object held by this context.
   */
  public abstract SSLSessionContext sessionContext();

  /**
   * Generates a key specification for an (encrypted) private key.
   *
   * @param password characters, if {@code null} or empty an unencrypted key is assumed
   * @param key bytes of the DER encoded private key
   *
   * @return a key specification
   *
   * @throws IOException if parsing {@code key} fails
   * @throws NoSuchAlgorithmException if the algorithm used to encrypt {@code key} is unkown
   * @throws NoSuchPaddingException if the padding scheme specified in the decryption algorithm is
   *         unkown
   * @throws InvalidKeySpecException if the decryption key based on {@code password} cannot be
   *         generated
   * @throws InvalidKeyException if the decryption key based on {@code password} cannot be used to
   *         decrypt
   *         {@code key}
   * @throws InvalidAlgorithmParameterException if decryption algorithm parameters are somehow
   *         faulty
   */
  protected static PKCS8EncodedKeySpec generateKeySpec(final char[] password, final byte[] key)
      throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
      InvalidKeyException, InvalidAlgorithmParameterException {

    if (password == null || password.length == 0) {
      return new PKCS8EncodedKeySpec(key);
    }

    EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
    SecretKeyFactory keyFactory = SecretKeyFactory
        .getInstance(encryptedPrivateKeyInfo.getAlgName());
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
    SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);

    Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
    cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());

    return encryptedPrivateKeyInfo.getKeySpec(cipher);
  }

  /**
   * Generates a new {@link KeyStore}.
   *
   * @param certChainFile a X.509 certificate chain file in PEM format,
   * @param keyFile a PKCS#8 private key file in PEM format,
   * @param keyPasswordChars the password of the {@code keyFile}.
   *        {@code null} if it's not password-protected.
   * @return generated {@link KeyStore}.
   */
  static KeyStore buildKeyStore(final InputStream certChainFile, final InputStream keyFile,
      final char[] keyPasswordChars)
      throws KeyStoreException, NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException,
      CertificateException, KeyException, IOException {
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
    List<Certificate> certChain = new ArrayList<Certificate>(certs.size());

    for (ByteBuffer buf : certs) {
      certChain.add(cf.generateCertificate(new ByteArrayInputStream(buf.array())));
    }

    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);
    ks.setKeyEntry("key", key, keyPasswordChars,
        certChain.toArray(new Certificate[certChain.size()]));
    return ks;
  }

  /**
   * Build a {@link TrustManagerFactory} from a certificate chain file.
   *
   * @param certChainFile The certificate file to build from.
   * @param trustManagerFactory The existing {@link TrustManagerFactory} that will be used if not
   *        {@code null}.
   * @return A {@link TrustManagerFactory} which contains the certificates in {@code certChainFile}
   */
  protected static TrustManagerFactory buildTrustManagerFactory(final InputStream certChainFile,
      TrustManagerFactory trustManagerFactory)
      throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");

    List<ByteBuffer> certs = PemReader.readCertificates(certChainFile);

    for (ByteBuffer buf : certs) {
      X509Certificate cert = (X509Certificate) cf
          .generateCertificate(new ByteArrayInputStream(buf.array()));
      X500Principal principal = cert.getSubjectX500Principal();
      ks.setCertificateEntry(principal.getName("RFC2253"), cert);
    }

    // Set up trust manager factory to use our key store.
    if (trustManagerFactory == null) {
      trustManagerFactory = TrustManagerFactory
          .getInstance(TrustManagerFactory.getDefaultAlgorithm());
    }
    trustManagerFactory.init(ks);

    return trustManagerFactory;
  }
}
