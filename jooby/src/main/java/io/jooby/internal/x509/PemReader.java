/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.x509;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a PEM file and converts it into a list of DERs so that they are imported into a
 * {@link KeyStore} easily.
 *
 * Borrowed from <a href="http://netty.io">Netty</a>
 */
final class PemReader {

  private static final Pattern CERT_PATTERN = Pattern.compile(
      "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
          "([a-z0-9+/=\\r\\n]+)" + // Base64 text
          "-+END\\s+.*CERTIFICATE[^-]*-+", // Footer
      Pattern.CASE_INSENSITIVE);
  private static final Pattern KEY_PATTERN = Pattern.compile(
      "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
          "([a-z0-9+/=\\r\\n]+)" + // Base64 text
          "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", // Footer
      Pattern.CASE_INSENSITIVE);

  static List<ByteBuffer> readCertificates(final InputStream file)
      throws CertificateException, IOException {
    String content = IOUtils.toString(file, StandardCharsets.US_ASCII);

    List<ByteBuffer> certs = new ArrayList<ByteBuffer>();
    Matcher m = CERT_PATTERN.matcher(content);
    int start = 0;
    while (m.find(start)) {
      ByteBuffer buffer = ByteBuffer.wrap(decode(m.group(1)));
      certs.add(buffer);

      start = m.end();
    }

    if (certs.isEmpty()) {
      throw new CertificateException("found no certificates: " + file);
    }

    return certs;
  }

  private static byte[] decode(String value) {
    return Base64.getDecoder().decode(value.replaceAll("(?:\\r\\n|\\n\\r|\\n|\\r)", ""));
  }

  static ByteBuffer readPrivateKey(final InputStream file) throws KeyException, IOException {
    String content = IOUtils.toString(file, StandardCharsets.US_ASCII);

    Matcher m = KEY_PATTERN.matcher(content);
    if (!m.find()) {
      throw new KeyException("found no private key: " + file);
    }

    String value = m.group(1);
    return ByteBuffer.wrap(decode(value));
  }

  private PemReader() {
  }
}
