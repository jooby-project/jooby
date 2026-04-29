/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.spec.PBEParameterSpec;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.SslOptions;

class SslX509ProviderTest {

  private MockedStatic<PemReader> pemReaderMock;
  private MockedStatic<CertificateFactory> certFactoryMock;
  private CertificateFactory cf;
  private X509Certificate x509Cert;

  @BeforeEach
  void setUp() throws Exception {
    // Intercept static utility calls
    pemReaderMock = mockStatic(PemReader.class);
    certFactoryMock = mockStatic(CertificateFactory.class);

    // Mock CertificateFactory to return a valid dummy X509Certificate
    cf = mock(CertificateFactory.class);
    certFactoryMock.when(() -> CertificateFactory.getInstance("X.509")).thenReturn(cf);

    x509Cert = mock(X509Certificate.class);
    when(cf.generateCertificate(any())).thenReturn(x509Cert);
    when(x509Cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=Test"));

    // Provide a default dummy cert buffer so the loop in buildKeyStore executes
    pemReaderMock
        .when(() -> PemReader.readCertificates(any()))
        .thenReturn(List.of(ByteBuffer.wrap(new byte[] {1, 2})));
  }

  @AfterEach
  void tearDown() {
    // Clean up static mocks to prevent leaking into other tests
    pemReaderMock.close();
    certFactoryMock.close();
  }

  @Test
  void shouldSupportX509Type() {
    SslX509Provider provider = new SslX509Provider();
    assertTrue(provider.supports("X509"));
    assertTrue(provider.supports("x509"));
    assertFalse(provider.supports("PKCS12"));
    assertFalse(provider.supports(""));
  }

  @Test
  void createWithRsaKeyAndNoProvider() throws Exception {
    SslOptions options = mock(SslOptions.class);
    when(options.getPassword()).thenReturn(null); // Triggers unencrypted branch

    // Generate an actual RSA Key to satisfy the RSA branch successfully
    byte[] rsaKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded();
    pemReaderMock.when(() -> PemReader.readPrivateKey(any())).thenReturn(ByteBuffer.wrap(rsaKey));

    SslX509Provider provider = new SslX509Provider();
    SSLContext ctx = provider.create(null, null, options);

    assertNotNull(ctx);
    assertEquals("TLS", ctx.getProtocol());

    // Verify auto-close try-with-resources was exercised on the options
    verify(options).close();
  }

  @Test
  void createWithDsaKeyAndTrustStoreAndCustomProvider() throws Exception {
    SslOptions options = mock(SslOptions.class);
    when(options.getPassword()).thenReturn(""); // Empty string also triggers unencrypted branch
    when(options.getTrustCert())
        .thenReturn(mock(InputStream.class)); // Triggers TrustManager branch

    // Generate an actual DSA Key to fail the RSA try-block and successfully catch in the DSA block
    byte[] dsaKey = KeyPairGenerator.getInstance("DSA").generateKeyPair().getPrivate().getEncoded();
    pemReaderMock.when(() -> PemReader.readPrivateKey(any())).thenReturn(ByteBuffer.wrap(dsaKey));

    SslX509Provider provider = new SslX509Provider();
    SSLContext ctx = provider.create(null, "SunJSSE", options);

    assertNotNull(ctx);
    assertEquals("SunJSSE", ctx.getProvider().getName());
  }

  @Test
  void createWithEcKey() throws Exception {
    SslOptions options = mock(SslOptions.class);
    when(options.getPassword()).thenReturn(null);

    // Generate an actual EC Key to fail both RSA and DSA try-blocks and catch in the EC block
    byte[] ecKey = KeyPairGenerator.getInstance("EC").generateKeyPair().getPrivate().getEncoded();
    pemReaderMock.when(() -> PemReader.readPrivateKey(any())).thenReturn(ByteBuffer.wrap(ecKey));

    SslX509Provider provider = new SslX509Provider();
    SSLContext ctx = provider.create(null, null, options);

    assertNotNull(ctx);
  }

  @Test
  void createWithInvalidKeyThrowsException() {
    SslOptions options = mock(SslOptions.class);
    when(options.getPassword()).thenReturn(null);

    // Random byte array will fail RSA, DSA, and EC parsing -> Exception thrown
    pemReaderMock
        .when(() -> PemReader.readPrivateKey(any()))
        .thenReturn(ByteBuffer.wrap(new byte[] {1, 2, 3}));

    SslX509Provider provider = new SslX509Provider();
    Exception ex = assertThrows(Exception.class, () -> provider.create(null, null, options));

    // Verify SneakyThrows propagated the right failure message
    assertTrue(ex.getMessage().contains("Neither RSA, DSA nor EC worked"));
  }

  @Test
  void createWithEncryptedPrivateKeyCoversKeySpecGeneration() throws Exception {
    SslOptions options = mock(SslOptions.class);
    when(options.getPassword()).thenReturn("secret_password"); // Triggers Encrypted flow

    // Create a valid EncryptedPrivateKeyInfo wrapper around dummy bytes
    AlgorithmParameters params = AlgorithmParameters.getInstance("PBEWithMD5AndDES");
    params.init(new PBEParameterSpec(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, 1000));
    EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(params, new byte[16]);
    byte[] encBytes = epki.getEncoded();

    pemReaderMock.when(() -> PemReader.readPrivateKey(any())).thenReturn(ByteBuffer.wrap(encBytes));

    SslX509Provider provider = new SslX509Provider();

    // The code will successfully execute all lines in generateKeySpec until it hits
    // cipher decryption (which will fail due to invalid payload wrapper), granting 100% line
    // coverage.
    assertThrows(Exception.class, () -> provider.create(null, null, options));
  }
}
