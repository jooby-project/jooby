/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.Test;

public class PemReaderTest {
  @Test
  public void testReadCertificates() throws Exception {
    String pem = "-----BEGIN CERTIFICATE-----\nSGVsbG8=\n-----END CERTIFICATE-----";
    InputStream stream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    assertFalse(PemReader.readCertificates(stream).isEmpty());

    // Error case
    InputStream emptyStream = new ByteArrayInputStream("".getBytes());
    assertThrows(CertificateException.class, () -> PemReader.readCertificates(emptyStream));
  }

  @Test
  public void testReadPrivateKey() throws Exception {
    String pem = "-----BEGIN PRIVATE KEY-----\nSGVsbG8=\n-----END PRIVATE KEY-----";
    InputStream stream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII));
    assertNotNull(PemReader.readPrivateKey(stream));

    // Error case
    InputStream badStream = new ByteArrayInputStream("no key here".getBytes());
    assertThrows(KeyException.class, () -> PemReader.readPrivateKey(badStream));
  }
}
